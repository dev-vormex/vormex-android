package com.kyant.backdrop.catalog.notifications

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kyant.backdrop.catalog.MainActivity
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.location.CrossedPathsLocationService

object CrossedPathsNotificationManager {
    const val CHANNEL = "crossed_paths_event_mode"
    const val NOTIFICATION_ID = 3817
    fun ongoing(context: Context, expiresAt: String): Notification {
        if (Build.VERSION.SDK_INT >= 26) (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(NotificationChannel(CHANNEL, "Crossed Paths Event Mode", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(context, 2, Intent(context, CrossedPathsLocationService::class.java).setAction(CrossedPathsLocationService.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val open = PendingIntent.getActivity(context, 3, Intent(context, MainActivity::class.java).putExtra("open_crossed_paths", true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_notification).setContentTitle("Crossed Paths is active")
            .setContentText("Finding people nearby until Event Mode ends").setOngoing(true).setContentIntent(open)
            .addAction(0, "Stop", stop).setCategory(NotificationCompat.CATEGORY_SERVICE).build()
    }
}
