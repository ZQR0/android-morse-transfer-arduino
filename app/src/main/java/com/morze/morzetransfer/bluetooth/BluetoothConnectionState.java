package com.morze.morzetransfer.bluetooth;

public interface BluetoothConnectionState {
    int STATE_CONNECTION_ERROR = -1;
    int STATE_NONE = 0;
    int STATE_CONNECTING = 1;
    int STATE_CONNECTED = 2;
}
