package com.morze.morzetransfer.bluetooth;

import android.bluetooth.BluetoothDevice;

import java.util.UUID;

public interface IBluetoothService {
    void connect(BluetoothDevice device);
    void disconnect();
    void sendData(String data);
    boolean isConnected();
}
