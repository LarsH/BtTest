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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
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
    private int pressed = 0;
    private int count = 0;
    private ArrayList<Float> filterCoeff;
    private ProgressBar progressbar;
    private int progressStatus = 0;


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
        //chart.setBackgroundColor(000);
        chart.setBackgroundColor(getResources().getColor(R.color.black));
        chart.setNoDataText("");
        Legend legend = chart.getLegend();
        legend.setTextColor(getResources().getColor(R.color.white));
        // ADD to XML to rotate android:rotation="90"

        //Get the intent that started this activity
        Intent intent = getIntent();
        device = intent.getExtras().getParcelable("com.example.jaosn.bttest.BtDevice");
        deviceAddress = device.getAddress();
        Log.d("CharacteristicActivity","getIntent()");

        //Register intent receiver and bind service to this activity
        registerReceiver(mGattUpdateReceiver, ConnectionActivity.makeGattUpdateIntentFilter());
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Log.d("CharacteristicActivity","Service bind!");

        final TextView tv = findViewById(R.id.dataField);
        final ToggleButton toggle = findViewById(R.id.notificationEnable);
        final Button button = findViewById(R.id.plotbutton);
        final Button enableButton = findViewById(R.id.enable);
        progressbar = findViewById(R.id.progressBar);
        tv.setVisibility(View.GONE);
        button.setVisibility(View.GONE); //Plot button invisible on start
        enableButton.setVisibility(View.GONE);
        enableButton.setText("Enable");
        progressbar.setVisibility(View.GONE);


        //Initialize lists to store stuff
        dataArray = new ArrayList<>();
        entries = new ArrayList<>();
        multiChannelList = new ArrayList<>();
        filterCoeff  = new ArrayList<>();


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button.setVisibility(View.GONE);
                tv.setVisibility(View.GONE);
                toggle.setChecked(false);
                progressbar.setVisibility(View.GONE);

                ArrayList<Float> yValues0 = parseByteToFloat(dataArray,0);
                ArrayList<Float> yValues1 = parseByteToFloat(dataArray,1);

                //Print data in log
                Log.d("Received data, Ch0:", yValues0.toArray().toString());
                Log.d("Received data, Ch1:", yValues1.toArray().toString());

                ArrayList<Entry> plotValues0;
                ArrayList<Entry> plotValues1;

                plotValues0 = parseFloatToEntry(yValues0);
                plotValues1 = parseFloatToEntry(yValues1);

                LineDataSet channel0 = new LineDataSet(plotValues0, "Channel 0");
                channel0.setColor(getResources().getColor(R.color.channel0));

                LineDataSet channel1 = new LineDataSet(plotValues1, "Channel 1");
                channel1.setColor(getResources().getColor(R.color.ecg_Green));

                List<ILineDataSet> dataSets = new ArrayList<>();
                dataSets.add(channel0);
                dataSets.add(channel1);
                channel1.setDrawCircles(false);
                channel0.setDrawCircles(false);

                LineData data = new LineData(dataSets);

                chart.setData(data);
                chart.invalidate(); // refresh
            }
        });


        enableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pressed == 0){
                    //enableButton.setText("Stop");
                    mBluetoothLeService.enableECG(true);
                    pressed = 1;
                    tv.setVisibility(View.VISIBLE);
                    enableButton.setVisibility(View.GONE);
                    button.setVisibility(View.VISIBLE);
                    progressbar.setVisibility(View.VISIBLE);
                    progressbar.setProgress(progressStatus);
                    Toast.makeText(getApplicationContext(), "Starting ECG measurement", Toast.LENGTH_LONG).show();
                } else {
                    //enableButton.setText("Enable");
                    mBluetoothLeService.enableECG(false);
                    button.setVisibility(View.VISIBLE);
                    pressed = 0;
                }
            }
        });


        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    dataArray.clear();
                    chart.clear(); //Clear before plot
                    chart.invalidate();
                    mBluetoothLeService.turnOnNotification(); //ECG enable in callback
                    Log.d("CharacteristicActivity","Notification enabled");
                    //button.setVisibility(View.VISIBLE);
                    enableButton.setVisibility(View.VISIBLE);
                    Log.d("CharacteristicActivity","Enable ECG true");
                } else {
                    // The toggle is disabled
                    enableButton.setVisibility(View.GONE);
                    button.setVisibility(View.GONE);
                    mBluetoothLeService.turnOffNotification();
                }
            }
        });

    } //onCreate

    public ArrayList<Float> parseByteToFloat(ArrayList<byte[]> receivedData, int channel){
        ArrayList<Float> parsed = new ArrayList<>();
        int SAMPLES = 10;

        for(byte[] data : receivedData){
            for(int i = channel; i < SAMPLES; i +=2 ){
                float y = (float) byteToIntAtIndex(data,2*i);
                parsed.add(y);
            }
        }
        return parsed;
    }

    public ArrayList<Entry> parseFloatToEntry(ArrayList<Float> yVals){
        ArrayList<Entry> toPlot = new ArrayList<>();
        int x = 0;
        for(float y : yVals){
            toPlot.add(new Entry(x,y));
            x += 1;
        }
        return toPlot;
    }

    public ArrayList<Float> filterData(ArrayList<Float> yvals){
        ArrayList<Float> filteredData = new ArrayList<>();
        float A[] ={1.0f, -1.93304166f, 3.97751368f, -3.79368207f, 3.63320288f, -1.61091454f, 0.76021461f};
        float B[] = {0.87555508f, -1.76890716f, 3.8099105f, -3.79982396f, 3.8099105f, -1.76890716f, 0.87555508f};
        ArrayList<Float> oldOutVal = new ArrayList<>();
        ArrayList<Float> oldInVal = new ArrayList<>();
        for(int i = 0; i < 7; i++){
            oldOutVal.add(i,0f);
            oldInVal.add(i,0f);
        }
        for(Float input : yvals){
            //DO filtering
            Float outVal = B[0]*input;
            for(int i = 1; i < 7; i++){
                outVal += B[i]*oldInVal.get(i-1) - A[i]*oldOutVal.get(i-1);
            }
            outVal = outVal/A[0];
            oldOutVal.add(0,outVal);
            oldInVal.add(0,input);
            filteredData.add(outVal);
        }
        return filteredData;
    }


    public void screenTapped(View view){
        mBluetoothLeService.readSavedCharacteristic();
    }


    public ArrayList<ArrayList<Entry>> parseDataIntoLists(ArrayList<byte[]> dataArray) {
        Entry tmp;
        int SAMPLES = 10;
        int x = 0;
        listOfEntries = new ArrayList<ArrayList<Entry>>();

        //for(int channel = 0; channel < numChannels; channel++) {
            // Fix this nicer, if possible...
            listOfEntries.add(0, new ArrayList<Entry>());
        //}

        for(byte[] data : dataArray) {
            //int timestamp = (0x10000 * byteToIntAtIndex(data,2)) + byteToIntAtIndex(data,0); //Get timestamp
            //float x = timestamp/(float)0x10000; //Convert system tick to second.

            for(int channel = 0; channel < SAMPLES; channel++) {  //Get y value for each channel
                int y = byteToIntAtIndex(data,2*channel);
                tmp = new Entry(x,y); //Data point for one channel
                x += 1;
                listOfEntries.get(0).add(tmp);
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
                //NEW
                data = mBluetoothLeService.returnDataToActivity();
                dataArray.add(data);
                //String s = Integer.toString(byteToInt(data));
                //TextView tv = findViewById(R.id.dataField);
                //tv.setText(s);
            } else if (BluetoothLeService.CHARACTERISTIC_DATA.equals(action)) {
                Log.d("CharacteristicActivity", "Broadcast data available!");
            } else if (BluetoothLeService.CHARACTERISTIC_CHANGED.equals(action)){ //NEW
                data = mBluetoothLeService.returnDataToActivity();
                dataArray.add(data); //END NEW
                //String s = Integer.toString(byteToInt(data));
                //TextView tv = findViewById(R.id.dataField);
                //tv.setText(s);
                count += 1;
                progressStatus += 1;
                progressbar.setProgress(progressStatus);
                mBluetoothLeService.dataAck(true); //Ack when read.
                if(count == 200){
                    Toast.makeText(getApplicationContext(), "Data transmission done", Toast.LENGTH_SHORT).show();
                    progressbar.setVisibility(View.GONE);
                    count = 0;
                    progressStatus = 0;
                }
            } else if (BluetoothLeService.WRITTEN_DESCRIPTOR.equals(action)){
                Toast.makeText(getApplicationContext(), "Data transmission done", Toast.LENGTH_LONG).show();
                Log.d("CharacteristicActivity","Descriptor written broadcast received");
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
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mGattUpdateReceiver);
    }

} //Class
