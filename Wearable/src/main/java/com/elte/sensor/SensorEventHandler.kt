package com.elte.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.core.net.toUri
import com.elte.sensor.ml.Trained
import org.tensorflow.lite.DataType
import kotlin.math.roundToInt
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Handles sensor events, logs them, and creates a CSV file with the sensor data.
 * Implements Android's SensorEventListener interface to receive updates from sensors.
 * @version 1.0 2024-04-13
 * @author Wittawin Panta
 */
class SensorEventHandler(context: Context) : SensorEventListener {
    private val readings: MutableList<Map<String, String>> = ArrayList()
    private var model: Trained = Trained.newInstance(context)
    val byteBuffer = ByteBuffer.allocateDirect(222 * 4).order(ByteOrder.nativeOrder())
    private var bufferIndex = 0
    private val totalReadingsNeeded = 222
    private var bufferCapacity = 222 * 4

    /**
     * Called when there is a new sensor event.
     * @param event The sensor event.
     */
    override fun onSensorChanged(event: SensorEvent) {
        updateEventUI(event)
        var unit = 1000000
        var interval = 20.0
        var rawTimestamp = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / unit
        var normalizedTimestamp = Math.floor(rawTimestamp / interval)  // This line rounds the timestamp down to the nearest 10 ms
        readings.add(mapOf<String,String>(
            "timestamp" to normalizedTimestamp.toString(),
            "name"      to event.sensor.name,
            "values"    to event.values.joinToString(" # "),
            "accuracy"  to event.accuracy.toString(),
            "coords"    to event.values.size.toString(),
            "type"      to event.sensor.stringType
        ))

        Log.d(
            TAG,
            "event from ${event.sensor.stringType} received (${event.values.joinToString(",")})"
        )

        if (bufferIndex + event.values.size > 222) {
            byteBuffer.flip()  // Prepare the buffer for reading
            runInference()
            byteBuffer.clear()  // Clear the buffer after processing
            bufferIndex = 0     // Reset the index for new data
        }

        event.values.forEach { value ->
            if (bufferIndex < 222) {
                byteBuffer.putFloat(value)
                bufferIndex++
            }
        }
        // Send the sensor data to the phone
        // sendSensorDataToPhone(event, normalizedTimestamp)
    }

    private fun runInference() {
        Log.d(MODEL_TAG, "Running inference on the model.")
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, totalReadingsNeeded), DataType.FLOAT32)
        inputFeature0.loadBuffer(byteBuffer)
        Log.d(MODEL_TAG, "Model input: ${inputFeature0.floatArray.joinToString(",")}")

        val outputs = model.process(inputFeature0)
        handleModelOutput(outputs.outputFeature0AsTensorBuffer)
    }

    private fun handleModelOutput(outputFeature0: TensorBuffer) {
        val probabilities = outputFeature0.floatArray
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
        val classNames = arrayOf("Clapping", "Drawing", "Drinking", "Glass_grabbing", "Goliath", "Hopscotch", "Knee_other", "Puding_eat")

        if (maxIndex != -1) {
            Log.d(MODEL_TAG, "Predicted Activity: ${classNames[maxIndex]} with Confidence: ${probabilities[maxIndex]}")
        }
    }

    private fun updateEventUI(event: SensorEvent) {
        when (event.sensor.stringType) {
            "android.sensor.accelerometer" -> {
                Log.d(TAG, "Accelerometer values updated: ${event.values.joinToString(",")}")
                MainActivity.instance.updateAccelerometerValues(event.values)
            }
            "android.sensor.gyroscope" -> {
                Log.d(TAG, "Gyroscope values updated: ${event.values.joinToString(",")}")
                MainActivity.instance.updateGyroscopeValues(event.values)
            }
        }
    }

//    private fun sendSensorDataToPhone(event: SensorEvent, normalizedTimestamp: Double) {
//        val dataMap = DataMap().apply {
//            putString("timestamp", normalizedTimestamp.toString())
//            putString("name", event.sensor.name)
//            putString("values", event.values.joinToString(" # "))
//            putString("accuracy", event.accuracy.toString())
//            putString("coords", event.values.size.toString())
//            putString("type", event.sensor.stringType)
//        }
//        val putDataMapReq = PutDataMapRequest.create("/sensor_data").apply {
//            dataMap.putAll(dataMap)
//        }
//        val putDataReq = putDataMapReq.asPutDataRequest()
//        Wearable.getDataClient(mainInstance).putDataItem(putDataReq)
//    }

    /**
     * Called when a sensor's accuracy is changed.
     * @param sensor The sensor whose accuracy has changed.
     * @param accuracy The new accuracy of this sensor.
     */
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.d(TAG, "Accuracy changed for: $sensor - $accuracy")
    }

    /**
     * Clears all recorded sensor readings.
     */
    fun clearReadings() {
        Log.d(TAG, "Clearing recordings array.")
        readings.clear()
    }

    /**
     * Rounds a timestamp to two decimal places.
     * @param timestamp The timestamp to round.
     * @return The rounded timestamp.
     */
    fun roundTimestamp(timestamp: Double): Double = (timestamp * 100).roundToInt() / 100.0

    /**
     * Creates a URI to a CSV file containing all recorded sensor readings.
     * @param context The application context.
     * @return Uri pointing to the generated CSV file.
    */
    fun getFileURI(context: Context): Uri {
        Log.i(TAG, "Writing ${readings.size} records to file.")
        val temp = File.createTempFile("recording", "csv", context.cacheDir)
        val names = arrayOf(
            "uacc_x", "uacc_y", "uacc_z",
            "grav_y", "grav_x", "grav_z",
            "gyr_x", "gyr_y", "gyr_z"
        )

        var output = "Timestamp,${names.joinToString(",")}\n"

        val filteredStamps: MutableMap<
                String,                     // timestamp
                MutableMap<String,String>   // sensor -> <values>
        > = mutableMapOf()

        for (item in readings) {
            if (filteredStamps.containsKey(item["timestamp"])) {
                val obj = filteredStamps[item["timestamp"]]!!
                obj[item["type"]!!] = item["values"]!!.replace(" # ", ",")
            }
            else {
                val obj: MutableMap<String,String> = mutableMapOf(
                    "android.sensor.accelerometer"          to "NaN,NaN,NaN",
                    "android.sensor.gravity"                to "NaN,NaN,NaN",
                    "android.sensor.gyroscope"              to "NaN,NaN,NaN",
                )
                obj[item["type"]!!] = item["values"]!!.replace(" # ", ",")
                filteredStamps[item["timestamp"]!!] = obj
            }
        }

        for ((key, value) in filteredStamps){
            val accData = value["android.sensor.accelerometer"] ?: "NaN,NaN,NaN"
            val gravityData = value["android.sensor.gravity"] ?: "NaN,NaN,NaN"
            val gyroData = value["android.sensor.gyroscope"] ?: "NaN,NaN,NaN"

            if (accData != "NaN,NaN,NaN") {
                val scaledAccValues = accData.split(",").joinToString(",") { v ->
                    try { (v.toFloat() * -0.1).toString() } catch (e: NumberFormatException) { "NaN" }
                }
                output += "$key,$scaledAccValues,$gravityData,$gyroData\n"
            } else {
                output += "$key,$accData,$gravityData,$gyroData\n"
            }
        }
        temp.writeText(output)
        return temp.toUri()
    }

    companion object {
        //val instance = SensorEventHandler(con)
        private const val TAG = "SensorEventHandler"
        private const val MODEL_TAG = "TF Lite"
    }
}