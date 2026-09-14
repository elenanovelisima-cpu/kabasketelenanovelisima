package com.example.vision

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

/**
 * Helper to manage system notifications for background video processing.
 * Creates the high-priority notification channel and posts push notifications
 * when video analysis finishes.
 */
object VideoAnalysisNotificationHelper {

    const val CHANNEL_ID = "kabasket_video_analysis_channel"
    const val NOTIFICATION_ID = 2024
    const val EXTRA_OPEN_ANALYZED_VIDEO = "extra_open_analyzed_video"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Análisis de Vídeo KaBasket",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos de finalización y progreso del análisis de vídeo táctico y de tiro"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                enableLights(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showAnalysisCompletedNotification(
        context: Context,
        totalFrames: Int,
        makes: Int,
        attempts: Int,
        tacticalSummary: String? = null
    ) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_ANALYZED_VIDEO, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "¡Análisis de vídeo completado! 🏀"
        val body = if (attempts > 0) {
            "Análisis listo: $makes/$attempts canastas en $totalFrames fotogramas. Toca para ver el partido."
        } else {
            "Análisis listo: $totalFrames fotogramas procesados con detección táctica. Toca para ver el informe."
        }

        val expandedText = buildString {
            append(body)
            if (!tacticalSummary.isNullOrBlank()) {
                append("\n\n")
                append(tacticalSummary)
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            android.util.Log.w("NotificationHelper", "Cannot post notification (permission missing): ${e.message}")
        }
    }
}
