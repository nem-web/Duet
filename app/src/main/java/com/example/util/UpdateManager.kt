package com.example.util

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionCode: Long = 0L,
    val minRequiredVersion: Long = 0L,
    val downloadUrl: String = "",
    val versionName: String = "1.1.0",
    val releaseNotes: String = ""
) {
    fun isMandatory(installedVersion: Long): Boolean {
        return installedVersion < minRequiredVersion
    }
}

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    data class ReadyToInstall(val file: java.io.File, val info: UpdateInfo) : UpdateState
    data class Error(val message: String) : UpdateState
}

object UpdateManager {
    private const val TAG = "UpdateManager"
    private const val APK_NAME = "duet-update.apk"

    /**
     * Gets the current app version code.
     */
    fun getInstalledVersionCode(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting version code", e)
            1L
        }
    }

    /**
     * Gets the current app version name.
     */
    fun getInstalledVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting version name", e)
            "1.0"
        }
    }

    /**
     * Checks Firestore for update information under the "app_config/latest" document path.
     */
    suspend fun checkForUpdate(firestore: FirebaseFirestore?): Result<UpdateInfo?> {
        return withContext(Dispatchers.IO) {
            try {
                val db = firestore ?: return@withContext Result.failure(Exception("Firestore is not initialized or unavailable"))
                Log.d(TAG, "Checking Firestore 'app_config' for application updates...")
                
                val docRef = db.collection("app_config").document("android")
                val documentSnapshot = Tasks.await(docRef.get(), 10, TimeUnit.SECONDS)
                
                if (!documentSnapshot.exists()) {
                    Log.d(TAG, "No update document found in Firestore 'app_config'.")
                    return@withContext Result.success(null)
                }
                
                val versionCode = documentSnapshot.getLong("versionCode") ?: 0L
                val minRequiredVersion = documentSnapshot.getLong("minRequiredVersion") ?: 0L
                val downloadUrl = documentSnapshot.getString("downloadUrl") ?: ""
                val versionName = documentSnapshot.getString("versionName") ?: "1.1.0"
                val releaseNotes = documentSnapshot.getString("releaseNotes") ?: ""
                
                val updateInfo = UpdateInfo(
                    versionCode = versionCode,
                    minRequiredVersion = minRequiredVersion,
                    downloadUrl = downloadUrl,
                    versionName = versionName,
                    releaseNotes = releaseNotes
                )
                
                Log.d(TAG, "Fetched update details: $updateInfo")
                Result.success(updateInfo)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check update from Firestore", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Triggers the APK file download using DownloadManager.
     */
    fun startApkDownload(context: Context, apkUrl: String): Long {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(apkUrl)
        
        // Clean up pre-existing downloaded update APKs
        val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_NAME)
        if (destinationFile.exists()) {
            destinationFile.delete()
        }
        
        val request = DownloadManager.Request(uri)
            .setTitle("Downloading Duet Update")
            .setDescription("Sideloading the latest release safely...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, APK_NAME)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            request.setRequiresCharging(false)
            request.setRequiresDeviceIdle(false)
        }

        val downloadId = downloadManager.enqueue(request)
        Log.d(TAG, "Enqueued update download ID: $downloadId")
        return downloadId
    }

    /**
     * Periodically monitors the DownloadManager queue for progress updates and completion.
     */
    suspend fun monitorDownloadProgress(
        context: Context,
        downloadId: Long,
        onProgress: (Float) -> Unit
    ): Result<File> {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        
        var completed = false
        var fileResult: Result<File> = Result.failure(Exception("Download failed or timed out"))
        
        withContext(Dispatchers.IO) {
            while (!completed) {
                try {
                    val cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val bytesDownloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val bytesTotalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        
                        if (statusIdx != -1) {
                            val status = cursor.getInt(statusIdx)
                            val bytesDownloaded = if (bytesDownloadedIdx != -1) cursor.getLong(bytesDownloadedIdx) else 0L
                            val bytesTotal = if (bytesTotalIdx != -1) cursor.getLong(bytesTotalIdx) else 0L
                            
                            if (bytesTotal > 0) {
                                val progress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                                withContext(Dispatchers.Main) {
                                    onProgress(progress)
                                }
                            }
                            
                            when (status) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    completed = true
                                    val destFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_NAME)
                                    fileResult = if (destFile.exists()) {
                                        Result.success(destFile)
                                    } else {
                                        Result.failure(Exception("Downloaded update APK not found at target destination"))
                                    }
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    completed = true
                                    val reasonIdx = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                                    val reason = if (reasonIdx != -1) cursor.getInt(reasonIdx) else -1
                                    fileResult = Result.failure(Exception("Download aborted by system. DownloadManager Error code: $reason"))
                                }
                            }
                        }
                        cursor.close()
                    } else {
                        completed = true
                        fileResult = Result.failure(Exception("Download task cancelled or invalidated by the user"))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during download query status", e)
                }
                
                if (!completed) {
                    kotlinx.coroutines.delay(500)
                }
            }
        }
        return fileResult
    }

    /**
     * Triggers the Android package installer flow using the FileProvider content URI.
     */
    fun installApk(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
            Log.d(TAG, "Sent package installer intent for ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
        }
    }

    /**
     * Checks if the app has permission to request installing other packages on Android 8.0+.
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Directs the user to the "Install unknown apps" settings panel for this package.
     */
    fun openInstallSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Could not open manage unknown app sources settings", e)
            }
        }
    }
}
