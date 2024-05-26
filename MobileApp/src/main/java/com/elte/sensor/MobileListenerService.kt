package com.elte.sensor

import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.elte.sensor.common.Constants
import com.google.android.gms.wearable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * A service that listens for data from the wearable device and uploads it to Edge Impulse and saves it to a file.
 * @version 2.0 2024-05-26
 * @author Wittawin Panta
 */
class MobileListenerService : WearableListenerService() {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://studio.edgeimpulse.com/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val edgeImpulseAPI = retrofit.create(EdgeImpulseAPI::class.java)

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Starting mobile message listener...")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onChannelOpened(channel: ChannelClient.Channel) {
        Log.i(TAG, "Channel opened: " + channel.path)
        Log.i(TAG, "Receiving data from wearable...")

        when (channel.path) {
            Constants.CHANNEL_PATH_SENSOR_READING -> {
                Log.i(TAG, "Make a file from sensor data...")
                val fileUri = this.createFile()
                Wearable.getChannelClient(application).receiveFile(channel, fileUri, false)
                uploadFileToEdgeImpulse(File(fileUri.path!!))
            }
            Constants.CHANNEL_PATH_PREDICTION -> {
                Log.i(TAG, "Receiving prediction from wearable...")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val inputStream = Wearable.getChannelClient(application).getInputStream(channel).await()
                        inputStream.use { stream ->
                            val predictionBytes = stream.readBytes()
                            val prediction = predictionBytes.toString(Charsets.UTF_8)
                            withContext(Dispatchers.Main) {
                                Log.i(TAG, "Received prediction: $prediction")
                                FirstFragment.prediction = prediction
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading prediction data: ${e.localizedMessage}")
                    }
                }
            }
            Constants.CHANNEL_SENSOR_NUMBER -> {
                Log.i(TAG, "Receiving sensor number from wearable...")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val inputStream = Wearable.getChannelClient(application).getInputStream(channel).await()
                        inputStream.use { stream ->
                            val sensorNumberBytes = stream.readBytes()
                            val sensorNumber = sensorNumberBytes.toString(Charsets.UTF_8)
                            withContext(Dispatchers.Main) {
                                Log.i(TAG, "Received sensor number: $sensorNumber")
                            }
                            FirstFragment.sensorNumber = sensorNumber.toInt()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading sensor number data: ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    private fun uploadFileToEdgeImpulse(file: File) {
        val apiKey = "c5cd49d07e2be55e38dc382c2563a3e5"
        val call = edgeImpulseAPI.uploadFile(apiKey, file)
        call.enqueue(object : retrofit2.Callback<Void> {
            override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {
                if (response.isSuccessful) {
                    Log.i(TAG, "File uploaded successfully to Edge Impulse")
                } else {
                    Log.e(TAG, "Failed to upload file to Edge Impulse: " + response.message())
                }
            }

            override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                Log.e(TAG, "Error uploading file to Edge Impulse", t)
            }
        })
    }

    override fun onInputClosed(channel: ChannelClient.Channel, p1: Int, p2: Int) {
        Wearable.getChannelClient(application).close(channel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createFile(): Uri {
        val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd_HH.mm.ss")
        val formatted = dateTime.format(formatter)
        return File("$downloadsDirectory/sensor_data_$formatted.csv").toUri()
    }

    companion object {
        private const val TAG = "MobileListenerService"
    }
}
