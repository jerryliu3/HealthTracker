package com.example.jerry.healthmonitor2;

import android.Manifest;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends WearableActivity implements View.OnClickListener{

    private static final long CONNECTION_TIME_OUT_MS = 100;

    private GoogleApiClient client;
    private String nodeId;
    private SensorManager mSensorManager;
    private Sensor mHeartRateSensor;
    private SensorEventListener heartListener;
    private TextView heartRateText;

    private Button send;
    private Button stop;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.round_activity_main);

        initApi();
        setAmbientEnabled();

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BODY_SENSORS}, 1);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        heartListener = new heartListener();
        heartRateText = (TextView) findViewById(R.id.heartRateText);

        send = (Button) findViewById(R.id.btn_send);
        send.setOnClickListener(this);
        stop = (Button) findViewById(R.id.btn_stop);
        stop.setOnClickListener(this);
        onPause();
    }


    //onResume() register the accelerometer for listening the events
    @Override
    protected void onResume() {
        super.onResume();
        //sensorManager.registerListener(oriL, oriSensor, SensorManager.SENSOR_DELAY_NORMAL);
        //sensorManager.registerListener(accL, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(heartListener, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        //sensorManager.registerListener(pedoL, pedoSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    //onPause() unregister the accelerometer for stop listening the events
    protected void onPause() {
        heartRateText.setText("--");
        //sensorManager.unregisterListener(oriL);    // unregister acceleration listener
        //sensorManager.unregisterListener(accL);    // unregister orientation listener
        mSensorManager.unregisterListener(heartListener);    // unregister orientation listener
        //sensorManager.unregisterListener(pedoL);
        super.onPause();

    }

    private class pedoListener implements SensorEventListener {
        public void onSensorChanged(SensorEvent event) {

            //Log.i("SENSOR", "Acceleration changed.");
            //Log.i("SENSOR", "  Acceleration X: " + event.values[0]
            //        + ", Acceleration Y: " + event.values[1]
            //        + ", Acceleration Z: " + event.values[2]);
            String val = new String();
            val = "pedo: " + String.valueOf(event.values[0]).substring(0,3);
            Log.i("SENSOR", "Acceleration changed. : " + event.values[0]);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    private class accListener implements SensorEventListener {
        public void onSensorChanged(SensorEvent event) {

            //Log.i("SENSOR", "Acceleration changed.");
            //Log.i("SENSOR", "  Acceleration X: " + event.values[0]
            //        + ", Acceleration Y: " + event.values[1]
            //        + ", Acceleration Z: " + event.values[2]);
            String val = new String();
            val = "Acc X: " + String.valueOf(event.values[0]).substring(0,3)
                    + "/Y: " + String.valueOf(event.values[1]).substring(0,3)
                    + "/Z: " + String.valueOf(event.values[2]).substring(0,3);
            //acc.setText(val);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    private class oriListener implements SensorEventListener {
        public void onSensorChanged(SensorEvent event) {

            //Log.i("SENSOR", "Orientation changed.");
            //Log.i("SENSOR", "  Orientation X: " + event.values[0]
            //        + ", Orientation Y: " + event.values[1]
            //        + ", Orientation Z: " + event.values[2]);
            String val = new String();
            val = "ori X: " + String.valueOf(event.values[0]).substring(0,3)
                    + "/Y: " + String.valueOf(event.values[1]).substring(0,3)
                    + "/Z: " + String.valueOf(event.values[2]).substring(0,3);
            //ori.setText(val);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    private class heartListener implements SensorEventListener {
        public void onSensorChanged(SensorEvent event) {

            float mHeartRateFloat = event.values[0];

            int mHeartRate = Math.round(mHeartRateFloat);

            //mTextView.setText(Integer.toString(mHeartRate));
            MainActivity.this.heartRateText.setText(Integer.toString(mHeartRate));
            sendData(mHeartRate);
        }
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    /**
     * Initializes the GoogleApiClient and gets the Node ID of the connected device.
     */
    private void initApi() {
        client = getGoogleApiClient(this);
        retrieveDeviceNode();
    }

    /**
     * Returns a GoogleApiClient that can access the Wear API.
     * @param context
     * @return A GoogleApiClient that can make calls to the Wear API
     */
    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
    }

    /**
     * Connects to the GoogleApiClient and retrieves the connected device's Node ID. If there are
     * multiple connected devices, the first Node ID is returned.
     */
    private void retrieveDeviceNode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(client).await();
                List<Node> nodes = result.getNodes();
                if (nodes.size() > 0) {
                    nodeId = nodes.get(0).getId();
                }
                client.disconnect();
            }
        }).start();
    }

    /**
     * Sends a message to the connected mobile device, telling it to show a Toast.
     */
    private void sendData(int heartRate) {
        Wearable.getMessageClient(this).sendMessage("message", Integer.toString(heartRate), new byte[2]);
        //Task<DataItem> dataItemTask = Wearable.getDataClient(this).putDataItem(request);
    }

    public void onClick(View v){
        if(v == send) {
            onResume();
            send.setVisibility(View.INVISIBLE);
            stop.setVisibility(View.VISIBLE);
        }
        else if(v == stop)
        {
            onPause();
            send.setVisibility(View.VISIBLE);
            stop.setVisibility(View.INVISIBLE);
        }
    }

}
