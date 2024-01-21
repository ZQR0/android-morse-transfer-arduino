package com.morze.morzetransfer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class SettingsActivityJava extends Activity {

    private static final String TAG = "SettingActivityJava";

    ImageView backButton;
    EditText bluetoothDeviceName;
    Button saveButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        this.initAllElements();

        this.switchToMainViewByButton();
        this.saveAndSwitch();
    }

    private void switchToMainViewByButton() {
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeActivity(MainActivityJava.class);
                finish();
                Log.d(TAG, "OnClick works");
                System.out.println("OnClick works");
            }
        });
    }

    private void initAllElements() {
        this.backButton = (ImageView) findViewById(R.id.back_button_image);
        this.saveButton = (Button) findViewById(R.id.save_btn);
        this.bluetoothDeviceName = (EditText) findViewById(R.id.bluetooth_device_name);
    }

    private void saveAndSwitch() {
        this.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeActivity(MainActivityJava.class);
                finish();
                Log.d(TAG, "Switched after saving bluetooth-device name");
            }
        });
    }

    private void changeActivity(Class<?> destinationActivity) {
        Intent intent = new Intent(SettingsActivityJava.this, destinationActivity);
        String deviceNameFromField = this.bluetoothDeviceName.getText().toString();
        if (deviceNameFromField.length() != 0) {
            String device = this.getBluetoothDeviceName();
            intent.putExtra(Constants.BLUETOOTH_DEVICE, device);
            Log.d(TAG, String.format("Extra data %s put", device));
        }
        startActivity(intent);
    }

    private String getBluetoothDeviceName() {
        String deviceName = bluetoothDeviceName.getText().toString();

        if (deviceName.length() != 0) {
            Log.d(TAG, deviceName);
            return deviceName;
        }

        Log.d(TAG, "Device name is empty string");
        return "";
    }

}
