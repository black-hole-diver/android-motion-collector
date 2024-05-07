package com.elte;

import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.Tasks;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import android.net.Uri;
import android.os.Environment;
import androidx.core.net.UriKt;
import com.google.android.gms.wearable.*;
import com.google.android.gms.wearable.ChannelClient.ChannelCallback;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class SensorWrapper extends CordovaPlugin {

    private final String TAG = "SensorWrapper";
    private final String MESSAGE_PATH_RECORDING_STARTED = "/message_path_recording_started";
    private final String MESSAGE_PATH_RECORDING_STOPPED = "/message_path_recording_stopped";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        Log.i(TAG, "Plugin was called with: " + action);
        switch (action) {
            case "startRecording": {
                this.startRecording(callbackContext);
                return true;
            }
            case "stopRecording": {
                this.stopRecording(callbackContext);
                return true;
            }
            default:
                return false;
        }
    }

    private void startRecording(CallbackContext callbackContext) {
        try {
            sendMessageToConnectedNodes(MESSAGE_PATH_RECORDING_STARTED);
            callbackContext.success("Recording started!");
        } catch (Throwable throwable) {
            Log.e(TAG, throwable.toString());
            callbackContext.error("Unexpected error has accoured!");
        }
    }

    private void stopRecording(CallbackContext callbackContext) {
        try {
            ChannelClient client = Wearable.getChannelClient(cordova.getActivity());
            ChannelCallback listener = this.createWearOsMessageListener();
            client.registerChannelCallback(listener);
            sendMessageToConnectedNodes(MESSAGE_PATH_RECORDING_STOPPED);
            callbackContext.success("Recording is saved!");
        } catch (Throwable throwable) {
            Log.e(TAG, throwable.toString());
            callbackContext.error("Unexpected error has accoured!");
        }
    }

    private void sendMessageToConnectedNodes(String message) {
        final MessageClient messageClient = Wearable.getMessageClient(cordova.getActivity());
        for (Node node : findAllWearDevices()) {
            Log.i(TAG, String.format("Sending message: [%s] to node: %s", message, node.getId()));
            messageClient.sendMessage(node.getId(), message, null);
        }
    }

    private List<Node> findAllWearDevices() {
        Log.i(TAG, "Looking for connected nodes...");
        final NodeClient nodeClient = Wearable.getNodeClient(cordova.getActivity());
        try {
            List<Node> connectedNodes = Tasks.await(nodeClient.getConnectedNodes());
            Log.i(TAG, String.format("Found %d connected device(s).", connectedNodes.size()));
            return connectedNodes;
        } catch (Throwable throwable) {
            Log.e(TAG, throwable.toString());
            return Collections.emptyList();
        }
    }

    private ChannelCallback createWearOsMessageListener() {
        ChannelCallback listener = new ChannelCallback() {

            @Override
            public void onChannelOpened(ChannelClient.Channel channel) {
                Log.i(TAG, "Channel opened: " + channel.getPath());
                File file = createFile();
                Log.i(TAG, "Receiving data from wearable..." + file.toString());
                Wearable.getChannelClient(cordova.getActivity()).receiveFile(channel, UriKt.toUri(file), false);
            }

            @Override
            public void onInputClosed(ChannelClient.Channel channel, int p1, int p2) {
                ChannelClient client = Wearable.getChannelClient(cordova.getActivity());
                client.close(channel);
                client.unregisterChannelCallback(this);
                Log.i(TAG, "Recording was saved!");
            }
        };
        return listener;
    }

    private File createFile() {
        File downloadsDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd_HH.mm");
        String formatted = dateTime.format(formatter);
        return new File(downloadsDirectory.toString() + "/sensor_data_" + formatted + ".txt");
    }

}
