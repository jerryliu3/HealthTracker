package com.example.jerry.HealthTracker;
import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HeartRateIntentService extends Service implements MessageClient.OnMessageReceivedListener{
    private static final long CONNECTION_TIME_OUT_MS = 100;

    private static final String TAG = "jerry.HealthTracker";
    private ArrayList<String> readings;
    private boolean stop = false;
    private Thread heartRateThread;
    public HeartRateIntentService(){
        readings = new ArrayList<String>();

    }

    public void saveData() {
        try {
            String externalfilename = "HeartRateData.txt";
            File file = Environment.getExternalStorageDirectory();
            File newFile = new File(file, "Wearable App");
            if (!newFile.exists()) {
                newFile.mkdirs();
            }
         /*int variable = 0;
         if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
         {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, variable);
         }*/
            File textFile = new File(newFile, externalfilename);
            FileWriter writer = new FileWriter(textFile, true);
            for(int x=0;x<readings.size();x++) {
                writer.append(readings.get(x) + "\n");
            }
            writer.flush();
            writer.close();
            readings = new ArrayList<String>();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("jerry.HealthTracker", "Service started");
        Wearable.getMessageClient(this).addListener(this);

        Runnable r = new Runnable() {
            @Override
            public void run(){
                while(!stop){
                    Log.i("jerry.HealthTracker", "Service going");
                }
            }
        };
        heartRateThread = new Thread(r);
        heartRateThread.start();
        return Service.START_NOT_STICKY;
    }



    @Override
    public void onDestroy(){
        Log.i("jerry.HealthTracker", "Service called on destroy");
        Wearable.getMessageClient(this).removeListener(this);
        stop = true;
        heartRateThread.interrupt();
        saveData();
        super.onDestroy();
    }
    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        /*LOGD(TAG, "onMessageReceived() A message from watch was received:"
                + messageEvent.getRequestId() + " " + messageEvent.getPath());*/
        Log.i(TAG, "Message received");

        readings.add(messageEvent.getPath());
    }

}
