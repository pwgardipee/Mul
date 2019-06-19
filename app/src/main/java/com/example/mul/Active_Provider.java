package com.example.mul;

import android.content.Context;
import android.content.Intent;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
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
import java.lang.reflect.Method;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class Active_Provider extends AppCompatActivity {
    private String TAG = ProviderActivity.class.getSimpleName();
    private Runnable updater;
    private long sessionStartRxBytes = 0;
    private long sessionStartTxBytes = 0;
    public final Handler timerHandler = new Handler();

    private long deltaTX_prev, deltaRX_prev;
    private boolean start_detecting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.active_provider);
        MainActivity.providing = true;

        TextView limitView = findViewById(R.id.limit);
        TextView providedView = findViewById(R.id.dataProvided);
        TextView tv = findViewById(R.id.stats);

        updater = new Runnable() {
            @Override
            public void run() {
                // check api for user info
                MulAPI.get_user(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.d(TAG, "interneting failed somehow");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String jsonStr = response.body().string();
                        try {
                            JSONObject obj = new JSONObject(jsonStr);
                            String id = obj.getString("id");
                            int dataProvided = 0;
                            int limit = 0;
                            if (!id.equals("")) {
                                // user actually exists
                                dataProvided = obj.getInt("data_provided");
                                limit = obj.getInt("limit");
                            }

                            final int finalLimit = limit;
                            final int finalProvided = dataProvided;

                            Active_Provider.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tv.setText(String.format("Data Provided: %s", formatDataUsed(finalProvided)));
                                }
                            });


//                    balanceView.setText(String.format("%d", centsBalance));
//                    dataUsedView.setText(String.format("%d", dataUsed));

                        } catch (JSONException e) {
                            Log.d(TAG, "failed parsing JSON from API");
                            return;
                        }
                    };
                });

                timerHandler.postDelayed(this, 3000);
            }
        };
//
//        // this actually starts the updater
        timerHandler.post(updater);
    }

    public void onClickStop(View view) {
        Log.i(TAG, "attempt to stop hotspot");
        if (isHotspotEnabled()) {
            Intent intent = new Intent(getString(R.string.intent_action_turnoff));
            ProviderActivity.sendImplicitBroadcast(this,intent);

            // shut off data update
            // TODO: try catch
            timerHandler.removeCallbacks(updater);
        } else {
            Toast.makeText(this, "! Looks like hotspot is not on !", Toast.LENGTH_SHORT).show();
        }

        Intent i = new Intent(getApplicationContext(), ProviderActivity.class);
        MainActivity.providing = false;
        startActivity(i);
    }

    public void onClickUpdate(View view) {
        Intent i = new Intent(getApplicationContext(), Session_Limits.class);
        startActivity(i);
    }

    public boolean isHotspotEnabled() {
        // assumes false if any errors occur, is that the more dangerous option?
        boolean isEnabled = false;
        // Check if hotspot on with reflection
        // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/pie-release-2/wifi/java/android/net/wifi/WifiManager.java#2133
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals("isWifiApEnabled")) {
                try {
                    isEnabled = (boolean) method.invoke(wifiManager);
                } catch (Exception e) {
                    Log.e(TAG, "Failed checking AP status: " + e.toString());
                    e.printStackTrace();
                    isEnabled = false;
                }
            }
        }
        //Error isWifiApEnabled not found
        return isEnabled;
    }

    private String formatDataUsed(long dataUsed) {
        if (dataUsed > (1024*1024)) {
            return String.format("%d.%d MB", dataUsed / (1024*1024), dataUsed % (1024*1024));
        } else if (dataUsed > 1024) {
            return String.format("%d KB", dataUsed / 1024);
        } else {
            return String.format("%d B", dataUsed);
        }
    }

    private String getFriendlyUsage(long tx, long rx) {
        return String.format("deltaTx: %s deltaRx: %s", formatDataUsed(tx), formatDataUsed(rx));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        timerHandler.removeCallbacks(updater);
    }


    public void onClickStats(View view) {
        // fill in the textview with current stats info
        TextView tv = (TextView) findViewById(R.id.stats);
        long deltaTx = 0;
        long deltaRx = 0;

        if (isHotspotEnabled()) {
            // no devices are connected, not purposely running any services but still have data exchange on order of hundreds of KB per minute
            long currentTx = TrafficStats.getMobileTxBytes();
            long currentRx = TrafficStats.getMobileRxBytes();
            Log.i(TAG, String.format("currentTx: %d currentRx: %d", currentTx, currentRx));
            Log.i(TAG, String.format("startTx: %d startRx: %d", sessionStartTxBytes, sessionStartRxBytes));
            deltaTx = currentTx - sessionStartTxBytes;
            deltaRx = currentRx - sessionStartRxBytes;

        }
    }
}
