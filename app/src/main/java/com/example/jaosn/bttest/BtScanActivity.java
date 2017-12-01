// Working activity lunch seminarium 2
package com.example.jaosn.bttest;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;

public class BtScanActivity extends Activity {

    private static int REQUEST_ENABLE_BT = 2;
    private BluetoothAdapter mBluetoothAdapter;
    public MyReceiver bReceiver;
    private String deviceName;
    private String deviceAddress;
    private ArrayList<BluetoothDevice> mBtDevices;
    //private ArrayAdapter<BluetoothDevice> mDeviceListAdapter;
    //private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;
    private ListView lv;
    private ArrayAdapter<BluetoothDevice> arrayAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_scan);
        lv = findViewById(R.id.listview);

        mBtDevices = new ArrayList<>(); //Initialize arrayList to store found devices
        //requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);

        //Initialize BT adapter
        //mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothManager bm = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bm.getAdapter();

        // Phone does not support Bluetooth so let the user know and exit.
        if (mBluetoothAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        // Is Bluetooth enabled? If not, enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothAdapter.enable();
        }
        // Initialize broadcast receiver
        bReceiver = new MyReceiver();

        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

        final Button button = findViewById(R.id.scan);
        button.setText(R.string.scan);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), R.string.buttext, Toast.LENGTH_SHORT).show();
                if (!mBluetoothAdapter.isDiscovering()) {
                    button.setText(R.string.stop);
                    registerReceiver(bReceiver, filter);
                    mBtDevices.clear(); //Clear device list before new scan.
                    mBluetoothAdapter.startDiscovery();
                    Log.d("startDiscovery", "Discovery started: " + String.valueOf(mBluetoothAdapter.isDiscovering()));
                    Toast.makeText(getApplicationContext(), "Searching", Toast.LENGTH_SHORT).show();
                } else {
                    button.setText(R.string.scan);
                    unregisterReceiver(bReceiver);
                    mBluetoothAdapter.cancelDiscovery();
                    mBtDevices.clear();
                    Toast.makeText(getApplicationContext(),"Stopped searching", Toast.LENGTH_SHORT).show();
                }
            }
        });

        arrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                mBtDevices);

        lv.setAdapter(arrayAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(),
                        "Click ListItem Number " + position, Toast.LENGTH_SHORT).show();
                BluetoothDevice device = mBtDevices.get(position);
                deviceAddress = device.getAddress();
                deviceName = device.getName();
                Intent intent = new Intent(getApplicationContext(), ConnectionActivity.class);
                intent.putExtra("com.example.jaosn.bttest.deviceAddress", deviceAddress);
                intent.putExtra("com.example.jaosn.bttest.deviceName", deviceName);
                intent.putExtra("com.example.jaosn.bttest.BtDevice", device);
                startActivity(intent);
                unregisterReceiver(bReceiver);
                mBluetoothAdapter.cancelDiscovery();
                //mBluetoothAdapter.disable();
            }
        });

    } // onCreate()


    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //Log.d("intent.getAction()", "got action from intent");
            if (BluetoothDevice.ACTION_FOUND.equals(action)) { //Unnecessary if statement ?
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                deviceName = device.getName(); //Pass these on in an intent
                deviceAddress = device.getAddress();
                //Toast.makeText(context, deviceName + " found", Toast.LENGTH_SHORT).show();
                mBtDevices.add(device); //Adding the found device to the array adapter
                arrayAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        //unregisterReceiver(bReceiver);
        //mBluetoothAdapter.disable();
        mBtDevices.clear();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        mBluetoothAdapter.disable();
        mBtDevices.clear();
    }
    @Override
    protected void onResume() {
        super.onResume();
        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bReceiver, filter);
        mBtDevices.clear();
    }

} //Class close
