package com.robloxguncel.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * DownloadManager ile indirilen APK'yi sistemin kurulum ekranina yonlendirir.
 * Android 8+ icin "bilinmeyen kaynaklar" izni yoksa ayarlara yonlendirir.
 */
object InstallHelper {

    fun tryInstall(context: Context, downloadId: Long) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val q = DownloadManager.Query().setFilterById(downloadId)
        var status = -1
        var uri: Uri? = null
        dm.query(q)?.use { c: Cursor ->
            if (c.moveToFirst()) {
                status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                uri = dm.getUriForDownloadedFile(downloadId)
            }
        }
        if (status != DownloadManager.STATUS_SUCCESSFUL) return
        val apkUri = uri ?: return

        if (!canInstall(context)) {
            requestInstallPermission(context)
            return
        }
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(i)
        } catch (e: Exception) {
            // Kurucu aktivite bulunamadi; kullanici indirme bildiriminden de kurabilir.
        }
    }

    fun canInstall(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val i = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + context.packageName)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(i)
            } catch (e: Exception) {
                // Bazi romlarda intent yok; kullanici manuel olarak izin verebilir.
            }
        }
    }
}
