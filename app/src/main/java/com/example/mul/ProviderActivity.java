package com.example.mul;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import android.net.TrafficStats;

import com.google.android.gms.common.util.Strings;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ProviderActivity extends AppCompatActivity {
    //Get Access to common methods
    Client_Provider_Common common = new Client_Provider_Common();

    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    //UI Elements
    private EditText ET_SSID, ET_Password;

    // Name of the connected device
    private String mConnectedDeviceName = null;

    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    // Member object for the chat services
    private BTCommunicationService BTService = null;

    private String TAG = ProviderActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider);

        TextView limitView = findViewById(R.id.limit);
        TextView providedView = findViewById(R.id.dataProvided);

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
                            int centsBalance = 0;
                            int dataUsed = 0;
                            int dataProvided = 0;
                            int limit = 0;
                            if (!id.equals("")) {
                                // user actually exists
                                centsBalance = obj.getInt("cents_balance");
                                dataUsed = obj.getInt("data_used");
                                dataProvided = obj.getInt("data_provided");
                                limit = obj.getInt("limit");
                            }

                            final int finalLimit = limit;
                            final int finalProvided = dataProvided;

                            ProviderActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    providedView.setText(String.format("%d", finalProvided));
                                    limitView.setText(String.format("%d", finalLimit));
                                }
                            });


//                    balanceView.setText(String.format("%d", centsBalance));
//                    dataUsedView.setText(String.format("%d", dataUsed));

                        } catch (JSONException e) {
                            Log.d(TAG, "failed parsing JSON from API");
                            return;
                        }
                    }

                    ;
                });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        mBluetoothAdapter.setName("MulTooth79797");

        common.ensureDiscoverable(mBluetoothAdapter, getApplicationContext());

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (BTService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (BTService != null) {
            if (BTService.getState() == BTCommunicationService.STATE_NONE) {
                BTService.start();
            }
        }
    }

    private void setupChat() {
        // Initialize the BluetoothChatService to perform bluetooth connections
        BTService = new BTCommunicationService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (BTService != null) BTService.stop();
    }

    // The Handler that gets information back from the BluetoothChatService

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            Log.i(TAG, "Bluetooth mHandler");
            switch (msg.what) {
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();

                    //When a connection is made, create the concatenated string containing hotspot credentials
                    //String wifiString = "Mul-123" + "." + "mulrocks" + " " + MainActivity.IMEINumber;
                    //String wifiString = "Mul-123" + "." + "mulrocks" + " " + MainActivity.IMEINumber;


                    String wifiString = readSetting("ssid") + "." + readSetting("password") + " " + MainActivity.IMEINumber;

                    //Then send that to the client so the client can connect
                    Log.d(TAG, "WIFI STRING: " + wifiString);
                    common.sendMessage1(wifiString, BTService, mOutStringBuffer, getApplicationContext());

                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult");
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(EXTRA_DEVICE_ADDRESS);

                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

                    // Attempt to connect to the device
                    BTService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    public void onClickProvide(View view) {
        Log.i(TAG, "clicked provide");
        if (!isHotspotEnabled()) {
            // actually starting the hotspot
            Intent intent = new Intent(getString(R.string.intent_action_turnon));
            sendImplicitBroadcast(this,intent);
        } else {
            Toast.makeText(this, "! Looks like hotspot is already on !", Toast.LENGTH_SHORT).show();
        }

        Intent i = new Intent(getApplicationContext(), Active_Provider.class);
        startActivity(i);
    }

    public void onClickUpdate(View view) {
        Intent i = new Intent(getApplicationContext(), Session_Limits.class);
        startActivity(i);
    }

//    public void connect(View v) {
//        Log.i(TAG, "connecting bluetooth");
//
//        if (BTService.getState() == BTCommunicationService.STATE_CONNECTED) {
//            Toast.makeText(this, "Already Connected", Toast.LENGTH_LONG);
//        } else {
//            Intent serverIntent = new Intent(this, DiscoverDevices.class);
//            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
//        }
//    }

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

    public static void sendImplicitBroadcast(Context ctxt, Intent i) {
        PackageManager pm=ctxt.getPackageManager();
        List<ResolveInfo> matches=pm.queryBroadcastReceivers(i, 0);

        for (ResolveInfo resolveInfo : matches) {
            Intent explicit=new Intent(i);
            ComponentName cn=
                    new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName,
                            resolveInfo.activityInfo.name);

            explicit.setComponent(cn);
            ctxt.sendBroadcast(explicit);
        }
    }

    public String readSetting(String key) {
        SharedPreferences settings = getApplicationContext().getSharedPreferences(SettingsActivity.class.getSimpleName(), 0);
        return settings.getString(key, "empty");
    }
}