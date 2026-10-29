package dev.fishpi.mobile

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.fishpi.mobile.data.FishPiApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun RegisterScreen(
    onClose: () -> Unit,
) {
    val api = remember { FishPiApiClient.shared }
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(1) }
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var captcha by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("0") }
    var captchaImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var userId by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    fun loadCaptcha() {
        scope.launch {
            loading = true
            error = null
            try {
                val encoded = withContext(Dispatchers.IO) { api.fetchRegisterCaptcha() }
                captchaImage = withContext(Dispatchers.IO) {
                    val bytes = Base64.decode(encoded, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            } catch (e: Exception) {
                error = e.message ?: "获取图形验证码失败"
                captchaImage = null
            } finally {
                loading = false
            }
        }
    }

    fun sendSms() {
        val name = username.trim()
        val phoneNumber = phone.trim()
        val code = captcha.trim()
        if (name.isBlank()) {
            error = "请输入用户名"
            return
        }
        if (!phoneNumber.matches(Regex("^1\\d{10}$"))) {
            error = "请输入正确的手机号"
            return
        }
        if (code.isBlank()) {
            error = "请输入图形验证码"
            return
        }
        scope.launch {
            loading = true
            error = null
            try {
                withContext(Dispatchers.IO) {
                    api.preRegister(name, phoneNumber, code)
                }
                step = 2
            } catch (e: Exception) {
                error = e.message ?: "发送短信验证码失败"
                loadCaptcha()
            } finally {
                loading = false
            }
        }
    }

    fun verifySms() {
        val code = smsCode.trim()
        if (code.isBlank()) {
            error = "请输入短信验证码"
            return
        }
        scope.launch {
            loading = true
            error = null
            try {
                userId = withContext(Dispatchers.IO) { api.verifyRegisterCode(code) }
                step = 3
            } catch (e: Exception) {
                error = e.message ?: "短信验证码校验失败"
            } finally {
                loading = false
            }
        }
    }

    fun finishRegister() {
        if (password.length < 6) {
            error = "密码至少需要 6 位"
            return
        }
        if (password != confirmPassword) {
            error = "两次输入的密码不一致"
            return
        }
        scope.launch {
            loading = true
            error = null
            try {
                withContext(Dispatchers.IO) {
                    api.registerAccount(userId, password, role)
                }
                onClose()
            } catch (e: Exception) {
                error = e.message ?: "注册失败"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadCaptcha()
    }

    DisposableEffect(Unit) {
        onDispose { api.resetRegisterSession() }
    }

    BackHandler {
        if (step > 1) step -= 1 else onClose()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {}
    ) {
        Surface(tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { if (step > 1) step -= 1 else onClose() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "注册账号",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onClose) {
                    Text("关闭")
                }
            }
        }

        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = when (step) {
                    1 -> "填写账号信息"
                    2 -> "验证手机号"
                    else -> "设置密码"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "第 $step 步 / 共 3 步",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            when (step) {
                1 -> {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("用户名") },
                        singleLine = true,
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("手机号") },
                        singleLine = true,
                        enabled = !loading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = captcha,
                            onValueChange = { captcha = it },
                            label = { Text("图形验证码") },
                            singleLine = true,
                            enabled = !loading,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 120.dp, height = 56.dp)
                                .clickable(enabled = !loading) { loadCaptcha() },
                            contentAlignment = Alignment.Center,
                        ) {
                            val image = captchaImage
                            if (image != null) {
                                Image(
                                    bitmap = image,
                                    contentDescription = "图形验证码，点击刷新",
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Text(
                                    text = "点击刷新",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    Button(
                        onClick = { sendSms() },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("获取短信验证码")
                    }
                }

                2 -> {
                    Text(
                        text = "验证码已发送至 $phone",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = smsCode,
                        onValueChange = { smsCode = it },
                        label = { Text("短信验证码") },
                        singleLine = true,
                        enabled = !loading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { verifySms() },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("验证")
                    }
                    TextButton(
                        onClick = { step = 1 },
                        enabled = !loading,
                    ) {
                        Text("返回修改手机号")
                    }
                }

                else -> {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码（至少 6 位）") },
                        singleLine = true,
                        enabled = !loading,
                        visualTransformation = if (showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                    contentDescription = if (showPassword) "隐藏密码" else "显示密码",
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("确认密码") },
                        singleLine = true,
                        enabled = !loading,
                        visualTransformation = if (showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "选择角色",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = role == "0",
                            onClick = { role = "0" },
                            label = { Text("黑客") },
                            enabled = !loading,
                        )
                        FilterChip(
                            selected = role == "1",
                            onClick = { role = "1" },
                            label = { Text("画家") },
                            enabled = !loading,
                        )
                    }
                    Button(
                        onClick = { finishRegister() },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("完成注册")
                    }
                    TextButton(
                        onClick = { step = 2 },
                        enabled = !loading,
                    ) {
                        Text("返回修改验证码")
                    }
                }
            }

            if (loading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
