package dev.fishpi.mobile.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import java.io.File

object UpdateInstaller {
    fun install(context: Context, apk: File): Boolean {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL,
        ).apply {
            setAppPackageName(context.packageName)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("base.apk", 0, apk.length()).use { output ->
                apk.inputStream().use { input -> input.copyTo(output) }
                session.fsync(output)
            }
            val resultIntent = Intent(context, UpdateInstallReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(pendingIntent.intentSender)
        }
        return true
    }

    fun statusMessage(status: Int): String = when (status) {
        PackageInstaller.STATUS_SUCCESS -> "安装成功"
        PackageInstaller.STATUS_FAILURE -> "安装失败"
        PackageInstaller.STATUS_FAILURE_ABORTED -> "安装已取消"
        PackageInstaller.STATUS_FAILURE_BLOCKED -> "安装被系统阻止"
        PackageInstaller.STATUS_FAILURE_CONFLICT -> "安装冲突，签名或版本不一致"
        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "安装包与当前系统不兼容"
        PackageInstaller.STATUS_FAILURE_INVALID -> "安装包无效"
        PackageInstaller.STATUS_FAILURE_STORAGE -> "存储空间不足"
        else -> "安装失败（$status）"
    }
}
