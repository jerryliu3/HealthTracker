package com.example.jerry.healthmonitor2;

import android.content.Context;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.util.Config.LOGD;

public class MainActivity extends AppCompatActivity implements MessageClient.OnMessageReceivedListener {
    private static final long CONNECTION_TIME_OUT_MS = 100;
    private static final String MESSAGE = "Hello Wear!";

    private GoogleApiClient client;
    private String nodeId;
    private static final String TAG = "MainActivity";
    TextView main;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListenerService ls = new ListenerService();
        ls.onCreate();
        main = (TextView) findViewById(R.id.helloworld);
        WearableListenerService wls = new WearableListenerService(){
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {
                showToast(messageEvent.getPath());
            }

            private void showToast(String message) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                main.setText(message);
            }
        };
        wls.onCreate();

        initApi();

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
        super.onResume();
        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getMessageClient(this).removeListener(this);
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        /*LOGD(TAG, "onMessageReceived() A message from watch was received:"
                + messageEvent.getRequestId() + " " + messageEvent.getPath());*/
        main.setText(messageEvent.getPath());
    }
}
