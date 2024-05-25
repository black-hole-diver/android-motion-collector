package com.elte.sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.elte.sensor.common.Constants.MESSAGE_PATH_RECORDING_STARTED
import com.elte.sensor.common.Constants.MESSAGE_PATH_RECORDING_STOPPED
import com.elte.sensor.common.Constants.MESSAGE_PATH_REQUEST_SENSOR_DATA
import com.elte.sensor.databinding.FragmentFirstBinding
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.android.gms.wearable.*

/**
 * First fragment.
 * @version 1.0 2024-04-13
 * @author Wittawin Panta
 */
class FirstFragment : Fragment() {

    private lateinit var binding: FragmentFirstBinding
    private var connectedNodes: List<Node> = emptyList()
    private var isRecording = false

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private val sensorDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                val timestamp = intent.getStringExtra("timestamp")
                val name = intent.getStringExtra("name")
                val values = intent.getStringExtra("values")
                val accuracy = intent.getStringExtra("accuracy")
                val coords = intent.getStringExtra("coords")
                val type = intent.getStringExtra("type")

                updateSensorData(timestamp, name, values, accuracy, coords, type)
            }
        }
    }

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            var numberOfSensors = sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE_UNCALIBRATED).size + sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE).size + sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size + sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED).size
            binding.availableSensors.text = "Available sensors: ${numberOfSensors}"
            val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            var accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accSensor == null) {
                Log.e(TAG, "Accelerometer sensor not available")
                binding.accX.text = "acc-x: Accelerometer sensor not available"
                binding.accY.text = "acc-y: Accelerometer sensor not available"
                binding.accZ.text = "acc-z: Accelerometer sensor not available"
            }
            if (gyroSensor == null) {
                Log.e(TAG, "Gyroscope sensor not available")
                binding.gyroX.text = "gyro-x: Gyroscope sensor not available"
                binding.gyroY.text = "gyro-y: Gyroscope sensor not available"
                binding.gyroZ.text = "gyro-z: Gyroscope sensor not available"
            }
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    binding.accX.text = "acc-x: ${event.values[0]}"
                    binding.accY.text = "acc-y: ${event.values[1]}"
                    binding.accZ.text = "acc-z: ${event.values[2]}"
                }
                Sensor.TYPE_GYROSCOPE -> {
                    binding.gyroX.text = "gyro-x: ${event.values[0]}"
                    binding.gyroY.text = "gyro-y: ${event.values[1]}"
                    binding.gyroZ.text = "gyro-z: ${event.values[2]}"
                }
            }
            Log.d(TAG, "Sensor changed: ${event.sensor.type} - ${event.values.contentToString()}")
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            Log.d(TAG, "Accuracy changed for: ${sensor.type} - $accuracy")
        }
    }

    private fun updateSensorData(timestamp: String?, name: String?, values: String?, accuracy: String?, coords: String?, type: String?) {
        val sensorData = """
            Timestamp: $timestamp
            Name: $name
            Values: $values
            Accuracy: $accuracy
            Coords: $coords
            Type: $type
        """.trimIndent()

        binding.connectionStatus.text = sensorData
        if (name == "Accelerometer") {
            binding.accX.text = "acc-x: ${values?.split("#")?.get(0)?.trim()}"
            binding.accY.text = "acc-y: ${values?.split("#")?.get(1)?.trim()}"
            binding.accZ.text = "acc-z: ${values?.split("#")?.get(2)?.trim()}"
        } else if (name == "Gyroscope") {
            binding.gyroX.text = "gyro-x: ${values?.split("#")?.get(0)?.trim()}"
            binding.gyroY.text = "gyro-y: ${values?.split("#")?.get(1)?.trim()}"
            binding.gyroZ.text = "gyro-z: ${values?.split("#")?.get(2)?.trim()}"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFirstBinding.inflate(inflater, container, false)
        binding.refreshConnectedNodes.setOnClickListener {
            findAllWearDevices()
        }
        binding.imageView.setImageResource(R.drawable.bear)
        binding.imageView.visibility = View.INVISIBLE

        binding.accX.text = "acc-x: 0.0"
        binding.accY.text = "acc-y: 0.0"
        binding.accZ.text = "acc-z: 0.0"
        binding.gyroX.text = "gyro-x: 0.0"
        binding.gyroY.text = "gyro-y: 0.0"
        binding.gyroZ.text = "gyro-z: 0.0"
        setTextInvisible()
        binding.startRecordingBtn.visibility = View.VISIBLE
        binding.startRecordingBtn.setOnClickListener {
            startRecording()
        }

        binding.stopRecordingBtn.visibility = View.INVISIBLE
        binding.stopRecordingBtn.setOnClickListener {
            stopRecording()
        }
        binding.openDownloadsBtn.visibility = View.VISIBLE
        binding.openDownloadsBtn.setOnClickListener {
            openDownloadsFolder()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findAllWearDevices()
        updateSensorData()
    }

    private fun openDownloadsFolder() {
        try {
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val uri: Uri? = context?.let {
                FileProvider.getUriForFile(
                    it,
                    "${context?.packageName}.provider",
                    downloadsFolder
                )
            }

            if (uri != null) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    setDataAndType(uri, "*/*")
                }

                context!!.startActivity(intent)
            } else {
                throw Exception("Uri is null")
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open Downloads folder", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun setTextInvisible() {
        binding.accX.visibility = View.INVISIBLE
        binding.accY.visibility = View.INVISIBLE
        binding.accZ.visibility = View.INVISIBLE
        binding.gyroX.visibility = View.INVISIBLE
        binding.gyroY.visibility = View.INVISIBLE
        binding.gyroZ.visibility = View.INVISIBLE
        binding.imageView.visibility = View.INVISIBLE
    }

    private fun setTextVisible() {
        binding.accX.visibility = View.VISIBLE
        binding.accY.visibility = View.VISIBLE
        binding.accZ.visibility = View.VISIBLE
        binding.gyroX.visibility = View.VISIBLE
        binding.gyroY.visibility = View.VISIBLE
        binding.gyroZ.visibility = View.VISIBLE
        binding.imageView.visibility = View.VISIBLE
    }

    private fun updateSensorData() {
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometer?.let {
            sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroscope?.let {
            sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun startRecording() {
        try {
            isRecording = true
            sendMessageToConnectedNodes(MESSAGE_PATH_RECORDING_STARTED)

            binding.startRecordingBtn.visibility = View.INVISIBLE
            binding.stopRecordingBtn.visibility = View.VISIBLE
            setTextVisible()
            binding.connectionStatus.text = "Recording in progress..."
            val filter = IntentFilter("com.elte.sensor.SENSOR_DATA")
            requireActivity().registerReceiver(sensorDataReceiver, filter)

            connectedNodes.forEach { node ->
                sendMessage(MESSAGE_PATH_REQUEST_SENSOR_DATA, node.id)
            }
            updateSensorData()

            binding.openDownloadsBtn.visibility = View.INVISIBLE
        } catch (throwable: Throwable) {
            isRecording = false
            Log.e(TAG, throwable.toString())
            binding.connectionStatus.text = throwable.message +
                    " Make sure the watch is connected to the phone" +
                    " via the Galaxy Watch app.."
        }
    }
    private fun stopRecording() {
        try {
            sendMessageToConnectedNodes(MESSAGE_PATH_RECORDING_STOPPED)
            binding.connectionStatus.text =
                "Recording stopped. File successfully saved in Downloads folder."
            setTextInvisible()
        } catch (throwable: Throwable) {
            Log.e(TAG, throwable.toString())
            binding.connectionStatus.text = throwable.message +
                    " Make sure the watch is connected to the phone" +
                    " via the Galaxy Watch app."
        } finally {
            isRecording = false
            requireActivity().unregisterReceiver(sensorDataReceiver)
            binding.startRecordingBtn.visibility = View.VISIBLE
            binding.stopRecordingBtn.visibility = View.INVISIBLE
            binding.openDownloadsBtn.visibility = View.VISIBLE
        }
    }
//    private suspend fun updateSensorData() {
//        try {
//            val sensorsClient = Wearable.getSensorsClient(requireActivity())
//            val accelerometerData = sensorsClient.getData("ACCELEROMETER").await()
//            val gyroscopeData = sensorsClient.getData("GYROSCOPE").await()
//            binding.accX.text = "acc-x: ${accelerometerData.x}"
//            binding.accY.text = "acc-y: ${accelerometerData.y}"
//            binding.accZ.text = "acc-z: ${accelerometerData.z}"
//            binding.gyroX.text = "gyro-x: ${gyroscopeData.x}"
//            binding.gyroY.text = "gyro-y: ${gyroscopeData.y}"
//            binding.gyroZ.text = "gyro-z: ${gyroscopeData.z}"
//        } catch (e: Exception) {
//            Log.e(TAG, "Error updating sensor data", e)
//        }
//    }

    private fun findAllWearDevices() {
        lifecycleScope.launch {
            try {
                val nodeClient = Wearable.getNodeClient(activity)
                binding.phoneNodeId.text =
                    getString(R.string.phone_node_id, nodeClient.localNode.await().id)

                Log.i(TAG, "Looking for nodes...")
                val capInfo = Wearable.getCapabilityClient(context).getCapability(
                    "AGL_MOZGASMERES", CapabilityClient.FILTER_REACHABLE
                ).await()
                connectedNodes = ArrayList(capInfo.nodes)
                Log.i(TAG, "Found ${connectedNodes.size} device(s).")
                if (connectedNodes.isNotEmpty()) {
                    val nodeList =
                        connectedNodes.joinToString(separator = "\n") { "${it.displayName} (${it.id})" }
                    binding.startRecordingBtn.isEnabled = true
                    binding.connectionStatus.text = "Watch connected:\n$nodeList"
                } else {
                    binding.startRecordingBtn.isEnabled = false
                    binding.connectionStatus.text = "No connected device found." +
                            " Make sure the watch is connected to the phone" +
                            " via the Galaxy Watch app.."
                }
            } catch (throwable: Throwable) {
                Log.e(TAG, throwable.toString())
                binding.startRecordingBtn.isEnabled = false
                binding.connectionStatus.text =
                    getString(R.string.connection_status, "An error occurred while searching for devices.")
            }
        }
    }

    private fun sendMessageToConnectedNodes(message: String) {
        connectedNodes.forEach { sendMessage(message, it.id) }
    }

    private fun sendMessage(message: String, watchNodeId: String) {
        lifecycleScope.launch {
            Log.i(TAG, "Sending message: $message to $watchNodeId")
            val messageId = Wearable.getMessageClient(activity)
                .sendMessage(watchNodeId, message, byteArrayOf()).await()
            Log.i(TAG, "messageResult $messageId")
        }
    }

    companion object {
        private const val TAG = "FirstFragment"
    }
}
