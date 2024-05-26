/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.elte.sensor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.elte.sensor.theme.SensorTheme

/**
 * Main activity of the Wear app.
 * @version 1.0 2024-04-13
 * @author Wittawin Panta
 */
class MainActivity : ComponentActivity() {
    private var accelerometerValues by mutableStateOf(floatArrayOf(0f, 0f, 0f))
    private var gyroscopeValues by mutableStateOf(floatArrayOf(0f, 0f, 0f))
    /**
     * Called when the activity is starting.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.checkPermission()
        setContent {
            WearApp(accelerometerValues, gyroscopeValues)
        }
    }

    /**
     * Checks if the app has the necessary permissions.
     */
    private fun checkPermission() {
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting necessary permissions...")
            requestPermissions(arrayOf(Manifest.permission.BODY_SENSORS), 1)
        } else {
            Log.d(TAG, "Permissions already granted.")
        }
    }

    companion object {
        val instance: MainActivity = MainActivity()
        private const val TAG = "MainActivity"
    }
}

/**
 * The main UI entry point for the Wear app.
 * @param accelerometerValues the accelerometer values to display
 * @param gyroscopeValues the gyroscope values to display
 */
@Composable
fun WearApp(accelerometerValues: FloatArray, gyroscopeValues: FloatArray) {
    SensorTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = 15.sp,
                text = "Android Motion Collector"
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                color = MaterialTheme.colors.primary,
                text = "Real-time movement prediction"
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = 10.sp,
                text = "Sensor data collection"
            )
        }
    }
}

/**
 * Preview the Wear app.
 */
@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(accelerometerValues = floatArrayOf(1.0f, 2.0f, 3.0f),
        gyroscopeValues = floatArrayOf(4.0f, 5.0f, 6.0f))
}