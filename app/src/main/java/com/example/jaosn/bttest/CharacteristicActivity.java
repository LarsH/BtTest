package com.example.jaosn.bttest;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.sql.Connection;
import java.util.ArrayList;



public class CharacteristicActivity extends AppCompatActivity {
    private ListView lv;
    private int size;
    private String tmp;
    private ArrayList<BluetoothGattCharacteristic> charaList;
    private ArrayList<String> charaStringList;
    private BluetoothGattCharacteristic chara;
    private BluetoothDevice device;
    private String deviceAddress;
    private ArrayAdapter<String> adapter;
    private byte[] data;
    private BtScanActivity.MyReceiver myReceiver;

    private BluetoothLeService mBluetoothLeService;
    private BluetoothManager mBluetoothManager;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder)service).getService();
            if (mBluetoothLeService != null) {
                // Automatically connects to the device upon successful start-up initialization.
                Toast.makeText(getApplicationContext(), "Service initialized", Toast.LENGTH_SHORT).show();
                Log.d("BluetoothLeService","Service initialized");
            }
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            finish();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_characteristic);
        setTitle("Characteristics");
        lv = findViewById(R.id.characteristics);

        /* //HAck TEST
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();

        // END HACK */

        //myReceiver = new BtScanActivity.MyReceiver();

        registerReceiver(mGattUpdateReceiver, ConnectionActivity.makeGattUpdateIntentFilter());
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


        //Get the intent that started this activity, and get extra
        Intent intent = getIntent();
        size = Integer.parseInt(intent.getStringExtra("com.example.jaosn.bttest.size"));

        charaStringList = new ArrayList<>();
        for(int i = 0; i < size; i++){
            String tmp = intent.getExtras().getString("com.example.jaosn.bttest.characteristicuuid"+i);
            charaStringList.add(tmp);
        }

        charaList = new ArrayList<>();
        for(int i = 0; i < size; i++){
            chara = intent.getExtras().getParcelable("com.example.jaosn.bttest.characteristic"+i);
            charaList.add(chara);
        }


        if(intent.getExtras().getParcelable("com.example.jaosn.bttest.BtDevice") != null){
            device = intent.getExtras().getParcelable("com.example.jaosn.bttest.BtDevice");
            deviceAddress = device.getAddress();
        }

        adapter = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_list_item_1, charaStringList);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //data = charaList.get(position).getValue();

                mBluetoothLeService.readCharacteristic(charaList.get(position));
                mBluetoothLeService.connect(deviceAddress);
                BluetoothGattService test = charaList.get(position).getService(); //This returns null, not good
                Log.d("Try to get service: ",test.toString());
                if(charaList.get(position).getValue() != null){
                    data = charaList.get(position).getValue();
                    Log.d("getValue()", "Got Value: " + data.toString());
                } else {
                    Log.d("getValue()","Value read is null");
                }
                Log.d("onItemClick",Integer.toString(position));
                Log.d("onItemClick got",charaList.get(position).toString());
                //Log.d("getValue", data.toString());
                //Toast.makeText(getApplicationContext(), "Data: " + data.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        );

    } //onCreate

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d("Broadcast","Data available");
                intent = getIntent();
                BluetoothGattCharacteristic characteristic = intent.getExtras().getParcelable("Characteristic");
                data = characteristic.getValue();
                Log.d("Data read: ", data.toString());
            }
        }
    };

} //Class
