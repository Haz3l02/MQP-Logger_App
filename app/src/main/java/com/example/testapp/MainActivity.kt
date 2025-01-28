package com.example.testapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var btnStart : Button
    lateinit var btnStop : Button
    lateinit var btnShare : Button

    lateinit var tempTv : TextView
    lateinit var recordStatusTv : TextView

    // variables used to put data into the csv
    var globalTemp = 0.0
    var globalBatLvl = 0.0
    var globalCharging = false
    var globalProxSensor = "n/a"
    var globalAccelX = 0.0
    var globalAccelY = 0.0
    var globalAccelZ = 0.0


    var globalRecordingStarted = false
    var recordingNow = false

    // Proximity sensor stuff:
    lateinit var proximitySensor: Sensor
    lateinit var proxSensorManager: SensorManager

    lateinit var accelSensorManager: SensorManager
    lateinit var accelSensor: Sensor

    object importantVars{
        var globalTemp = 0.0
        var globalBatLvl = 0.0
        var globalCharging = false
        var globalProxSensor = "n/a"
        var globalAccelX = 0.0
        var globalAccelY = 0.0
        var globalAccelZ = 0.0
        //var tempTv = null
        //var recordStatusTv = null
        var recordingNow = false
    }

    private val receiver: BroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // get battery temperature programmatically from intent
            // battery temperature in tenths of a degree centigrade
            intent?.apply {
                val temp = getIntExtra(
                    BatteryManager.EXTRA_TEMPERATURE, 0
                ) / 10F

                // store current temp value for use in other parts of the program
                globalTemp = temp.toDouble()
                importantVars.globalTemp = globalTemp

                var charging = getIntExtra(
                    BatteryManager.EXTRA_PLUGGED, 0
                )

                if(charging != 0){
                    globalCharging = true
                    importantVars.globalCharging = globalCharging
                }
                else{
                    importantVars.globalCharging = false
                }

                var batteryLevel = getIntExtra(
                    BatteryManager.EXTRA_LEVEL, 0
                )

                globalBatLvl = batteryLevel.toDouble()
                importantVars.globalBatLvl = globalBatLvl

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnStart = findViewById(R.id.btn_start)
        btnStop = findViewById(R.id.btn_stop)
        btnShare = findViewById((R.id.btn_share))

        tempTv = findViewById(R.id.temp_tv)
        recordStatusTv = findViewById(R.id.recordStatusTv)

        btnStart.setOnClickListener(this)
        btnStop.setOnClickListener(this)
        btnShare.setOnClickListener(this)


        // initialize a new intent filter instance
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

        // register the broadcast receiver
        registerReceiver(receiver,filter)

        // on below line we are initializing our sensor manager
        proxSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accelSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // on below line we are initializing our proximity sensor variable
        proximitySensor = proxSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)!!

        accelSensor = accelSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!




        var proximitySensorEventListener: SensorEventListener? = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // check if the sensor type is proximity sensor.
                if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                    if (event.values[0] == 0f) {
                        // if the sensor event returns 0, then it's close
                        // some phones have an actual distance value returned here, though
                        // the only thing we care about is whether it's 0

                        globalProxSensor = "Near"
                    } else {
                        // sensor says object is far from sensor
                        globalProxSensor = "Far"
                    }
                    importantVars.globalProxSensor = globalProxSensor
                }
//                else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER){
//
//
//                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // not used
            }
        }

        var accelSensorEventListener: SensorEventListener? = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // check if the sensor type is proximity sensor.
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {

                    globalAccelX = event.values[0].toDouble()
                    globalAccelY = event.values[1].toDouble()
                    globalAccelZ = event.values[2].toDouble()

                    importantVars.globalAccelX = globalAccelX
                    importantVars.globalAccelY = globalAccelY
                    importantVars.globalAccelZ = globalAccelZ

                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // not used
            }
        }

        // unreachable code probably (it will never == null)
        if (proximitySensor == null){
            print("No Proximity Sensor found on device!")
            globalProxSensor = "Error: Sensor not found"
        }
        else{
            proxSensorManager.registerListener(
                proximitySensorEventListener,
                proximitySensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        accelSensorManager.registerListener(
            accelSensorEventListener,
            accelSensor,
            10000000  // milliseconds btw (== 10 seconds, change to 600... for final version)
        )

        // start the background recording service
        startService(Intent(this, DataRecordingService::class.java))


    }

    override fun onClick(v: View?) {
        // note: seconds = value/1000 -- so 10_000 --> 10 sec
        val delayValue = 10_000.toLong()
        //var numIterations = 0

        val path = getExternalFilesDir(null)
        val fileOut = File(path, "MQP_data.csv")


        when(v?.id){
            R.id.btn_start ->{
                // start logging temp and other metrics
                println("start recording data")

                recordStatusTv.text = "Recording Status: On"

                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                // ONLY run the recording thread if one isn't active already
                // to prevent duplicate instances of the lifecycle, each recording data separately
                if(globalRecordingStarted == false) {

                    globalRecordingStarted = true

                    // thread-thing that runs concurrently every X seconds (60_000 = 60 sec), see delayValue
                    this.lifecycleScope.launch() {


                        val path = getExternalFilesDir(null)
                        val fileOut = File(path, "MQP_data.csv")

                        // initialize CSV file
                        fileOut.appendText(
                            "Timestamp, Battery Temp, Charging Status, Battery Level %, Proximity, Accel X (m/s^2), Accel Y, Accel Z \n")



                        while (true) {

                            if (recordStatusTv.text == "Recording Status: On") {

                                // let the service thread know to start recording data
                                recordingNow = true
                                importantVars.recordingNow = true

                                // update temperature display
                                tempTv.text = "Battery Temperature: $globalTemp${0x00B0.toChar()}C"

                            }

                            delay(delayValue)
                        } // while loop
                    }   // lifecycle launch
                } // if statement
            }
            R.id.btn_stop ->{
                // stop logging temp and other metrics

                // cancels the coroutine logging the data (and you can't restart it without closing the app)
                //this.lifecycleScope.cancel()

                println("stop recording data")

                recordStatusTv.text = "Recording Status: Off"

                recordingNow = false
                importantVars.recordingNow = false


                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            R.id.btn_share ->{

                val sendIntent = Intent(Intent.ACTION_SEND)
                sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                sendIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this,"${application.packageName}.provider", fileOut))
                sendIntent.type = "text/csv"
                startActivity(Intent.createChooser(sendIntent, "SHARE"))

            }
        }

    }
}