package com.example.testapp

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat




class DataRecordingService : Service() {


    var notificationManager: NotificationManagerCompat? = null


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // do your jobs here

        print("service started!")


        startForeground()

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForeground() {
        val notificationIntent: Intent = Intent(
            this,
            MainActivity::class.java
        )

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val CHANNEL_ID = "10"
        val channel = NotificationChannel(
            CHANNEL_ID,
            "MQPAppChannel",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "MQP App channel for foreground service notification"

        notificationManager = NotificationManagerCompat.from(this);

        notificationManager = getSystemService<NotificationManager>(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)



        startForeground(
            NOTIF_ID, NotificationCompat.Builder(
                this,
                NOTIF_CHANNEL_ID
            ) // don't forget create a notification channel first
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_dialog_info)
                .setContentTitle("MQP Data Logger App")
                .setContentText("Service is running in the background")
                .setContentIntent(pendingIntent)
                .build()
        )
    }

    companion object {
        private const val NOTIF_ID = 1
        private const val NOTIF_CHANNEL_ID = "Channel_Id"
    }
}