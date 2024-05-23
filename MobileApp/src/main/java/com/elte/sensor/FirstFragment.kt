package com.elte.sensor

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.elte.sensor.common.Constants.MESSAGE_PATH_RECORDING_STARTED
import com.elte.sensor.common.Constants.MESSAGE_PATH_RECORDING_STOPPED
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

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFirstBinding.inflate(inflater, container, false)
        binding.refreshConnectedNodes.setOnClickListener {
            findAllWearDevices()
        }
        binding.imageView.setImageResource(R.drawable.bear)

        binding.accX.text = "acc-x: 0.0"
        binding.accY.text = "acc-y: 0.0"
        binding.accZ.text = "acc-z: 0.0"
        binding.gyroX.text = "gyro-x: 0.0"
        binding.gyroY.text = "gyro-y: 0.0"
        binding.gyroZ.text = "gyro-z: 0.0"
        binding.startRecordingBtn.visibility = View.VISIBLE
        binding.startRecordingBtn.setOnClickListener {
            startRecording()
        }
        binding.stopRecordingBtn.visibility = View.INVISIBLE
        binding.stopRecordingBtn.setOnClickListener {
            stopRecording()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findAllWearDevices()
    }

    private fun stopRecording() {
        try {
            sendMessageToConnectedNodes(MESSAGE_PATH_RECORDING_STOPPED)
            binding.connectionStatus.text =
                "Recording stopped. File successfully saved in Downloads folder."
        } catch (throwable: Throwable) {
            Log.e(TAG, throwable.toString())
            binding.connectionStatus.text = throwable.message +
                    " Make sure the watch is connected to the phone" +
                    " via the Galaxy Watch app."
        } finally {
            binding.startRecordingBtn.visibility = View.VISIBLE
            binding.stopRecordingBtn.visibility = View.INVISIBLE
        }
    }

    private fun startRecording() {
        try {
            sendMessageToConnectedNodes(MESSAGE_PATH_RECORDING_STARTED)

            binding.startRecordingBtn.visibility = View.INVISIBLE
            binding.stopRecordingBtn.visibility = View.VISIBLE
            binding.connectionStatus.text = "Recording in progress..."
//            while (true) {
//                updateSensorData()
//            }
        } catch (throwable: Throwable) {
            Log.e(TAG, throwable.toString())
            binding.connectionStatus.text = throwable.message +
                    " Make sure the watch is connected to the phone" +
                    " via the Galaxy Watch app.."
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
