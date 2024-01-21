package com.morze.morzetransfer.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import java.util.Set;

public interface IBluetoothService {
    BluetoothAdapter getBluetoothAdapterFromContext(Context context);
}
