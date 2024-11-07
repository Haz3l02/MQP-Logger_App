package com.example.testapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.io.File
import java.util.Locale

class MainActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var btnStart : Button
    lateinit var btnStop : Button

    lateinit var tempTv : TextView
    lateinit var voltTv : TextView

    var globalTemp = 0.0
    var globalVoltage = 0.0



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

                var voltage = getIntExtra(
                    BatteryManager.EXTRA_VOLTAGE, 0
                ) / 1F

                // some devices have voltage in mV, some in Volts, this ensures all are in Volts
                if(voltage > 1000){
                    voltage /= 1000F
                }
                // store current voltage as a global variable
                globalVoltage = voltage.toDouble()
                // round to 2 decimal places
                String.format(Locale.ENGLISH ,"%.2f", globalVoltage)

                // show the battery temperate in text view
                // 0x00B0 is the degree symbol in ASCII !!!
                //tempTv.text = "Battery Temperature: $temp${0x00B0.toChar()}C"
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

        tempTv = findViewById(R.id.temp_tv)
        voltTv = findViewById(R.id.volt_tv)

        btnStart.setOnClickListener(this)
        btnStop.setOnClickListener(this)


        // initialize a new intent filter instance
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

        // register the broadcast receiver
        registerReceiver(receiver,filter)


        val path = getExternalFilesDir(null)
        val fileOut = File(path, "MQP_data.csv")

        //delete any file object with path and filename that already exists
        fileOut.delete()

        // initialize CSV file
        fileOut.appendText("Seconds Elapsed, Battery Temp, Battery Voltage \n")
        //csvWriter().writeAll(rows, "MQP_data.csv")
    }

    override fun onClick(v: View?) {
        // note: seconds = value/1000 -- so 10_000 --> 10 sec
        val delayValue = 10_000.toLong()
        var numIterations = 0

        val path = getExternalFilesDir(null)
        val fileOut = File(path, "MQP_data.csv")


        when(v?.id){
            R.id.btn_start ->{
                // start logging temp and other metrics
                println("start recording data")

                //tempTv.text = "Battery Temperature: $btemp C"
                //voltTv.text = "Battery Voltage: $bvolt"
                //println(this.lifecycleScope.isActive)

                this.lifecycleScope.launch() {

                    // thread-thing that runs concurrently every X seconds (60_000 = 60 sec), see delayValue
                    while(true) {
                        tempTv.text = "Battery Temperature: $globalTemp${0x00B0.toChar()}C"
                        voltTv.text = "Battery Voltage: $globalVoltage V"
                        //println(this.isActive)
                        // for keeping track of timing
                        numIterations++

                        //println("recorded temp: $globalTemp")

                        // add a new line of data to CSV
                        val secondsElapsed = (delayValue * numIterations) / 1000
                        val data = "$secondsElapsed, $globalTemp, $globalVoltage \n"

                        fileOut.appendText(data)

                        delay(delayValue)
                    }
                }
            }
            R.id.btn_stop ->{
                // stop logging temp and other metrics

                // cancels the coroutine logging the data (and you can't restart it without closing the app)
                this.lifecycleScope.cancel()
                //println(this.lifecycleScope.isActive)
                println("stop recording data")

            }
        }

    }
}