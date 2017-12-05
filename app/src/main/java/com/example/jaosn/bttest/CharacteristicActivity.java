package com.example.jaosn.bttest;


import android.bluetooth.BluetoothDevice;
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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;


public class CharacteristicActivity extends AppCompatActivity {
    private BluetoothDevice device;
    private String deviceAddress;
    private byte[] data;
    private BluetoothLeService mBluetoothLeService;
    private TextView tv;

    private LineChart chart;
    private List<Entry> entries;
    private ArrayList<byte[]> dataArray;


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

        //for the chart
        chart = findViewById(R.id.chart);
        chart.setBackgroundColor(000);

        //Get the intent that started this activity
        Intent intent = getIntent();
        BluetoothDevice device = intent.getExtras().getParcelable("com.example.jaosn.bttest.BtDevice");
        deviceAddress = device.getAddress();
        Log.d("CharacteristicActivity","getIntent()");

        //Register intent receiver and bind service to this activity
        registerReceiver(mGattUpdateReceiver, ConnectionActivity.makeGattUpdateIntentFilter());
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Log.d("CharacteristicActivity","Service bind!");

        tv = findViewById(R.id.dataField);
        //entries = new ArrayList<>();
        dataArray = new ArrayList<>();

        entries = new ArrayList<>();



        Button button = findViewById(R.id.plotbutton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                entries = formatToPlot(dataArray);
                LineDataSet dataSet = new LineDataSet(entries, "Test");
                dataSet.setColor(getResources().getColor(R.color.ecg_Green)); //This didn't work very well
                dataSet.setLineWidth(3f);
                chart.setBackgroundColor(getResources().getColor(R.color.black));
                final LineData lineData = new LineData(dataSet);
                chart.setData(lineData);
                chart.invalidate(); // refresh
            }
        });

        ToggleButton toggle = (ToggleButton) findViewById(R.id.notificationEnable);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    dataArray.clear();
                    mBluetoothLeService.turnOnNotification();
                } else {
                    // The toggle is disabled
                    mBluetoothLeService.turnOffNotification();
                }
            }
        });



    } //onCreate

    public void screenTapped(View view) {
        mBluetoothLeService.readSavedCharacteristic(); //Is this necessary?

        data = mBluetoothLeService.returnDataToActivity();
        String s = Integer.toString(byteToInt(data));
        tv.setText(s);
        dataArray.add(data);
    }


    public List<Entry> formatToPlot(ArrayList<byte[]> dataArray){
        int i = 0;
        for(byte[] data : dataArray) {
            int y = byteToInt(data);
            entries.add(new Entry(i,y));
            i = i+1;
        }
        return entries;
    }

    public int byteToInt(byte[] data){
        int msb = data[1];
        int lsb = data[0];
        if(msb < 0){
            msb += 256;
        }
        if(lsb < 0){
            lsb += 256;
        }
        return 256*msb + lsb;
    }


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d("Broadcast","Data available");
                Log.d("CharacteristicActivity","Data available broadcast");
            } else if (BluetoothLeService.CHARACTERISTIC_DATA.equals(action)) {
                Log.d("CharacteristicActivity", "Broadcast data available!");
            } else if (BluetoothLeService.CHARACTERISTIC_CHANGED.equals(action)){ //NEW
                data = mBluetoothLeService.returnDataToActivity();
                dataArray.add(data); //END NEW
                Log.d("CharacteristicActivity","Notification on changed value");
            }
        }
    };

    @Override
    protected void onPause(){
        super.onPause();
        //mBluetoothLeService.turnOffNotification();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        //mBluetoothLeService.turnOffNotification();
        unregisterReceiver(mGattUpdateReceiver);
    }

} //Class
