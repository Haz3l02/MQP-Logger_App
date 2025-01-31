package com.example.testapp

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.example.testapp.MainActivity.importantVars.globalAccelX
import com.example.testapp.MainActivity.importantVars.globalAccelY
import com.example.testapp.MainActivity.importantVars.globalAccelZ
import com.example.testapp.MainActivity.importantVars.globalBatLvl
import com.example.testapp.MainActivity.importantVars.globalCharging
import com.example.testapp.MainActivity.importantVars.globalProxSensor
import com.example.testapp.MainActivity.importantVars.globalTemp
import com.example.testapp.MainActivity.importantVars.recordingNow

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DataRecordingService : Service() {


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // do your jobs here

        print("service started! \n")


        val path = getExternalFilesDir(null)
        val fileOut = File(path, "MQP_data.csv")



        //delete any file object with path and filename that already exists
        //fileOut.delete()


        Thread {


            while (true) {

                if (recordingNow == true) {


                    println("recorded temp: $globalTemp")

                    //val secondsElapsed = (delayValue * numIterations) / 1000

                    val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")
                    val currentTime = LocalDateTime.now().format(formatter)

                    // add a new line of data to CSV
                    val data =
                        "$currentTime, $globalTemp, $globalCharging, $globalBatLvl%, $globalProxSensor, " +
                                "$globalAccelX, $globalAccelY, $globalAccelZ \n"

                    fileOut.appendText(data)
                }

                Thread.sleep(10_000)
            } // while loop
        }.start()


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

        val name = "MQP App Data Recording"
        val descriptionText = "Necessary to Record Data with this App"
        val importance = NotificationManager.IMPORTANCE_LOW
        val mChannel = NotificationChannel(NOTIF_CHANNEL_ID, name, importance)
        mChannel.description = descriptionText

        // creates the notification channel
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)



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