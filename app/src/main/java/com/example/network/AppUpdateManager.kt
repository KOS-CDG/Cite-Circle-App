package com.example.network

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"

    fun startDownloadAndInstall(context: Context, updateInfo: SupabaseClient.AppUpdateInfo) {
        if (updateInfo.downloadUrl.isBlank()) {
            Toast.makeText(context, "Download link not available", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "Downloading Cite Circle v${updateInfo.latestVersionName}...", Toast.LENGTH_LONG).show()

        try {
            val uri = Uri.parse(updateInfo.downloadUrl)
            val request = DownloadManager.Request(uri).apply {
                setTitle("Cite Circle v${updateInfo.latestVersionName}")
                setDescription("Downloading academic update...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "CiteCircle-${updateInfo.latestVersionName}.apk")
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                    if (id == downloadId) {
                        try {
                            context.unregisterReceiver(this)
                        } catch (e: Exception) {
                            // Already unregistered
                        }
                        installApk(context, updateInfo.latestVersionName)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            }
        } catch (e: Exception) {
            Log.e(TAG, "DownloadManager failed, opening in browser", e)
            openInBrowser(context, updateInfo.downloadUrl)
        }
    }

    fun installApk(context: Context, versionName: String) {
        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "CiteCircle-$versionName.apk")
            if (!file.exists()) {
                Log.e(TAG, "Downloaded APK file not found at ${file.absolutePath}")
                return
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
            Toast.makeText(context, "Update downloaded. Please open Downloads to install.", Toast.LENGTH_LONG).show()
        }
    }

    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open download link in browser", e)
        }
    }
}
