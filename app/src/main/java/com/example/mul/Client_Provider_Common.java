package com.example.mul;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.Random;

import static android.content.ContentValues.TAG;

public class Client_Provider_Common {

    private WifiManager wifiManager;
    private WifiConfiguration conf;
    private Random rand = new Random();

    private String TAG = Client_Provider_Common.class.getSimpleName();

    public void sendMessage1(String message, BTCommunicationService BTService, StringBuffer mOutStringBuffer, Context context) {
        Log.i(TAG, "sendMesssage1: " + message);
        // Check that we're actually connected before trying anything
        if (BTService.getState() != BTCommunicationService.STATE_CONNECTED) {
            Toast.makeText(context, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            BTService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }

    public void ensureDiscoverable(BluetoothAdapter mBluetoothAdapter, Context context) {
        Log.i(TAG, "ensureDiscoverable");

        mBluetoothAdapter.setName(rand.nextInt() + "MulTooth" + rand.nextInt());

        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //Device is always discoverable with value 0
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            //discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            context.startActivity(discoverableIntent);
        }
    }


    public void connectToWifi(String wifiPair, Context context){
        Log.i(TAG, "connectToWifi");
        //The wifiPair will come in the form of <SSID>.<password>

        //Break down the wifiPair string into the individual parts
        int dotIndex = wifiPair.indexOf(".");
        int pairLength = wifiPair.length();
        String networkSSID = wifiPair.substring(0,dotIndex);
        String networkPass = wifiPair.substring(dotIndex + 1,pairLength);

        Log.d(TAG, "SSID: " + networkSSID);
        Log.d(TAG, "Password: " + networkPass);

        //Connect to the wifi network
        conf = new WifiConfiguration();

        conf.SSID = "\"" + networkSSID + "\"";   // Please note the quotes. String should contain ssid in quotes`

        conf.preSharedKey = "\""+ networkPass +"\"";

        //WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if(!wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(true);
            Log.d(TAG, "Trying to turn on WIFI");
        }

        wifiManager.addNetwork(conf);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();
                break;
            }
        }
    }

    public void forgetCurrentNetwork(Context context){
        Log.i(TAG, "forgetCurrentNetwork");
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        WifiInfo info = wifiManager.getConnectionInfo();
        String ssid = ((WifiInfo) info).getSSID();
        wifiManager.setWifiEnabled(false);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            Log.d(TAG, "Trying to forget");
            if (i != null) {
                if(i.SSID.equals(ssid)){
                    wifiManager.removeNetwork(i.networkId);
                }
            }

        }
    }
}
