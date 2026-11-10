package com.kgsoft.kait.studio

import android.app.Application
import android.content.Intent
import java.io.File
import kotlin.system.exitProcess

/**
 * Installs the global error boundary: any uncaught exception is saved to
 * crash.log and the user is taken to [ErrorActivity] instead of the app
 * simply disappearing. No crash report ever leaves the device.
 */
class KAITApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        LocaleHelper.applySavedLanguage(this)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val log = File(filesDir, "crash.log")
                log.writeText(
                    "KAIT Studio crash\n" +
                        "Thread: ${thread.name}\n" +
                        throwable.stackTraceToString()
                )
                val intent = Intent(this, ErrorActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(ErrorActivity.EXTRA_MESSAGE, throwable.toString())
                }
                startActivity(intent)
            } catch (_: Exception) {
                // If even the boundary fails, fall back to the system handler.
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
                exitProcess(2)
            }
        }
    }
}
