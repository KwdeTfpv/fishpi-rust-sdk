package dev.fishpi.mobile.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class V3SessionManager private constructor(
    context: Context,
    private val api: FishPiApiClient = FishPiApiClient.shared,
) {
    companion object {
        private const val PREFS = "fishpi_v3_session"
        private const val KEY_PREFIX = "session."
        private const val EARLY_REFRESH_MS = 60_000L

        @Volatile
        private var instance: V3SessionManager? = null

        fun get(context: Context): V3SessionManager =
            instance ?: synchronized(this) {
                instance ?: V3SessionManager(context.applicationContext).also { instance = it }
            }
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val refreshMutex = Mutex()
    private val writeMutex = Mutex()

    @Volatile
    private var active: V3SessionSnapshot? = null

    fun current(): V3SessionSnapshot? = active

    fun isBoundTo(userId: String): Boolean = active?.ownerUserId == userId

    fun restoreFor(userId: String): V3SessionSnapshot? {
        if (userId.isBlank()) return null
        val stored = readStored(userId) ?: return null
        active = stored
        return stored
    }

    suspend fun authorize(
        nameOrEmail: String,
        password: String,
        mfaCode: String?,
        legacyUserId: String,
    ): V3SessionSnapshot {
        if (legacyUserId.isBlank()) {
            throw V3RemoteException(
                V3FailureKind.Protocol,
                null,
                null,
                null,
                "当前账号缺少用户 ID，无法绑定外观授权",
            )
        }
        val credentials = withContext(Dispatchers.IO) {
            api.v3Login(nameOrEmail, password, mfaCode)
        }
        val identity = withContext(Dispatchers.IO) { api.v3Me(credentials.accessToken) }
        if (identity.userId.isBlank() || identity.userId != legacyUserId) {
            runCatching {
                withContext(Dispatchers.IO) { api.v3Logout(credentials.refreshCookiesJson) }
            }
            throw V3RemoteException(
                V3FailureKind.Protocol,
                null,
                null,
                null,
                "外观授权账号与当前登录账号不一致",
            )
        }
        val next = V3SessionSnapshot(
            ownerUserId = legacyUserId,
            accessToken = credentials.accessToken,
            expiresAt = System.currentTimeMillis() + credentials.expiresIn * 1000L,
            refreshExpiresIn = credentials.refreshExpiresIn,
            scope = credentials.scope,
            refreshCookiesJson = credentials.refreshCookiesJson,
            generation = System.currentTimeMillis(),
        )
        store(next)
        return next
    }

    suspend fun <T> withToken(legacyUserId: String, block: suspend (String) -> T): T {
        val token = ensureFresh(legacyUserId)
        return try {
            block(token.accessToken)
        } catch (error: V3RemoteException) {
            if (!error.requiresReauth) throw error
            val refreshed = refreshLocked(legacyUserId)
            block(refreshed.accessToken)
        }
    }

    private suspend fun ensureFresh(legacyUserId: String): V3SessionSnapshot {
        val bound = active ?: throw V3RemoteException(
            V3FailureKind.Unauthorized,
            null,
            null,
            null,
            "尚未授权外观功能",
        )
        if (bound.ownerUserId != legacyUserId) {
            throw V3RemoteException(
                V3FailureKind.Unauthorized,
                null,
                null,
                null,
                "外观授权账号与当前登录账号不一致，请重新授权",
            )
        }
        if (bound.accessToken.isNotBlank() &&
            bound.expiresAt - System.currentTimeMillis() > EARLY_REFRESH_MS
        ) {
            return bound
        }
        return refreshLocked(legacyUserId)
    }

    private suspend fun refreshLocked(legacyUserId: String): V3SessionSnapshot {
        return refreshMutex.withLock {
            val currentBound = active ?: throw V3RemoteException(
                V3FailureKind.Unauthorized,
                null,
                null,
                null,
                "尚未授权外观功能",
            )
            if (currentBound.ownerUserId != legacyUserId) {
                throw V3RemoteException(
                    V3FailureKind.Unauthorized,
                    null,
                    null,
                    null,
                    "外观授权账号与当前登录账号不一致，请重新授权",
                )
            }
            if (currentBound.accessToken.isNotBlank() &&
                currentBound.expiresAt - System.currentTimeMillis() > EARLY_REFRESH_MS
            ) {
                return@withLock currentBound
            }
            if (currentBound.refreshCookiesJson.isBlank()) {
                throw V3RemoteException(
                    V3FailureKind.Unauthorized,
                    null,
                    null,
                    null,
                    "外观授权已失效，请重新授权",
                )
            }
            val credentials = withContext(Dispatchers.IO) {
                api.v3Refresh(currentBound.refreshCookiesJson)
            }
            val next = currentBound.copy(
                accessToken = credentials.accessToken,
                expiresAt = System.currentTimeMillis() + credentials.expiresIn * 1000L,
                refreshExpiresIn = credentials.refreshExpiresIn,
                scope = credentials.scope,
                refreshCookiesJson = mergeRefreshCookies(
                    currentBound.refreshCookiesJson,
                    credentials.refreshCookiesJson,
                ),
                generation = System.currentTimeMillis(),
            )
            store(next)
            next
        }
    }

    suspend fun revoke(userId: String) {
        val bound = active?.takeIf { it.ownerUserId == userId } ?: readStored(userId)
        if (bound != null && bound.refreshCookiesJson.isNotBlank()) {
            runCatching {
                withContext(Dispatchers.IO) { api.v3Logout(bound.refreshCookiesJson) }
            }
        }
        clear(userId)
    }

    suspend fun revokeAll() {
        val bound = active
        if (bound != null && bound.refreshCookiesJson.isNotBlank()) {
            runCatching {
                withContext(Dispatchers.IO) { api.v3Logout(bound.refreshCookiesJson) }
            }
        }
        clearAll()
    }

    fun clear(userId: String) {
        writeMutex.tryLock()
        try {
            if (active?.ownerUserId == userId) active = null
            prefs.edit().remove(KEY_PREFIX + userId).apply()
        } finally {
            if (writeMutex.isLocked) writeMutex.unlock()
        }
    }

    fun clearAll() {
        active = null
        prefs.edit().clear().apply()
    }

    private suspend fun store(snapshot: V3SessionSnapshot) {
        writeMutex.withLock {
            active = snapshot
            val payload = JSONObject()
                .put("ownerUserId", snapshot.ownerUserId)
                .put("accessToken", snapshot.accessToken)
                .put("expiresAt", snapshot.expiresAt)
                .put("refreshExpiresIn", snapshot.refreshExpiresIn)
                .put("scope", snapshot.scope)
                .put("refreshCookies", snapshot.refreshCookiesJson)
                .put("generation", snapshot.generation)
                .toString()
            val sealed = runCatching { SessionVault.encrypt(payload) }.getOrNull() ?: return
            prefs.edit().putString(KEY_PREFIX + snapshot.ownerUserId, sealed).apply()
        }
    }

    private fun readStored(userId: String): V3SessionSnapshot? {
        val sealed = prefs.getString(KEY_PREFIX + userId, null) ?: return null
        val raw = runCatching { SessionVault.decrypt(sealed) }.getOrNull() ?: return null
        return runCatching {
            val json = JSONObject(raw)
            V3SessionSnapshot(
                ownerUserId = json.optString("ownerUserId").ifBlank { userId },
                accessToken = json.optString("accessToken"),
                expiresAt = json.optLong("expiresAt", 0L),
                refreshExpiresIn = json.optLong("refreshExpiresIn", 0L),
                scope = json.optString("scope"),
                refreshCookiesJson = json.optString("refreshCookies"),
                generation = json.optLong("generation", 0L),
            )
        }.getOrNull()
    }
}

private fun mergeRefreshCookies(existing: String, incoming: String): String {
    if (incoming.isBlank()) return existing
    val merged = LinkedHashMap<String, JSONObject>()
    for (source in listOf(existing, incoming)) {
        val array = runCatching { JSONArray(source) }.getOrNull() ?: continue
        for (index in 0 until array.length()) {
            val cookie = array.optJSONObject(index) ?: continue
            val name = cookie.optString("name")
            if (name.isBlank()) continue
            merged[name] = cookie
        }
    }
    if (merged.isEmpty()) return existing
    val array = JSONArray()
    merged.values.forEach { array.put(it) }
    return array.toString()
}

private object SessionVault {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore",
            )
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generator.generateKey()
        }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + cipherText, Base64.NO_WRAP)
    }

    fun decrypt(sealed: String): String {
        val bytes = Base64.decode(sealed, Base64.NO_WRAP)
        require(bytes.size > IV_BYTES) { "session payload truncated" }
        val iv = bytes.copyOfRange(0, IV_BYTES)
        val cipherText = bytes.copyOfRange(IV_BYTES, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }
}

private const val KEY_ALIAS = "fishpi.v3.session"
