package com.example.montaje_tamer.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object UpdateManager {
    private const val GITHUB_API_URL = "https://api.github.com/repos/Fabricio285/Montaje_Tamer/releases/latest"
    private val client = OkHttpClient()

    suspend fun checkForUpdates(context: Context, currentVersion: String, onUpdateAvailable: (String, String) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(GITHUB_API_URL).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext
                    val jsonData = response.body?.string() ?: return@withContext
                    val jsonObject = JSONObject(jsonData)
                    val latestVersion = jsonObject.getString("tag_name") // Ejemplo: "V4.0.1"

                    if (latestVersion != currentVersion) {
                        val assets = jsonObject.getJSONArray("assets")
                        if (assets.length() > 0) {
                            val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
                            withContext(Dispatchers.Main) {
                                onUpdateAvailable(latestVersion, downloadUrl)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                FileLogger.logInfo("Error al buscar actualizaciones: ${e.message}")
            }
        }
    }

    suspend fun downloadAndInstallApk(context: Context, downloadUrl: String) {
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(downloadUrl).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext

                    val apkFile = File(context.externalCacheDir, "update.apk")
                    if (apkFile.exists()) apkFile.delete()

                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(apkFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        installApk(context, apkFile)
                    }
                }
            } catch (e: Exception) {
                FileLogger.logInfo("Error al descargar APK: ${e.message}")
            }
        }
    }

    private fun installApk(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        
        // El APK se borrará en el próximo inicio o podemos intentar borrarlo después de un tiempo
        // pero técnicamente el instalador necesita que el archivo exista durante el proceso.
        // Una buena práctica es borrar archivos viejos en checkForUpdates.
    }
    
    fun cleanOldUpdates(context: Context) {
        try {
            val apkFile = File(context.externalCacheDir, "update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
                FileLogger.logInfo("APK de actualización anterior borrado.")
            }
        } catch (e: Exception) {
            // Ignorar
        }
    }
}
