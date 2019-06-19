//package com.example.mul;
//
//import android.Manifest;
//import android.app.Activity;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
//import android.util.Log;
//import android.widget.Toast;
//
//
//public class DiscoverDevices extends AppCompatActivity {
//    private BluetoothAdapter myBlueToothAdapter;
//    public static String EXTRA_DEVICE_ADDRESS = "device_address";
//
//    private String TAG = DiscoverDevices.class.getSimpleName();
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.discover_devices);
//
//        //Initialize bluetooth adapter
//        myBlueToothAdapter = BluetoothAdapter.getDefaultAdapter();
//
//        //Turn on Bluetooth
//        if (myBlueToothAdapter == null)
//            Toast.makeText(DiscoverDevices.this, "Your device doesnt support Bluetooth", Toast.LENGTH_LONG).show();
//        else if (!myBlueToothAdapter.isEnabled()) {
//            Intent BtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(BtIntent, 0);
//            Toast.makeText(DiscoverDevices.this, "Turning on Bluetooth", Toast.LENGTH_LONG).show();
//        }
//
//        // Quick permission check
//        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
//        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
//        if (permissionCheck != 0) {
//            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
//        }
//
//        //Direct all responses of "found" when searching to the "FoundReciever"
//        registerReceiver(FoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
//        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//        this.registerReceiver(FoundReceiver, filter);
//
//        //Start Discovering local bluetooth signals
//        myBlueToothAdapter.startDiscovery();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        unregisterReceiver(FoundReceiver);
//    }
//
//    private void returnResultAddress(String address){
//        Log.i(TAG, "returnResultAddresss");
//        //Pass the given address back to the client page
//        myBlueToothAdapter.cancelDiscovery();
//        Intent intent = new Intent();
//        intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
//        setResult(Activity.RESULT_OK, intent);
//        finish();
//    }
//
//    private final BroadcastReceiver FoundReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            Log.i(TAG, "broadcastreceiver fond receiver");
//            String action = intent.getAction();
//
//            // When discovery finds a new device
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                // Get the BluetoothDevice object from the Intent
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//
//                //Determine if found device is part of the Mul Community
//                if(device.getName() != null){
//                    if(device.getName().contains("MulTooth")){
//                        //If it is, return the device's address
//                        //This currently implements a "first found" algorithm. A better solution
//                        //might be to get a list of all local community members, then return the one with the
//                        //strongest signal strength.
//                        returnResultAddress(device.getAddress());
//                    }
//                }
//                Toast.makeText(DiscoverDevices.this, "name: " + device.getName() + " " + device.getAddress(), Toast.LENGTH_LONG).show();
//            }
//
//            // When discovery cycle finished
//            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//                Toast.makeText(DiscoverDevices.this, "Finished Searching", Toast.LENGTH_LONG).show();
//            }
//        }
//    };
//}