package com.elte.sensor

import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.google.android.gms.wearable.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * A service that listens for data from the wearable device and uploads it to Edge Impulse and saves it to a file.
 * @version 1.0 2024-04-13
 * @author Wittawin Panta
 */
class MobileListenerService : WearableListenerService() {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://studio.edgeimpulse.com/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val edgeImpulseAPI = retrofit.create(EdgeImpulseAPI::class.java)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onChannelOpened(channel: ChannelClient.Channel) {
        Log.i(TAG, "Channel opened: " + channel.path)
        Log.i(TAG, "Receiving data from wearable...")
        val fileUri = this.createFile()
        Wearable.getChannelClient(application).receiveFile(channel, fileUri, false)
        uploadFileToEdgeImpulse(File(fileUri.path!!))
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

//
//import android.net.Uri
//import android.os.Build
//import android.os.Environment
//import android.util.Log
//import androidx.annotation.RequiresApi
//import androidx.core.net.toUri
//import com.google.android.gms.wearable.*
//import java.io.File
//import java.time.LocalDateTime
//import java.time.format.DateTimeFormatter
//
//class MobileListenerService : WearableListenerService() {
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun onChannelOpened(channel: ChannelClient.Channel) {
//        Log.i(TAG, "Channel opened: " + channel.path)
//        Log.i(TAG, "Receiving data from wearable...")
//        Wearable.getChannelClient(application).receiveFile(channel, this.createFile(), false)
//    }
//
//    override fun onInputClosed(channel: ChannelClient.Channel, p1: Int, p2: Int) {
//        Wearable.getChannelClient(application).close(channel)
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun createFile(): Uri {
//        val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//        val dateTime = LocalDateTime.now()
//        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd_HH.mm")
//        val formatted = dateTime.format(formatter)
//        return File("$downloadsDirectory/sensor_data_$formatted.csv").toUri()
//    }
//
//    companion object {
//        private const val TAG = "MobileListenerService"
//    }
//}
//class MobileListenerService : WearableListenerService(){
//    private var currentFile: Uri? = null
//    private var handler = Handler(Looper.getMainLooper())
//
//    override fun onChannelOpened(channel: ChannelClient.Channel) {
//        Log.i(TAG, "Channel opened: ${channel.path}")
//        Log.i(TAG, "Receiving data from wearable...")
//        val runnable = object : Runnable {
//            @RequiresApi(64)
//            override fun run() {
//                currentFile = createFile()
//                Wearable.getChannelClient(application).receiveFile(channel, currentFile!!, false)
//                handler.postDelayed(this, 3000)
//            }
//        }
//        handler.postDelayed(runnable, 3000)
//    }
//
//    override fun onInputClosed(channel: ChannelClient.Channel, p1: Int, p2: Int) {
//        handler.removeCallbacksAndMessages(null)
//        Wearable.getChannelClient(application).close(channel)
//    }
//
//    private fun createFile(): Uri{
//        val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//        val dateTime = LocalDateTime.now()
//        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd_HH.mm.ss")
//        val formatted = dateTime.format(formatter)
//        val fileName = "sensor_data_$formatted.csv"
//        val file = File(downloadsDirectory, fileName)
//        if (!file.exists()) {
//            file.createNewFile()  // Ensure the file exists
//        }
//        return file.toUri()
//    }
//    companion object{
//        private const val TAG = "MobileListenerService"
//    }
//}
//
//class MobileListenerService : WearableListenerService() {
//
//    //private val sensorDataBuffer = mutableListOf<String>()
//    private val executorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
//    private var currentFile: Uri? = null
//    private var startTime = System.currentTimeMillis()
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun onChannelOpened(channel: ChannelClient.Channel) {
//        Log.i(TAG, "Channel opened: ${channel.path}")
//        Log.i(TAG, "Receiving data from wearable...")
//
//        // Initialize the timing mechanism and current file when the channel is opened.
//        startTime = System.currentTimeMillis()
//        currentFile = createFile()
//
//        executorService.scheduleAtFixedRate({
//            currentFile = createFile()
//            Wearable.getChannelClient(application).receiveFile(channel, currentFile!!, false)
//        }, 3000, 3000, TimeUnit.MILLISECONDS)
//    }
//
//    override fun onInputClosed(channel: ChannelClient.Channel, p1: Int, p2: Int) {
//        executorService.shutdownNow()
//        Wearable.getChannelClient(application).close(channel)
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun createFile(): Uri {
////        val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
////        val currentTime = System.currentTimeMillis()
////        val timeSegment = (currentTime - startTime) / 3000 // Determine the segment based on the elapsed time
////        val fileName = "sensor_data_segment_${timeSegment}.csv"
////        return File(downloadsDirectory, fileName).also { file ->
////            if (!file.exists()) {
////                file.createNewFile()
////            }
////        }
//        val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//        val currentTime = LocalDateTime.now()
//        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd_HH.mm.ss")
//        val formatted = currentTime.format(formatter)
//        val fileName = "sensor_data_segment_${formatted}.csv"
//        return File("${downloadsDirectory}/${fileName}").toUri()
//    }
//
//    // Call this method to add data to the buffer and flush to the file
////    private fun addSensorData(data: String) {
////        sensorDataBuffer.add(data)
////
////        // Writes the current buffer to the file and clears it
////        currentFile?.let { file ->
////            FileOutputStream(file, true).use { fos ->
////                PrintWriter(fos).use { writer ->
////                    sensorDataBuffer.forEach { sensorData ->
////                        writer.println(sensorData)
////                    }
////                }
////            }
////        }
////        sensorDataBuffer.clear() // Clear the buffer after writing
////    }
//
//    companion object {
//        private const val TAG = "MobileListenerService"
//    }
//}
//
