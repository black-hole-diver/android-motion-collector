package com.elte.sensor

import android.content.Context
import android.content.Intent
import android.util.Log
import com.elte.sensor.common.Constants
import com.elte.sensor.common.Constants.INTENT_PHONE_NODE_ID
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * The wear listener service.
 * @version 1.0 2024-04-13
 * @author Wittawin Panta
 */
class WearListenerService : WearableListenerService() {

    /**
     * Called when the service is starting.
     */
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Starting wearable message listener...")
    }

    /**
     * Called when a message is received from the connected phone.
     * @messageEvent The message event.
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.i(TAG, "Message received: ${messageEvent.path} from ${messageEvent.sourceNodeId}")
        when (messageEvent.path) {
            Constants.MESSAGE_PATH_RECORDING_STARTED -> {
                val intent = Intent(this, SensorService::class.java)
                intent.putExtra(INTENT_PHONE_NODE_ID, messageEvent.sourceNodeId)
                startService(intent)
            }
            Constants.MESSAGE_PATH_RECORDING_STOPPED -> {
                stopService(Intent(this, SensorService::class.java))
            }
            else -> {
                Log.w(TAG,"Unrecognized message path: ${messageEvent.path}")
            }
        }
    }
    companion object {
        val instance = WearListenerService()
        private const val TAG = "ListenerService"
    }
}
