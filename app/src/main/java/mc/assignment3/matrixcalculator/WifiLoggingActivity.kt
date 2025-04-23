package mc.assignment3.matrixcalculator

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import mc.assignment3.matrixcalculator.databinding.ActivityWifiLoggingBinding
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WifiLoggingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWifiLoggingBinding
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiReceiver: BroadcastReceiver
    private val PERMISSION_CODE = 101
    private val STORAGE_PERMISSION_CODE = 102
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.NEARBY_WIFI_DEVICES
    )

    private val locationData = mutableMapOf(
        "Room A" to mutableListOf<MutableList<Int>>(),
        "Room B" to mutableListOf<MutableList<Int>>(),
        "Corridor" to mutableListOf<MutableList<Int>>()
    )
    private val locationSampleCounts = mutableMapOf("Room A" to 0, "Room B" to 0, "Corridor" to 0)
    private val maxScansPerLocation = 10
    private var selectedLocation: String = "Room A"
    private val topAPs = mutableMapOf<String, String>()
    private var lastScanTime: Long = 0
    private val SCAN_INTERVAL_MS = 30000
    private val csvFileName = "wifi_rss_data.csv"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWifiLoggingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadDataFromCsv()

        val adapter = ArrayAdapter.createFromResource(
            this, R.array.locations_array, android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.locationSpinner.adapter = adapter

        binding.locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedLocation = parent.getItemAtPosition(position).toString()
                displayMatrix(selectedLocation)
                displayMatrixUnion() // Ensure summary table is updated
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.startScanBtn.setOnClickListener {
            checkAndRequestPermissions()
        }
        binding.clearDataBtn.setOnClickListener {
            clearData()
        }
    }

    private fun clearData() {
        locationData.forEach { it.value.clear() }
        locationSampleCounts.forEach { entry -> locationSampleCounts[entry.key] = 0 }
        binding.logText.text = "Logs will appear here..."
        binding.sampleCount.text = "0 / $maxScansPerLocation scans"
        binding.matrixGrid.removeAllViews()
        binding.summaryGrid.removeAllViews() // Clear the summary grid
        val file = File(filesDir, csvFileName)
        if (file.exists()) {
            file.delete()
            Toast.makeText(this, "CSV data cleared", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No CSV data to clear", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_CODE)
        } else {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            } else {
                checkLocationServices()
            }
        }
    }

    private fun checkLocationServices() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }

        if (!isLocationEnabled) {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
            startWifiScan()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            checkLocationServices()
        } else if (requestCode == STORAGE_PERMISSION_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            checkLocationServices()
        } else {
            Toast.makeText(this, "All permissions required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startWifiScan() {
        wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "Please enable WiFi", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanTime < SCAN_INTERVAL_MS) {
            val delay = SCAN_INTERVAL_MS - (currentTime - lastScanTime)
            Toast.makeText(this, "Waiting for scan cooldown (${delay/1000}s)", Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({ startWifiScan() }, delay)
            return
        }

        wifiReceiver = object : BroadcastReceiver() {
            @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            override fun onReceive(context: Context?, intent: Intent?) {
                unregisterReceiver(this)
                lastScanTime = System.currentTimeMillis()
                val results = wifiManager.scanResults
                if (results.isEmpty()) {
                    binding.logText.append("\n[$selectedLocation] Scan ${locationSampleCounts[selectedLocation]!! + 1}: No APs found")
                    scheduleNextScan()
                    return
                }

                val currentLocation = selectedLocation
                val scanCount = (locationSampleCounts[currentLocation] ?: 0) + 1
                locationSampleCounts[currentLocation] = scanCount

                val sortedResults = results.sortedByDescending { it.level }.take(10)
                val rssiValues = mutableListOf<Int>()
                sortedResults.forEach { result ->
                    rssiValues.add(result.level)
                    topAPs[result.BSSID] = result.SSID
                    binding.logText.append("\n[$currentLocation] AP: ${result.SSID}, RSSI: ${result.level} dBm")
                }
                while (rssiValues.size < 10) rssiValues.add(-80)

                locationData[currentLocation]?.add(rssiValues)
                binding.logText.append("\n[$currentLocation] Scan $scanCount: ${rssiValues.joinToString()}")
                binding.sampleCount.text = "${locationData[currentLocation]?.size ?: 0} / $maxScansPerLocation scans"

                if (scanCount < maxScansPerLocation) {
                    scheduleNextScan()
                } else {
                    Toast.makeText(this@WifiLoggingActivity, "$currentLocation complete", Toast.LENGTH_SHORT).show()
                    saveDataToCsv(currentLocation)
                    displayMatrixUnion()
                    displayMatrix(currentLocation) // Update matrix for current location
                }
            }
        }

        registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        if (!wifiManager.startScan()) {
            binding.logText.append("\n[$selectedLocation] Scan failed")
            Toast.makeText(this, "Scan failed. Retrying...", Toast.LENGTH_SHORT).show()
            scheduleNextScan()
        }
    }

    private fun scheduleNextScan() {
        Handler(Looper.getMainLooper()).postDelayed({ startWifiScan() }, SCAN_INTERVAL_MS.toLong())
    }

    private fun saveDataToCsv(location: String) {
        try {
            val file = File(filesDir, csvFileName)
            val isNewFile = !file.exists()
            FileWriter(file, true).use { writer ->
                if (isNewFile) {
                    writer.append("Timestamp,Location,ScanNumber,AP1,AP2,AP3,AP4,AP5,AP6,AP7,AP8,AP9,AP10\n")
                }
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                locationData[location]?.forEachIndexed { index, rssiValues ->
                    writer.append("$timestamp,$location,${index + 1},${rssiValues.joinToString(",")}\n")
                }
            }
            Toast.makeText(this, "Data saved to CSV: $csvFileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadDataFromCsv() {
        val file = File(filesDir, csvFileName)
        if (!file.exists()) return

        try {
            FileReader(file).use { reader ->
                reader.readLines().drop(1).forEach { line ->
                    val parts = line.split(",")
                    if (parts.size < 13) return@forEach
                    val location = parts[1]
                    val rssiValues = parts.subList(3, 13).map { it.toIntOrNull() ?: -80 }.toMutableList()
                    locationData.getOrPut(location) { mutableListOf() }.add(rssiValues)
                    locationSampleCounts[location] = locationData[location]?.size ?: 0
                }
            }
            displayMatrixUnion()
            binding.logText.append("\nLoaded data from CSV:")
            locationData.forEach { (loc, data) ->
                data.forEachIndexed { index, rssiValues ->
                    binding.logText.append("\n[$loc] Scan ${index + 1}: ${rssiValues.joinToString()}")
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun displayMatrix(location: String) {
        binding.matrixGrid.removeAllViews()
        binding.matrixGrid.columnCount = 10
        val matrix = locationData[location] ?: return

        matrix.take(10).forEach { row ->
            row.take(10).forEach { rssi ->
                val cell = TextView(this).apply {
                    text = rssi.toString()
                    setPadding(8, 4, 8, 4)
                    textSize = 12f
                    setBackgroundColor(getColorForRSSI(rssi))
                }
                binding.matrixGrid.addView(cell)
            }
        }
    }

    private fun displayMatrixUnion() {
        binding.summaryGrid.removeAllViews() // Use the new summaryGrid
        binding.summaryGrid.columnCount = 4
        val locations = listOf("Room A", "Room B", "Corridor")

        listOf("Location", "Min RSSI", "Max RSSI", "Avg RSSI").forEach { title ->
            binding.summaryGrid.addView(TextView(this).apply {
                text = title
                setPadding(8, 4, 8, 4)
                setBackgroundColor(0xFFDDDDDD.toInt())
            })
        }

        locations.forEach { loc ->
            val matrix = locationData[loc] ?: return@forEach
            val allRssi = matrix.flatten()
            if (allRssi.isEmpty()) return@forEach
            val min = allRssi.minOrNull() ?: -80
            val max = allRssi.maxOrNull() ?: -80
            val avg = if (allRssi.isNotEmpty()) allRssi.average().toInt() else -80

            listOf(loc, min.toString(), max.toString(), avg.toString()).forEach { value ->
                binding.summaryGrid.addView(TextView(this).apply {
                    text = value
                    setPadding(8, 4, 8, 4)
                    textSize = 12f
                })
            }
        }
    }

    private fun getColorForRSSI(rssi: Int): Int {
        return when {
            rssi >= -50 -> 0xFF81C784.toInt()
            rssi >= -70 -> 0xFFFFF176.toInt()
            else -> 0xFFE57373.toInt()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(wifiReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }
}