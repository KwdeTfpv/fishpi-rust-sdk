package dev.fishpi.mobile.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import dev.fishpi.mobile.FishPiNotifier

class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @Suppress("DEPRECATION")
            val confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT) as? Intent
            if (confirm != null) {
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(confirm) }
            }
            return
        }
        val detail = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()
        val label = UpdateInstaller.statusMessage(status)
        val store = SessionStore(context)
        store.saveUpdateDownloadAt(System.currentTimeMillis())
        store.saveUpdateDownloadResult(if (detail.isBlank()) label else "$label：$detail")
        if (status != PackageInstaller.STATUS_SUCCESS &&
            status != PackageInstaller.STATUS_FAILURE_ABORTED
        ) {
            FishPiNotifier.error(label)
        }
    }
}
