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
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;



public class CharacteristicActivity extends AppCompatActivity {
    private BluetoothDevice device;
    private String deviceAddress;
    private byte[] data;
    private BluetoothLeService mBluetoothLeService;

    private LineChart chart;
    private List<Entry> entries;
    private ArrayList<byte[]> dataArray;
    private ArrayList<ArrayList<Entry>> multiChannelList;
    private ArrayList<ArrayList<Entry>> listOfEntries;
    private int tapped;


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
        setTitle("Bluetooth ECG");

        //for the chart
        chart = findViewById(R.id.chart);
        chart.setBackgroundColor(000);
        chart.setNoDataText("");
        // ADD to XML to rotate android:rotation="90"

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

        final TextView tv = findViewById(R.id.dataField);

        //Initialize lists to store stuff
        dataArray = new ArrayList<>();
        entries = new ArrayList<>();
        multiChannelList = new ArrayList<>();



        final Button button = findViewById(R.id.plotbutton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button.setVisibility(View.GONE);
                tv.setVisibility(View.GONE);

                //NEW, this is not correct
                multiChannelList = parseDataIntoLists(dataArray);
                LineDataSet channel0 = new LineDataSet(multiChannelList.get(0), "Channel 0");
                channel0.setColor(getResources().getColor(R.color.channel0));
                LineDataSet channel1 = new LineDataSet(multiChannelList.get(1), "Channel 1");
                channel1.setColor(getResources().getColor(R.color.channel1));
                LineDataSet channel2 = new LineDataSet(multiChannelList.get(2), "Channel 2");
                channel2.setColor(getResources().getColor(R.color.ecg_Green));
                List<ILineDataSet> dataSets = new ArrayList<>();
                dataSets.add(channel0);
                dataSets.add(channel1);
                dataSets.add(channel2);

                LineData data = new LineData(dataSets);
                chart.setData(data);
                chart.invalidate(); // refresh

            }
        });

        ToggleButton toggle = (ToggleButton) findViewById(R.id.notificationEnable);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    dataArray.clear();
                    chart.clear(); //Clear before plot
                    chart.invalidate();
                    mBluetoothLeService.turnOnNotification();
                    button.setVisibility(View.VISIBLE);
                    tv.setVisibility(View.VISIBLE);
                } else {
                    // The toggle is disabled
                    mBluetoothLeService.turnOffNotification();
                }
            }
        });

    } //onCreate


    public void screenTapped(View view){
        if(tapped == 4){
            tapped = 0;
            Toast.makeText(getApplicationContext(), "BAJS", Toast.LENGTH_SHORT).show();
        } else if(tapped == 3){
            Toast.makeText(getApplicationContext(), "Snart får du", Toast.LENGTH_SHORT).show();
            tapped += 1;
        } else {
            tapped += 1;
        }
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

    public ArrayList<ArrayList<Entry>> parseDataIntoLists(ArrayList<byte[]> dataArray) {
        Entry tmp;
        int numChannels = 8;
        listOfEntries = new ArrayList<ArrayList<Entry>>();

        for(int channel = 0; channel < numChannels; channel++) {
            // Fix this nicer, if possible...
            listOfEntries.add(channel, new ArrayList<Entry>());
        }

        for(byte[] data : dataArray) {
            int timestamp = (0x10000 * byteToIntAtIndex(data,2)) + byteToIntAtIndex(data,0); //Get timestamp
            float x = timestamp/(float)0x10000; //Convert system tick to second.
            for(int channel = 0; channel < numChannels; channel++) {  //Get y value for each channel
                int y = byteToIntAtIndex(data,4+2*channel);
                tmp = new Entry(x,y); //Data point for one channel
                listOfEntries.get(channel).add(tmp);
            }
        }
        return listOfEntries;
    }

    public int byteToIntAtIndex(byte[] data, int index){
        int msb = data[index + 1];
        int lsb = data[index];
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
                String s = Integer.toString(byteToInt(data));
                TextView tv = findViewById(R.id.dataField);
                tv.setText(s);
                Log.d("CharacteristicActivity","Notification on changed value");
            }
        }
    };

    @Override
    public void onBackPressed(){
        unregisterReceiver(mGattUpdateReceiver);
        CharacteristicActivity.super.onBackPressed();
    }

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
