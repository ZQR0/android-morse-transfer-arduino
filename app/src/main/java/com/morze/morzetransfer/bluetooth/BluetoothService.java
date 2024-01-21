package com.morze.morzetransfer.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.morze.morzetransfer.Constants;
import com.morze.morzetransfer.bluetooth.type.ConnectionType;
import com.morze.morzetransfer.bluetooth.type.BluetoothConstants;
import com.morze.morzetransfer.bluetooth.type.MessageCodes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService implements IBluetoothService {


    private static final String TAG = "BluetoothServiceDebug"; //Tag for debugger
    private static final String DEVICE_NAME_TAG = "device_name";
    private static final String TOAST = "Toast";

    private Context mActivityContext;
    private Activity mActivity;
    private int mState; // State of connection
    private Handler mHandler; // handler that gets info from Bluetooth service
    private BluetoothAdapter mBluetoothAdapter;

    //Threads
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    //UUID's for connection
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");


    //Connection states
    // Constants that indicate the current connection state
    private static final int STATE_CLOSING = -1; // close connection
    private static final int STATE_NONE = 0;       // we're doing nothing
    private static final int STATE_LISTEN = 1;     // now listening for incoming connections
    private static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    private static final int STATE_CONNECTED = 3;  // now connected to a remote device

    //TODO: Конструктор с Handler & BluetoothAdapter инициализацией
    public BluetoothService(Context context, Handler handler, Activity activity) {
        this.mActivityContext = context;
        this.mHandler = handler;
        this.mActivity = activity;
    }

    @Override
    public BluetoothAdapter getBluetoothAdapterFromContext(Context context) {
        BluetoothManager manager = context.getSystemService(BluetoothManager.class);
        return manager.getAdapter();
    }


    public synchronized void start() {
        Log.d(TAG, "Bluetooth Service Started");

        if (this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }

        this.mState = STATE_NONE;
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param isSecured Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean isSecured) {
        Log.d(TAG, String.format("Connect to %s", device));

        if (this.mState == STATE_CONNECTING) {
            if (this.mConnectThread != null) {
                this.mConnectThread.cancel();
                this.mConnectThread = null;
            }
        }

        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }

        this.mConnectThread = new ConnectThread(device, isSecured);
        this.mConnectThread.start();
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    @SuppressLint("MissingPermission")
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, String connectionType) {
        Log.d(TAG, String.format("Connected, Socket type : %s", connectionType));

        if (this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }

        this.mConnectedThread = new ConnectedThread(socket, connectionType);
        this.mConnectedThread.start();

        Message message = this.mHandler.obtainMessage(BluetoothConstants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.BLUETOOTH_DEVICE, device.getName());
        message.setData(bundle);
        this.mHandler.sendMessage(message);
    }

    public synchronized void stop() {
        Log.d(TAG, "Bluetooth Service work stopped");

        if (this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }

        this.mState = STATE_NONE;
    }

    private void connectionFailed() {
        Message msg = mHandler.obtainMessage(BluetoothConstants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothConstants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Update state of connection
        mState = STATE_NONE;

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    private void connectionLost() {
        Message msg = mHandler.obtainMessage(BluetoothConstants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothConstants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (this.mState != STATE_CONNECTED) return;
            r = this.mConnectedThread;
        }

        r.write(out);
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mnSocket;
        private BluetoothDevice mnDevice;
        private String mnSocketConnectionType;


        public ConnectThread(BluetoothDevice device, boolean isSecured) {
            this.mnDevice = device;
            BluetoothSocket beforeInitSocket = null;
            this.mnSocketConnectionType = isSecured ? ConnectionType.SECURED : ConnectionType.UNSECURED;
            try {
                if (isSecured) {
                    if (ActivityCompat.checkSelfPermission(mActivityContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        if (!BluetoothPermissionsUtils.isPermissionGranted(mActivityContext)) {
                            BluetoothPermissionsUtils.requestPermissionsForBluetooth(mActivityContext, mActivity);
                        }
                    }
                    beforeInitSocket = this.mnDevice.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                } else {
                    if (ActivityCompat.checkSelfPermission(mActivityContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        BluetoothPermissionsUtils.requestPermissionsForBluetooth(mActivityContext, mActivity);
                    }
                    beforeInitSocket = this.mnDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                }
            } catch (IOException ex) {
                Log.e(TAG, "Socket Connection Type: " + this.mnSocketConnectionType + "create() failed", ex);
            }

            this.mnSocket = beforeInitSocket;
            mState = STATE_CONNECTING;
        }

        @Override
        public void run() {
            if (ActivityCompat.checkSelfPermission(mActivityContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                if (!BluetoothPermissionsUtils.isPermissionGranted(mActivityContext)) {
                    BluetoothPermissionsUtils.requestPermissionsForBluetooth(mActivityContext, mActivity);
                }
            }
            mBluetoothAdapter.cancelDiscovery();

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mnSocket.connect();
                mState = STATE_CONNECTED;
            } catch (IOException e) {
                // Close the socket
                try {
                    mnSocket.close();
                    mState = STATE_CLOSING;
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mnSocketConnectionType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                // Reset thread logic
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mnSocket, mnDevice, mnSocketConnectionType);
        }

        public void cancel() {
            try {
                mnSocket.close();
            } catch (IOException ex) {
                Log.e(TAG, "Error occurred when Bluetooth Socket closed");
            }
        }
    }

    private class ConnectedThread extends Thread {
        private static final String DEBUG_TAG = "[CONNECTION THREAD]";

        private BluetoothSocket mainBtSocket;
        private BluetoothDevice mnDevice;
        private byte[] buffer;
        private InputStream mnInputStream;
        private OutputStream mnOutputStream;
        private boolean isRunning = false;
        private Handler handler;


        public ConnectedThread(BluetoothSocket btSocket, String connectionType) {
            InputStream threadInputStream = null;
            OutputStream threadOutputStream = null;
            this.mainBtSocket = btSocket;

            try {
                threadInputStream = btSocket.getInputStream();
            } catch (IOException ex) {
                Log.d(DEBUG_TAG, "Something went wrong while input stream initialization");
            }

            try {
                threadOutputStream = btSocket.getOutputStream();
            } catch (IOException ex) {
                Log.d(DEBUG_TAG, "Something went wrong while output stream initialization");
            }

            this.mnInputStream = threadInputStream;
            this.mnOutputStream = threadOutputStream;
            this.isRunning = true;
        }

        public boolean isRunning() {
            return this.isRunning;
        }

        public void setIsRunning(boolean isRunning) {
            this.isRunning = isRunning;
        }

        @Override
        public void run() {
            buffer = new byte[1024]; // 1 MB buffer
            int bytes;
            while (this.isRunning) {
                try {
                    bytes = this.mnInputStream.read(buffer);
                    handler.obtainMessage(MessageCodes.RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();
                } catch (IOException ex) {
                    Log.d(DEBUG_TAG, "Error while reading bytes");
                    break;
                }
            }
        }


        public void cancel() {
            if (this.isRunning) {
                try {
                    this.mainBtSocket.close();
                } catch (IOException ex) {
                    Log.d(DEBUG_TAG, "Error while closing ConnectionThread");
                }
            }

            this.isRunning = false;
        }


        public void write(byte[] bytes) {
            try {
                this.mnOutputStream.write(bytes);

                Message message =  handler.obtainMessage(
                        MessageCodes.RECEIVE_MESSAGE, -1, -1, this.buffer
                );
            } catch (IOException ex) {
                Log.d(DEBUG_TAG, "Error occurred when sending data");

                Message errorMessage = handler.obtainMessage(MessageCodes.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast", "Couldn't send data to other device");
                errorMessage.setData(bundle);
                handler.sendMessage(errorMessage);
            }
        }
    }
}
