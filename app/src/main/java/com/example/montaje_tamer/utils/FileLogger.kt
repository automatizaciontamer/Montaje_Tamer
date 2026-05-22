package com.example.montaje_tamer.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object FileLogger {
    private const val TAG = "FileLogger"

    fun logInfo(message: String) {
        writeLog("INFO", message)
    }

    fun logError(context: Context?, message: String, throwable: Throwable? = null) {
        writeLog("ERROR", "$message\n${throwable?.stackTraceToString() ?: ""}")
    }

    fun setupCrashLogger() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            writeLog("CRASH", "Uncaught exception in thread ${thread.name}: ${throwable.message}\n${throwable.stackTraceToString()}")
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun writeLog(level: String, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $level: $message\n\n"
        
        try {
            val fileName = "montaje_tamer_log.txt"
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            
            val file = File(downloadDir, fileName)
            FileOutputStream(file, true).use {
                it.write(logEntry.toByteArray())
            }
            Log.d(TAG, "Log written to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error writing log to file", e)
        }
    }
    fun openFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            logError(context, "Error al abrir el archivo: ${file.name}", e)
        }
    }
}
