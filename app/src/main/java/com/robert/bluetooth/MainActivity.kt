package com.robert.bluetooth

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.*
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_FOUND
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.robert.bluetooth.databinding.ActivityMainBinding
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.app.ActivityCompat.startActivityForResult

import android.location.LocationManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var devicesFound: MutableList<Device> = mutableListOf()
    private val devicesAdapter: PairingAdapter = PairingAdapter(devicesFound)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestPermissions()
        setListener()
        setBluetooth()
    }

    private fun setBluetooth() {
        bluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        registerReceiver(bluetoothActiveReceiver, IntentFilter(ACTION_STATE_CHANGED))

        if (bluetoothAdapter == null) {
            makeToast(NOT_SUPPORTED)
        } else {
            checkBluetoothStatus()
        }
    }

    private fun checkBluetoothStatus() {
        with(bluetoothAdapter?.isEnabled == true) {
            binding.activatedSwitch.isChecked = this
            if (this) {
                bluetoothAdapter?.startDiscovery()
                registerReceiver(discoverReceiver, IntentFilter(ACTION_FOUND))
                binding.pairingDevices.apply {
                    adapter = devicesAdapter
                    layoutManager = LinearLayoutManager(context, VERTICAL, false)
                }
            }
        }
    }

    private fun setListener() = binding.activatedSwitch.apply {
        setOnClickListener {
            val enableBtIntent = Intent(if (isChecked) ACTION_REQUEST_ENABLE else DISABLE_BLUE)
            activityTurnOn.launch(enableBtIntent)
        }
    }

    private fun updateSwitch(isSuccess: Boolean) = binding.activatedSwitch.apply {
        isChecked = if (isSuccess) isChecked else isChecked.not()
    }

    private val bluetoothActiveReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action.equals(ACTION_STATE_CHANGED)) {
                when (intent?.getIntExtra(EXTRA_STATE, ERROR)) {
                    STATE_ON -> {
                        checkBluetoothStatus()
                        makeToast("Bluetooth is On")
                    }
                    STATE_OFF -> makeToast("Bluetooth ON is REQUIRED")
                }
            }
        }
    }

    private val discoverReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_FOUND) {
                val device: BluetoothDevice? = intent.getParcelableExtra(EXTRA_DEVICE)
                device?.let {
                    val index = devicesFound.size
                    devicesFound.add(index, Device(it.name, it.address))
                    devicesAdapter.notifyItemInserted(index)
                }
            }
        }
    }

    private fun listenForPairing() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val devicesList = pairedDevices?.map {
            Device(it.name, it.address)
        }
        binding.pairingDevices.apply {
            adapter = PairingAdapter(devicesList)
            layoutManager = LinearLayoutManager(context, VERTICAL, false)
        }
    }

    private val activityTurnOn =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (it.resultCode) {
                Activity.RESULT_OK -> updateSwitch(true)
                Activity.RESULT_CANCELED -> updateSwitch(false)
            }
        }

    private val activityAskLocation =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (it.resultCode) {
                Activity.RESULT_OK -> checkBluetoothStatus()
                Activity.RESULT_CANCELED -> gpsPermission(true)
            }
        }

    private fun requestPermissions() {
        gpsPermission(false)
        if (checkSelfPermission(FINE_LOCATION_REQUEST) != PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(FINE_LOCATION_REQUEST),
                FINE_LOCATION_REQUEST_CODE
            )
        }
    }

    private fun gpsPermission(isResult: Boolean) {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        when {
            isGpsEnabled.not() && isResult -> makeToast(GPS_REQUIRED)
            isGpsEnabled.not() -> activityAskLocation.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            else -> checkBluetoothStatus()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == FINE_LOCATION_REQUEST_CODE && grantResults.first() == PERMISSION_DENIED) {
            makeToast(LOCATION_REQUIRED)
        } else {
            checkBluetoothStatus()
        }
    }

    private fun makeToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        unregisterReceiver(bluetoothActiveReceiver)
        unregisterReceiver(discoverReceiver)
        super.onDestroy()
    }

    companion object {
        private const val DISABLE_BLUE = "android.bluetooth.adapter.action.REQUEST_DISABLE"
        private const val NOT_SUPPORTED = "Bluetooth not supported"
        private const val LOCATION_REQUIRED = "This app required this permission to be accepted"
        private const val GPS_REQUIRED =
            "This app requires to turn on GPS for Bluetooth related functionalities"
        private const val FINE_LOCATION_REQUEST_CODE = 1001
        private const val FINE_LOCATION_REQUEST = Manifest.permission.ACCESS_FINE_LOCATION
    }
}