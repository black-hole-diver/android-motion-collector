package com.elte.sensor

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import com.elte.sensor.common.Constants
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException

/**
 * Sensor service of the wearable.
 * @author Wittawin Panta
 * @version 2.0 2024-05-26
 */
class SensorService : Service() {
    private lateinit var sensorEventHandler: SensorEventHandler
    private var isRunning = false
    private var phoneNodeId: String? = null

    /**
     * Called by the system when the service is first created.
     */
    override fun onCreate() {
        super.onCreate()
        sensorEventHandler = SensorEventHandler(this)
    }

    /**
     * Called when the service is starting.
     * @param intent The intent supplied to start the service.
     * @param flags Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     * @return The return value indicates what semantics the system should use for the service's current started state.
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        //sensorEventHandler.clearReadings()
        Log.d(TAG, "Starting...")
        phoneNodeId = intent.getStringExtra(Constants.INTENT_PHONE_NODE_ID)
        return if (phoneNodeId == null) {
            Log.e(TAG, "Service was started without phone node ID! Stopping...")
            stopSelf()
            START_NOT_STICKY
        } else {
            startRecording()
            START_STICKY
        }
    }

    /**
     * Called when the service is no longer used and is being destroyed.
     */
    override fun onDestroy() {
        isRunning = false
        Log.d(TAG, "Stopping...")
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.unregisterListener(sensorEventHandler)

        Log.i(TAG, "Opening channel.")
        val channelClient = Wearable.getChannelClient(this@SensorService)
        channelClient.openChannel(
            phoneNodeId!!,
            Constants.CHANNEL_PATH_SENSOR_READING
        ).continueWith {
            channelClient.sendFile(it.result, sensorEventHandler.getFileURI(this@SensorService))
            Log.i(TAG, "Successfully sent recorded data.")
        }
    }

    /**
     * Called when a client is binding to the service with bindService().
     * @param intent The intent supplied to bindService.
     */
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Starts recording sensor data.
     */
    private fun startRecording() {
        isRunning = true
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val availableSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
            .map { i -> i.type }
            .toSet()
        val missingSensors = REQUIRED_SENSORS.keys.subtract(availableSensors)
            .map { i -> REQUIRED_SENSORS[i] }
            .toString()
        Log.d(TAG, "Missing sensors: $missingSensors")

        REQUIRED_SENSORS.keys.intersect(availableSensors).forEach { sensorType ->
            val sensor: Sensor = sensorManager.getDefaultSensor(sensorType)
            sensorManager.registerListener(sensorEventHandler, sensor, SAMPLE_RATE)
        }

        val channelClient = Wearable.getChannelClient(this@SensorService)
        val channelFuture = channelClient.openChannel(phoneNodeId!!, Constants.CHANNEL_SENSOR_NUMBER)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val channel = Tasks.await(channelFuture)
                channelClient.getOutputStream(channel).addOnSuccessListener { outputStream ->
                    try {
                        val sensorNumber = availableSensors.size.toString()
                        outputStream.write(sensorNumber.toByteArray(Charsets.UTF_8))
                        outputStream.flush()
                        Log.i(TAG, "Number of sensors $sensorNumber has been sent.")
                    } finally {
                        try {
                            outputStream.close()
                        } catch (e: IOException) {
                            Log.e(TAG, "Failed to close OutputStream: ${e.localizedMessage}")
                        }
                    }
                }.addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to open OutputStream: ${exception.localizedMessage}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error while opening channel: ${e.localizedMessage}")
            }
        }

        /**
         * Thread to send prediction to phone.
         */
        Thread(Runnable{
            while (isRunning) {
                try {
                    Thread.sleep(1000)
                    Log.d(TAG, "Send prediction to phone")

                    val channelClient = Wearable.getChannelClient(this@SensorService)
                    val channelFuture = channelClient.openChannel(phoneNodeId!!, Constants.CHANNEL_PATH_PREDICTION)
                    val channel = Tasks.await(channelFuture)

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            channelClient.getOutputStream(channel).addOnSuccessListener { outputStream ->
                                try {
                                    val predictionData = sensorEventHandler.getPrediction()
                                    outputStream.write(predictionData)
                                    outputStream.flush()
                                    Log.i(TAG, "The prediction ${predictionData.size} bytes has been sent.")
                                    Log.i(TAG, "Successfully sent prediction.")
                                } catch (e: IOException) {
                                    Log.e(TAG, "Failed to write data to channel: ${e.localizedMessage}")
                                } finally {
                                    try {
                                        outputStream.close()
                                    } catch (e: IOException) {
                                        Log.e(TAG, "Failed to close OutputStream: ${e.localizedMessage}")
                                    }
                                }
                            }.addOnFailureListener { exception ->
                                Log.e(TAG, "Failed to obtain OutputStream: ${exception.localizedMessage}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error while sending prediction: ${e.localizedMessage}")
                        }
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    Log.e(TAG, "Broadcast thread was interrupted", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in broadcast thread: ${e.localizedMessage}")
                }
            }
        }).start()
    }

    companion object {
        private const val TAG = "SensorService"
        private const val SAMPLE_RATE = 20000
        private val REQUIRED_SENSORS = mapOf(
            Sensor.TYPE_ACCELEROMETER to Sensor.STRING_TYPE_ACCELEROMETER,
            Sensor.TYPE_GRAVITY to Sensor.STRING_TYPE_GRAVITY,
            Sensor.TYPE_GYROSCOPE to Sensor.STRING_TYPE_GYROSCOPE,
        )
    }

}