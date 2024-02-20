package com.morze.morzetransfer.bluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService implements IBluetoothService {

    // Constants
    private static final String TAG = "BluetoothServiceJava";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private int mState;
    private int mNewState;

    // Adapter and threads
    private final BluetoothAdapter btAdapter;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private Activity activity;


    @SuppressLint("MissingPermission")
    public BluetoothService(BluetoothAdapter bluetoothAdapter, Activity activity) {
        btAdapter = bluetoothAdapter;
        Log.i(TAG, String.format("Bluetooth adapter initialized %s", btAdapter.getName()));
        this.activity = activity;
        mState = BluetoothConnectionState.STATE_NONE;
        mNewState = mState;
    }

    @Override
    public synchronized void connect(BluetoothDevice device) {
        // Обнуляем все подключения
        if (mState == BluetoothConnectionState.STATE_CONNECTING) {//Возможно придётся убрать
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }


        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Создаём поток на подключение и стартуем его
        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    public synchronized void connected(BluetoothSocket socket) {
        // Обнуляем все подключения
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
            Log.d(TAG, "Connect Thread canceled from connected() method");
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
            Log.d(TAG, "Connected Thread canceled from connected() method");
        }

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }

    @Override
    public synchronized void disconnect() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
            Log.d(TAG, "Connect Thread canceled from disconnect()");
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
            Log.d(TAG, "Connected Thread canceled from disconnect()");
        }

        mState = BluetoothConnectionState.STATE_NONE;
        Log.i(TAG, "Disconnected from all threads");
    }

    @Override
    public void sendData(String data) {
        // FIXME: 18.02.2024 Где-то тут проблема, которая крашит подключение
        ConnectedThread r;
        if (this.isConnected() && this.connectedThread != null) {
            this.connectedThread.write(data.getBytes());
        }
    }

    @Override
    public boolean isConnected() {
        return mState == BluetoothConnectionState.STATE_CONNECTED;
    }

    public String getStateAsMessage() {
        switch (mState) {
            case BluetoothConnectionState.STATE_NONE: return "Состояния нет";
            case BluetoothConnectionState.STATE_CONNECTING: return "Подключение...";
            case BluetoothConnectionState.STATE_CONNECTED: return "Подключено";
            case BluetoothConnectionState.STATE_CONNECTION_ERROR: return "Ошибка подключения";
        }

        return "Состояние не записалось";
    }

    /**
     * Class for managing connection to other bluetooth devices
     * In this class we just use connect and disconnect methods, nothing more
     *
     * @author Yaroslav
     */
    // Поток для управления подключением
    private final class ConnectThread extends Thread {
        private final BluetoothSocket btSocket;
        private final BluetoothDevice btDevice;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmpSocket = null;
            this.btDevice = device;

            try {
                tmpSocket = btDevice.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                if (tmpSocket.isConnected()) {
                    Log.d(TAG, "RFCOMM tmpSocket connected");
                } else {
                    Log.e(TAG, "RFCOMM tmpSocket is not connected");
                }
            } catch (IOException ex) {
                Log.e(TAG, "IOException handled\n" + ex.getMessage());
            }

            this.btSocket = tmpSocket;
            Log.d(TAG, "ConnectThread start");
            mState = BluetoothConnectionState.STATE_CONNECTING;
        }

        @Override
        @SuppressLint("MissingPermission")
        public void run() {
            btAdapter.cancelDiscovery();
            Log.d(TAG, "Discovery of Bluetooth adapter canceled");
            try {
                btSocket.connect();
                Log.d(TAG, "Socket connect method called");
            } catch (IOException ex) {
                try {
                    btSocket.close();
                    Log.e(TAG, "Closing connection because of exception\n" + ex);
                } catch (IOException ex1) {
                    Log.e(TAG, "Unable to disconnect from socket\n" + ex1.getMessage());
                }
            }

            // Делаем ресет ConnectThread, потому что мы завершили подключение
            synchronized (BluetoothService.this) {
                connectThread = null;
                Log.d(TAG, "ConnectThread reset");
            }

            connected(btSocket);
            Log.d(TAG, "Performing connection from ConnectThread to ConnectedThread");
        }

        @SuppressLint("MissingPermission")
        public void cancel() {
            try {
                btSocket.close();
                Log.d(TAG, "Connection closed from cancel() Connect Thread");
            } catch (IOException ex) {
                Log.e(TAG, "Unable to disconnect from socket\n" + ex.getMessage());
            }
        }
    }


    // Поток для управления отправлением данных (работает во время активного подключения)
    private final class ConnectedThread extends Thread {

        private final BluetoothSocket btSocket;
        private final OutputStream outputStream;
        private final InputStream inputStream;

        public ConnectedThread(BluetoothSocket socket) {
            btSocket = socket;
            OutputStream tmpOutputStream = null;
            InputStream tmpInputStream = null;

            if (socket.isConnected()) {
                try {
                    tmpOutputStream = this.btSocket.getOutputStream();
                    tmpInputStream = this.btSocket.getInputStream();
                } catch (IOException ex) {
                    Log.e(TAG, "IOException handled\n" + ex.toString());
                }
            } else {
                Log.e(TAG, "Socket is not connected to ConnectedThread");
            }

            outputStream = tmpOutputStream;
            inputStream = tmpInputStream;
            mState = BluetoothConnectionState.STATE_CONNECTED;
        }

        @Override
        public void run() {
            Log.d(TAG, "ConnectedThread start");
            byte[] buffer = new byte[1024];
            int bytes;
            // Method for reading data from connected device
            if (this.isConnected()) {
                try {
                    bytes = inputStream.read(buffer);
                    Log.d(TAG, "Listening from ConnectedThread");
                    Log.d(TAG, "Bytes: " + Integer.toString(bytes));
                } catch (IOException ex) {
                    Log.e(TAG, ex.toString());
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException ex) {
                Log.e(TAG, ex.toString());
            }
        }

        @SuppressLint("MissingPermission")
        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException ex) {
                Log.e(TAG, "Unable to disconnect from socket\n" + ex.toString());
            }
        }

        public boolean isConnected() {
            return btSocket.isConnected();
        }
    }
}
