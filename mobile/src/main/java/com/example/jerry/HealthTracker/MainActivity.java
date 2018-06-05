package com.example.jerry.HealthTracker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

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

public class MainActivity extends AppCompatActivity implements MessageClient.OnMessageReceivedListener, DataClient.OnDataChangedListener{
    private static final long CONNECTION_TIME_OUT_MS = 100;

    private GoogleApiClient client;
    private String nodeId;
    private static final String TAG = "MainActivity";
    private ArrayList<String> readings;
    TextView main;
    Button exit;
    Button learn;
    Switch toggleIntent;
    boolean intentOn = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        main = (TextView) findViewById(R.id.helloworld);
        readings = new ArrayList<String>();
        exit = (Button) findViewById(R.id.exit);
        learn = (Button) findViewById(R.id.learn);
        toggleIntent = (Switch) findViewById(R.id.toggleIntent);
        toggleIntent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                if(isChecked)
                {
                    intentOn = true;
                }
                else
                {
                    intentOn = false;
                }
            }
        });
        initApi();
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }

    // Check if external storage is available to read and write
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state));
    }

    public void saveData(View view) {
        Log.i("jerry.HealthTracker", "Saving data");
        try {
            String externalfilename = "HeartRateData.txt";
            if (isExternalStorageWritable()) {
            } else {
                main.setText("not writable");
            }
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

    public void resetData(View view) {
        Log.i("jerry.HealthTracker", "Saving data");
        try {
            String externalfilename = "HeartRateData.txt";
            if (isExternalStorageWritable()) {
            } else {
                main.setText("not writable");
            }
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
            FileWriter writer = new FileWriter(textFile, false);
            writer.flush();
            writer.close();
            readings = new ArrayList<String>();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void learn(View view) {
        Intent intent = new Intent(this, MachineLearning.class);
        startActivity(intent);
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

    @Override
    protected void onResume() {
        Intent intent = new Intent(this, HeartRateIntentService.class);
        stopService(intent);
        Log.i("jerry.HealthTracker", "Stopping service");
        super.onResume();
        Wearable.getMessageClient(this).addListener(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("jerry.HealthTracker", "On pause");
        Wearable.getMessageClient(this).removeListener(this);
        //saveData(findViewById(android.R.id.content));
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        /*LOGD(TAG, "onMessageReceived() A message from watch was received:"
                + messageEvent.getRequestId() + " " + messageEvent.getPath());*/
        main.setText(messageEvent.getPath());
        readings.add(messageEvent.getPath());
    }

    @Override
    public void onDataChanged(final DataEventBuffer dataEventBuffer) {
        /*LOGD(TAG, "onMessageReceived() A message from watch was received:"
                + messageEvent.getRequestId() + " " + messageEvent.getPath());*/

        Iterator<DataEvent> iterator = dataEventBuffer.iterator();
        List<DataEvent> list = new ArrayList<DataEvent>();
        while(iterator.hasNext())
        {
            list.add(iterator.next());
        }
        main.setText(list.get(0).getDataItem().toString());
    }
    @Override
    protected void onUserLeaveHint(){
        onPause();
        Log.i("jerry.HealthTracker", "On user leave hint");
        if(intentOn) {
            Intent intent = new Intent(this, HeartRateIntentService.class);
            startService(intent);
        }
        super.onUserLeaveHint();
    }
    public void exit(View view){
        Wearable.getMessageClient(this).removeListener(this);
        Intent intent = new Intent(this, HeartRateIntentService.class);
        stopService(intent);
        finish();
    }
}
