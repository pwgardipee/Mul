package com.example.mul;

import android.content.Intent;
import android.net.TrafficStats;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class Active_Client extends AppCompatActivity {
    // TODO: this is same as Provider, is there anyway to abstract all this crap?
    private long sessionStartRxBytes = 0;
    private long sessionStartTxBytes = 0;
    private final Handler timerHandler = new Handler();
    private Runnable updater;

    private String TAG = ClientActivity.class.getSimpleName();

    private long deltaRX_prev = 0;
    private long threshold = 15*1024;
    private boolean start_detecting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.active_client);
        MainActivity.connected = true;

        final TextView tv = findViewById(R.id.stats);
//        tv.post(new Runnable() {
//            @Override
//            public void run() {
//                tv.setText(getFriendlyUsage(0,0));
//            }
//        });

        updater = new Runnable() {
            @Override
            public void run() {
                TextView tv = findViewById(R.id.stats);
                long currentTx = TrafficStats.getTotalTxBytes();
                long currentRx = TrafficStats.getTotalRxBytes();
                Log.i(TAG, String.format("currentTx: %d currentRx: %d", currentTx, currentRx));
                Log.i(TAG, String.format("startTx: %d startRx: %d", sessionStartTxBytes, sessionStartRxBytes));
                long deltaTx = currentTx - sessionStartTxBytes;
                long deltaRx = currentRx - sessionStartRxBytes;

                deltaRX_prev += deltaRx;

                if(deltaRX_prev > threshold) {
                    deltaRX_prev -= threshold;
                    MulAPI.post_mulchunk(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.d(TAG, "failed trying to internet");
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                            } else {
                                Active_Client.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(Active_Client.this,
                                                "Provider data limit reached, have to disconnect", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                onClickDisconnect(null);
                            }
                        }
                    });
                }

                MulAPI.get_user(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.i(TAG, "Failed to get data usage.");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if(response.isSuccessful()){
                            String jsonStr = response.body().string();
                            try {
                                JSONObject obj = new JSONObject(jsonStr);
                                String id = obj.getString("id");
                                int dataProvided = 0;
                                int limit = 0;
                                if (!id.equals("")) {
                                    // user actually exists
                                    dataProvided = obj.getInt("data_used");
                                }

                                final int finalUsed = dataProvided;

                                Active_Client.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tv.setText(String.format("Data Used: %s", formatDataUsed(finalUsed)));
                                    }
                                });
                            } catch (JSONException e) {
                                Log.d(TAG, "failed parsing JSON from API");
                                return;
                            }
                        }
                        else{
                            Log.i(TAG, "Failed to get response.");
                        }
                    }
                });


                // TODO: change to a longer time but can leave at 1 second while building app
                timerHandler.postDelayed(this, 1000);
            }
        };

        // TODO: here for testing functionality, remove me and uncomment section when bluetooth connection made
        sessionStartRxBytes = TrafficStats.getTotalRxBytes();
        sessionStartTxBytes = TrafficStats.getTotalTxBytes();
        // hold off on starting the timer to get over the data spike
        timerHandler.postDelayed(updater, 5000);
    }


    public void onClickDisconnect(View view) {
        Log.d(TAG, "in handle disconnect");
        timerHandler.removeCallbacks(updater);

        // TODO: get working
        // this can't work because there's no BTService in this activity, we would have to use some sort of singleton pattern to share variables between activities
//        if (BTService != null){
//            BTService.stop();
//        }
        ClientActivity.common.forgetCurrentNetwork(getApplicationContext());
        MainActivity.connected = false;

        finish();
    }

    public void onClickTopUp(View view) {
        Intent i = new Intent(getApplicationContext(), topUp.class);
        startActivity(i);
    }

    // TODO: ahhhh don't hate me these are copied directly from Provider, they need to be abstracted to atone for this shameful behaviour
    private String formatDataUsed(long dataUsed) {
        if (dataUsed > (1024*1024)) {
            return String.format("%d.%d MB", dataUsed / (1024*1024), dataUsed % (1024*1024));
        } else if (dataUsed > 1024) {
            return String.format("%d KB", dataUsed / 1024);
        } else {
            return String.format("%d B", dataUsed);
        }
    }

//    private String getFriendlyUsage(long tx, long rx) {
//        return String.format("deltaTx: %s deltaRx: %s", formatDataUsed(tx), formatDataUsed(rx));
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        timerHandler.removeCallbacks(updater);
        Log.d(TAG, "destroying active client");
    }

}
