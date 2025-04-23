package mc.assignment3.matrixcalculator

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import mc.assignment3.matrixcalculator.databinding.ActivityWifiLoggingBinding

class WifiLoggingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWifiLoggingBinding
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiReceiver: BroadcastReceiver
    private val LOCATION_PERMISSION_CODE = 101

    private val locationData = mutableMapOf(
        "Room A" to mutableListOf<Int>(),
        "Room B" to mutableListOf<Int>(),
        "Corridor" to mutableListOf<Int>()
    )
    private var selectedLocation: String = "Room A"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWifiLoggingBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // 🔥 Add this to populate the spinner
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.locations_array,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.locationSpinner.adapter = adapter


        // Setup spinner
        binding.locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedLocation = parent.getItemAtPosition(position).toString()
                displayMatrix(selectedLocation)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.startScanBtn.setOnClickListener {
            checkAndRequestPermissions()
        }

    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        } else {
            startWifiScan()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startWifiScan()
        } else {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startWifiScan() {
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        wifiReceiver = object : BroadcastReceiver() {
            @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            override fun onReceive(context: Context?, intent: Intent?) {
                val results: List<ScanResult> = wifiManager.scanResults
                unregisterReceiver(this)


                val rssi = results.firstOrNull()?.level ?: return
                val list = locationData[selectedLocation] ?: return
                binding.sampleCount.text = "${list.size + 1} / 100 samples collected"

                if (list.size < 100) {
                    list.add(rssi)
                    binding.logText.append("[$selectedLocation] Sample ${list.size}: $rssi dBm\n")
                }

                if (list.size < 100) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        startWifiScan()
                    }, 2000)
                } else {
                    Toast.makeText(this@WifiLoggingActivity, "100 samples collected for $selectedLocation", Toast.LENGTH_LONG).show()
                    displayMatrix(selectedLocation)
                }
            }
        }

        registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        wifiManager.startScan()
    }

    private fun displayMatrix(location: String) {
        val values = locationData[location] ?: return
        binding.matrixGrid.removeAllViews()

        for (rssi in values) {
            val cell = TextView(this).apply {
                text = "$rssi"
                setPadding(12, 6, 12, 6)
                textSize = 12f
                setBackgroundColor(getColorForRSSI(rssi))
            }
            binding.matrixGrid.addView(cell)
        }
    }

    private fun getColorForRSSI(rssi: Int): Int {
        return when {
            rssi >= -50 -> 0xFF81C784.toInt() // Strong (green)
            rssi >= -70 -> 0xFFFFF176.toInt() // Medium (yellow)
            else -> 0xFFE57373.toInt()        // Weak (red)
        }
    }
}
