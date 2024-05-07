package com.elte.sensor

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import com.elte.sensor.common.Constants
import com.google.android.gms.wearable.Wearable

/**
 * Sensor service.
 * @author Wittawin Panta
 * @version 1.0 2024-04-13
 */
class SensorService : Service() {
    /**
     * Called when the service is starting.
     */
    private val sensorEventHandler = SensorEventHandler.instance

    /**
     * The ID of the phone node.
     */
    private var phoneNodeId: String? = null

    /**
     * Called when the service is starting.
     * @param intent The intent supplied to start the service.
     * @param flags Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     * @return The return value indicates what semantics the system should use for the service's current started state.
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
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
     * Registers the required sensors.
     */
    private fun startRecording() {
        sensorEventHandler.clearReadings()
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