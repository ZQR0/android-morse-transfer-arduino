package com.morze.morzetransfer.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class BluetoothServiceManager {

    private static final boolean IS_SECURED = false;

    private final Context context;
    private final Handler handler;
    private final Activity activity;
    private final BluetoothService bluetoothService;

    public BluetoothServiceManager(Context context, Handler handler, Activity activity) {
        this.context = context;
        this.handler = handler;
        this.activity = activity;
        this.bluetoothService = new BluetoothService(this.context, this.handler, this.activity);
    }


    public void sendMessage(String data) {
        if (data.length() > 0) {
            byte[] bytes = data.getBytes();
            this.bluetoothService.write(bytes);
        }
    }

    public void connectDevice(BluetoothDevice device, boolean isSecured) {
        this.bluetoothService.connect(device, false);
    }

    public void stopConnection() {
        this.bluetoothService.stop();
    }

}
