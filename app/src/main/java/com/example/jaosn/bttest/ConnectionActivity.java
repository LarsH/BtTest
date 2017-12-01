package com.example.jaosn.bttest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
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
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class ConnectionActivity extends AppCompatActivity {
    private String deviceName;
    private String deviceAddress;
    private BluetoothDevice device;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothLeService mBluetoothLeService;

    private ArrayList<String> theList;
    private List<BluetoothGattService> services;
    private ListView lv;
    private ArrayAdapter<String> arrayAdapter;
    private ArrayAdapter<BluetoothGattCharacteristic> charayAdapter;
    private List<BluetoothGattCharacteristic> charaList;
    private ArrayList<String> uuids;


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
        setTitle("Services");

        //Register broadcast receiver and bind connection service to activity
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Log.d("BluetoothLeService","Service bind");


        //Get the intent that started this activity, and get extra
        Intent intent = getIntent();
        deviceName = intent.getStringExtra("com.example.jaosn.bttest.deviceName");
        deviceAddress = intent.getStringExtra("com.example.jaosn.bttest.deviceAddress");
        if(intent.getExtras().getParcelable("com.example.jaosn.bttest.BtDevice") != null){
            device = intent.getExtras().getParcelable("com.example.jaosn.bttest.BtDevice");
        }

        TextView tv = (TextView)findViewById(R.id.textView);
        tv.setText(deviceName);

        // The button, connects and returns services.
        final Button button = (Button) findViewById(R.id.connect);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothLeService.connect(deviceAddress)){
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Connection failed", Toast.LENGTH_SHORT).show();
                }
                Log.d("onClick", "connectGatt");
            }
        });



        // Populating listview with service UUIDs.
        theList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, theList);
        lv = findViewById(R.id.services);
        lv.setAdapter(arrayAdapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                charaList = services.get(position).getCharacteristics(); //List with characteristics
                Log.d("getCharacteristics()","Got characteristics!");


                //HACK TEST, this actually works
                mBluetoothLeService.readCharacteristic(charaList.get(1));

                // END HACK TEST

                /*
                // This is used to start characteristicActivity
                Intent intent = new Intent(getBaseContext(), CharacteristicActivity.class);
                intent.putExtra("com.example.jaosn.bttest.BtDevice",device);
                intent.putExtra("com.example.jaosn.bttest.size", Integer.toString(charaList.size()));

                for(int i = 0; i < charaList.size();i++){
                    intent.putExtra("com.example.jaosn.bttest.characteristicuuid"+i,charaList.get(i).getUuid().toString());
                }

                for(int i = 0; i < charaList.size();i++){
                    intent.putExtra("com.example.jaosn.bttest.characteristic"+i,charaList.get(i));
                }

                startActivity(intent); */
                //unbindService(mServiceConnection); //Is this necessary?!
            }
        });
    } //onCreate()

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                Log.d("Broadcast","Connected");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
                //Get services and add the UUIDs to a list using getServicesToList
                services = mBluetoothLeService.getSupportedGattServices();
                theList = getServicesToList(services);
                arrayAdapter.notifyDataSetChanged();
                Log.d("Broadcast","Services discovered datasetchange");
            }
        }
    };

    //Method to extract services uuid from list with BluetoothGattService objects
    private ArrayList<String> getServicesToList(List<BluetoothGattService> serviceList){
        for(BluetoothGattService service : serviceList){
            String uuid = service.getUuid().toString();
            theList.add(uuid);
        }
        return theList;
    }


    public static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

} //Class
