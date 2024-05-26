package com.elte.sensor

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
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
import org.checkerframework.common.value.qual.StaticallyExecutable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * First fragment.
 * @version 1.0 2024-04-13
 * @author Wittawin Panta
 */
class FirstFragment : Fragment() {

    private lateinit var binding: FragmentFirstBinding
    private var connectedNodes: List<Node> = emptyList()
    private var isRecording = false
    var timeAfterRecord = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFirstBinding.inflate(inflater, container, false)
        binding.refreshConnectedNodes.setOnClickListener {
            findAllWearDevices()
        }
        binding.imageView.setImageResource(R.drawable.clapping)
        binding.imageView.visibility = View.INVISIBLE

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
        binding.imageView.visibility = View.INVISIBLE
        binding.predictedText.visibility = View.INVISIBLE
    }

    private fun setTextVisible() {
        binding.imageView.visibility = View.VISIBLE
        binding.predictedText.visibility = View.VISIBLE
    }

    private fun startRecording() {
        try {
            isRecording = true
            sendMessageToConnectedNodes(MESSAGE_PATH_RECORDING_STARTED)

            binding.startRecordingBtn.visibility = View.INVISIBLE
            binding.stopRecordingBtn.visibility = View.VISIBLE
            setTextVisible()
            binding.connectionStatus.text = "Recording in progress..."
            binding.openDownloadsBtn.visibility = View.INVISIBLE
            updatePrediction()
            recordingTimeUIStart()
        } catch (throwable: Throwable) {
            isRecording = false
            Log.e(TAG, throwable.toString())
            binding.connectionStatus.text = throwable.message +
                    " Make sure the watch is connected to the phone" +
                    " via the Galaxy Watch app.."
        }
    }

    private fun recordingTimeUIStart() {
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        scheduler.scheduleAtFixedRate({
            if (isRecording) {
                timeAfterRecord += 1
                val hours = timeAfterRecord / 3600000
                val minutes = (timeAfterRecord / 60000) % 60
                val seconds = (timeAfterRecord / 1000) % 60
                val milliseconds = timeAfterRecord % 1000
                requireActivity().runOnUiThread {
                    binding.recordingTime.text = String.format(
                        "%02d:%02d:%02d:%03d",
                        hours, minutes, seconds, milliseconds
                    )
                }
            } else {
                scheduler.shutdown()
            }
        }, 0, 100, TimeUnit.MILLISECONDS)
    }

    private fun updatePrediction(){
        Thread(Runnable {
            while (isRecording) {
                try {
                    Thread.sleep(1000)
                    binding.predictedText.text = prediction
                    if (prediction.contains("Clapping")) {
                        binding.imageView.setImageResource(R.drawable.clapping)
                    } else if (prediction.contains("Hopscotch")) {
                        binding.imageView.setImageResource(R.drawable.hopscotch)
                    } else if ( prediction.contains("Bear") ) {
                        binding.imageView.setImageResource(R.drawable.bear)
                    } else if ( prediction.contains("Drawing") ) {
                        binding.imageView.setImageResource(R.drawable.drawing)
                    } else if ( prediction.contains("Goliath") ) {
                        binding.imageView.setImageResource(R.drawable.goliath)
                    } else if ( prediction.contains("Puding_Eat") ) {
                        binding.imageView.setImageResource(R.drawable.pudding_eat)
                    } else if ( prediction.contains("Spider") ) {
                        binding.imageView.setImageResource(R.drawable.spider)
                    } else {
                        binding.predictedText.text = "Unknown Movement"
                        binding.imageView.setImageResource(R.drawable.unknown)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending prediction to phone", e)
                }
            }
        }).start()
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
            timeAfterRecord = 0
            isRecording = false
            binding.startRecordingBtn.visibility = View.VISIBLE
            binding.stopRecordingBtn.visibility = View.INVISIBLE
            binding.openDownloadsBtn.visibility = View.VISIBLE
        }
    }

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
        var prediction = ""
        var sensorNumber = 0
    }
}
