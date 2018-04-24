package com.example.jerry.HealthTracker;

import android.Manifest;
import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WearIntentService extends Service {

    private static final long CONNECTION_TIME_OUT_MS = 100;

    private GoogleApiClient client;
    private String nodeId;
    private SensorManager mSensorManager;
    private Sensor mHeartRateSensor;
    private SensorEventListener heartListener;
    private boolean stop = false;
    private Thread heartRateThread;
    public WearIntentService(){
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        heartListener = new WearIntentService.heartListener();
        Runnable r = new Runnable() {
            long futureTime = System.currentTimeMillis() + 5000;
            @Override
            public void run(){
                while(!stop){

                    if(System.currentTimeMillis() >= futureTime) {
                        futureTime = System.currentTimeMillis() + 5000;
                        mSensorManager.registerListener(heartListener, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
                    }
                }
            }
        };
        heartRateThread = new Thread(r);
        heartRateThread.start();
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }
    private class heartListener implements SensorEventListener {
        public void onSensorChanged(SensorEvent event) {

            float mHeartRateFloat = event.values[0];

            int mHeartRate = Math.round(mHeartRateFloat);

            //mTextView.setText(Integer.toString(mHeartRate));
            sendData(mHeartRate);
            if (mHeartRate != 0){
                mSensorManager.unregisterListener(heartListener);
            }
        // unregister orientation listener

        }
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    /**
     * Sends a message to the connected mobile device, telling it to show a Toast.
     */
    private void sendData(int heartRate) {
        Wearable.getMessageClient(this).sendMessage("message", Integer.toString(heartRate), new byte[2]);
        //Task<DataItem> dataItemTask = Wearable.getDataClient(this).putDataItem(request);
    }

    //onPause() unregister the accelerometer for stop listening the events
    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(heartListener);    // unregister orientation listener

        stop = true;
        heartRateThread.interrupt();
        super.onDestroy();
    }
}
