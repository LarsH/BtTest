package com.example.jaosn.bttest;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import java.util.List;
import java.util.UUID;


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private int count = 0;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.jaosn.bttest.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.jaosn.bttest.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.jaosn.bttest.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.jaosn.bttest.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.jaosn.bttest.EXTRA_DATA";
    public final static String CHARACTERISTIC_DATA =
            "com.example.jaosn.bttest.CHARACTERISTIC_DATA";
    public final static String CHARACTERISTIC_CHANGED =
            "com.example.jaosn.bttest.CHARACTERISTIC_CHANGED";
    public final static String CHARACTERISTIC_WRITE =
            "com.example.jaosn.bttest.CHARACTERISTIC_WRITE";
    public final static String WRITTEN_DESCRIPTOR =
            "com.example.jaosn.bttest.WRITTEN_DESCRIPTOR";

    //TEST method to "save" this characteristic we want to read from
    private BluetoothGattCharacteristic thisCharacteristic;
    private BluetoothGattService thisService;


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices()); //Triggers callback if true

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                Log.d("Service", "onServicesDiscovered: discovered services");
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                Log.d("Service","onCharacteristicRead callback");
            } else {
                Log.d("Service", "onCharacteristicread: Read failed!");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            broadcastUpdate(CHARACTERISTIC_CHANGED); //NEW
            Log.d("Service","Characteristic changed");
        }
        @Override
        public void onCharacteristicWrite (BluetoothGatt gatt,
                                    BluetoothGattCharacteristic characteristic,
                                    int status){
            if(status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(CHARACTERISTIC_WRITE);
                Log.d("Service","onCharacteristicWrite callback");
                Log.d("Service","Write successful");
            } else {
                Log.d("Service","Write failed");
            }
        }
        @Override
        public void onDescriptorWrite (BluetoothGatt gatt,
                                BluetoothGattDescriptor descriptor,
                                int status){
            if(status == BluetoothGatt.GATT_SUCCESS){
                Log.d("Service","Written descriptor");
                broadcastUpdate(WRITTEN_DESCRIPTOR);
                //Test ECG Enable here!
                //enableECG(true);
            } else {
                Log.d("Service","Descriptor write failed");
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        //New code
        //intent.putExtra("Characteristic",characteristic);
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
        if (thisCharacteristic != characteristic)
            saveCharacteristic(characteristic);
        Log.d("Service","readCharacteristic() " + characteristic.toString());
    }

    public BluetoothGattCharacteristic saveCharacteristic(BluetoothGattCharacteristic characteristic){
        thisCharacteristic = characteristic;
        return thisCharacteristic;
    }

    public void readSavedCharacteristic(){
        readCharacteristic(thisCharacteristic);
    }

    public byte[] returnDataToActivity(){
        count += 1;
        Log.d("Service","getValue() from 'saved' characteristic: " + count);
        return thisCharacteristic.getValue();
    }

    public void turnOnNotification(){
        mBluetoothGatt.setCharacteristicNotification(thisCharacteristic, true);
        List<BluetoothGattDescriptor> descriptorList = thisCharacteristic.getDescriptors();
            UUID uuid = descriptorList.get(0).getUuid();
            BluetoothGattDescriptor descriptor = thisCharacteristic.getDescriptor(uuid);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
    }

    public void turnOffNotification(){
        mBluetoothGatt.setCharacteristicNotification(thisCharacteristic, false);
        List<BluetoothGattDescriptor> descriptorList = thisCharacteristic.getDescriptors();
        UUID uuid = descriptorList.get(0).getUuid();
        BluetoothGattDescriptor descriptor = thisCharacteristic.getDescriptor(uuid);
        descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);

    }

    public void enableECG(boolean enable){
        byte[] value = new byte[1];
        BluetoothGattCharacteristic enableECG = thisService.getCharacteristic(UUID.fromString("F0001121-0451-4000-B000-000000000000"));
        if(enable){
            value[0] = (byte) 1;
            //Write '1' to characteristic in enable service
            enableECG.setValue(value);
            mBluetoothGatt.writeCharacteristic(enableECG);
        } else {
            value[0] = (byte) 0;
            //Write '0' to characteristic in enable service
            enableECG.setValue(value);
            mBluetoothGatt.writeCharacteristic(enableECG);
        }
    }

    public void dataAck(boolean ack){
        byte[] value = new byte[1];
        BluetoothGattCharacteristic dataAck = thisService.getCharacteristic(UUID.fromString("F0001124-0451-4000-B000-000000000000"));
        if (ack){
            value[0] = (byte) 1;
            //Write '1' to characteristic in ack service to signal data ack
            dataAck.setValue(value);
            mBluetoothGatt.writeCharacteristic(dataAck);
        }
    }

    public BluetoothGattService saveService(BluetoothGattService service){
        thisService = service;
        return thisService;
    }


    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
        public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}
