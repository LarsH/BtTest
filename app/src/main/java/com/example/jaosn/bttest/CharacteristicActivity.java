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
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;

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

    private LineChart chart;

    private TextView tv;

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

        /* for the chart
        chart = findViewById(R.id.chart);
        XML
        <com.github.mikephil.charting.charts.LineChart
        android:id="@+id/chart"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
        */

        //Get the intent that started this activity
        Intent intent = getIntent();
        BluetoothDevice device = intent.getExtras().getParcelable("com.example.jaosn.bttest.BtDevice");
        deviceAddress = device.getAddress();
        int position = Integer.parseInt(intent.getStringExtra("com.example.jaosn.bttest.position"));
        Log.d("CharacteristicActivity","getIntent()");

        //Bind service to this activity
        registerReceiver(mGattUpdateReceiver, ConnectionActivity.makeGattUpdateIntentFilter());
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Log.d("CharacteristicActivity","Service bind!");

        tv = findViewById(R.id.dataField);
        

    } //onCreate

    public void screenTapped(View view) {
        mBluetoothLeService.readSavedCharacteristic();
        data = mBluetoothLeService.returnDataToActivity();
        tv.setText(new String(data));
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d("Broadcast","Data available");
            } else if (BluetoothLeService.CHARACTERISTIC_DATA.equals(action)){
                Log.d("CharacteristicActivity","Broadcast data available!");
                //Get the bundle in intent
                Bundle b = intent.getBundleExtra("Data");
                //data = b.getByte(data);
            }
        }
    };

} //Class
