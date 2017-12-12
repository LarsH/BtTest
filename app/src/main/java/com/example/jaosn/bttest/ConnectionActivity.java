package com.example.jaosn.bttest;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import java.util.ArrayList;
import java.util.List;



public class ConnectionActivity extends AppCompatActivity {
    private String deviceName;
    private String deviceAddress;
    private BluetoothDevice device;
    private BluetoothLeService mBluetoothLeService;

    private ArrayList<String> servicesList;
    private List<BluetoothGattService> services;
    private ListView lvServices;
    private ListView lvCharacteristics;
    private ArrayAdapter<String> arrayAdapter;
    private ArrayAdapter<String> arrayAdapterCharas;
    private List<BluetoothGattCharacteristic> charaList;
    private ArrayList<String> charaStringList;


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
            //mBluetoothLeService = null;
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        //Get the intent that started this activity, and get extra
        Intent intent = getIntent();
        if(intent.getExtras().getParcelable("com.example.jaosn.bttest.BtDevice") != null){
            device = intent.getExtras().getParcelable("com.example.jaosn.bttest.BtDevice");
        }
        deviceName = device.getName();
        deviceAddress = device.getAddress();

        setTitle(deviceName + " Services");

        //Register broadcast receiver and bind connection service to activity
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Log.d("BluetoothLeService","Service bind");



        // Populating listview with service UUIDs.
        servicesList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, servicesList);
        lvServices = findViewById(R.id.services);
        lvServices.setAdapter(arrayAdapter);

        // Populating listview with characteristics UUIDs.
        charaStringList = new ArrayList<>();
        arrayAdapterCharas = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,charaStringList);
        lvCharacteristics = findViewById(R.id.characteristic);
        lvCharacteristics.setAdapter(arrayAdapterCharas);


        lvServices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                arrayAdapterCharas.clear();
                charaList = services.get(position).getCharacteristics(); //List with characteristics
                charaStringList = getCharacteristicsToList(charaList); //List with string UUIDs of characteristics

                mBluetoothLeService.saveService(services.get(position)); //HACK!!!! NEW

                arrayAdapterCharas.notifyDataSetChanged();
                Log.d("ConnectionActivity","getCharacteristics()");
            }
        });


        lvCharacteristics.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d("onItemClick charalist","Attempting to read characteristic");
                mBluetoothLeService.readCharacteristic(charaList.get(i));
                Intent intent = new Intent(getBaseContext(),CharacteristicActivity.class);
                intent.putExtra("com.example.jaosn.bttest.BtDevice", device);
                startActivity(intent);
                unregisterReceiver(mGattUpdateReceiver); //NEW
            }
        });

    } //onCreate()



    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                Log.d("ConnectionActivity","Broadcast connected");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
                //Get services and add the UUIDs to a list using getServicesToList
                services = mBluetoothLeService.getSupportedGattServices();
                servicesList = getServicesToList(services); //For display in listview
                arrayAdapter.notifyDataSetChanged();
                Log.d("ConnectionActivity","Broadcast services discovered datasetchange");
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)){
                Log.d("ConnectionActivity","Broadcast data available!");
            } else if (BluetoothLeService.CHARACTERISTIC_DATA.equals(action)){
                Log.d("ConnectionActivity","Broadcast CHARACTERISTIC_DATA");
            }
        }
    };

    //Method to extract services uuid from list with BluetoothGattService objects
    private ArrayList<String> getServicesToList(List<BluetoothGattService> serviceList){
        for(BluetoothGattService service : serviceList){
            String uuid = service.getUuid().toString();
            servicesList.add(uuid);
        }
        return servicesList;
    }

    private ArrayList<String> getCharacteristicsToList(List<BluetoothGattCharacteristic> charaList){
        for(BluetoothGattCharacteristic chara : charaList){
            String uuid = chara.getUuid().toString();
            charaStringList.add(uuid);
        }
        return charaStringList;
    }


    public static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.CHARACTERISTIC_CHANGED);
        return intentFilter;
    }

    //NEW
/*
    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    } */

    @Override
    protected void onResume(){
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        /*
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Log.d("BluetoothLeService","Service bind");

        lvServices.setAdapter(arrayAdapter);
        lvCharacteristics.setAdapter(arrayAdapterCharas);
        arrayAdapter.notifyDataSetChanged();
        arrayAdapterCharas.notifyDataSetChanged(); */
    }


} //Class
