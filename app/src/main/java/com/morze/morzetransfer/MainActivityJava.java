package com.morze.morzetransfer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.morze.morzetransfer.bluetooth.BluetoothPermissionsUtils;
import com.morze.morzetransfer.bluetooth.BluetoothService;
import com.morze.morzetransfer.bluetooth.BluetoothServiceManager;
import com.morze.morzetransfer.converter.MorseTranslator;

import java.util.Set;


public class MainActivityJava extends Activity {

    // Constants
    private static final String TAG = "MainActivityJava";
    private static final int REQUEST_ENABLE_BT = 1;

    // Bluetooth objects
    private BluetoothAdapter adapter;
    private BroadcastReceiver discoveryReciever;
    private BluetoothServiceManager manager;
    private Handler handler;


    // Elements from activity xml
    private ImageView settingsImage;
    private Button sendButton;
    private TextView morseTextField;
    private EditText inputField;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        Log.d(TAG, "onCreate: activity created, logs work");
        System.out.println("onCreate: activity created, logs work");

        this.initAllElements();
        BluetoothPermissionsUtils.requestPermissionsForBluetooth(this, this);
        this.enableBluetoothFromActivity();
        this.switchToSettings();
        this.translateAndShow();
        this.registerAllReceivers();

        this.manager = new BluetoothServiceManager(this, handler, this);
        Log.d(TAG, "Bluetooth manager initialized");

        //ONLY FOR DEBUGGING
        // TODO: 12.12.2023 remove after debugging and final tests
        String deviceName = this.getDeviceNameFromIntentExtras();
        if (deviceName != null) {
            Toast.makeText(this, deviceName, Toast.LENGTH_SHORT).show();
            this.chooseConnectionMethod(deviceName);
            Log.d(TAG, "Connection method in choosing");
        } else {
            Toast.makeText(this, "Пусто", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth включен", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Включение Bluetooth отменено", Toast.LENGTH_SHORT).show();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        this.registerAllReceivers();
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            BluetoothPermissionsUtils.requestPermissionsForBluetooth(this, this);
        }
        if (this.adapter != null) {
            adapter.cancelDiscovery();
        }
        if (this.discoveryReciever != null) {
            unregisterReceiver(this.discoveryReciever);
            this.discoveryReciever = null;
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            BluetoothPermissionsUtils.requestPermissionsForBluetooth(this, this);
        }
        if (this.adapter != null) {
            adapter.cancelDiscovery();
        }
        if (this.discoveryReciever != null) {
            unregisterReceiver(this.discoveryReciever);
            this.discoveryReciever = null;
        }

        super.onDestroy();
    }

    private void initAllElements() {
        settingsImage = (ImageView) findViewById(R.id.settings_image);
        inputField = (EditText) findViewById(R.id.input_field);
        sendButton = (Button) findViewById(R.id.sendButton);
        morseTextField = (TextView) findViewById(R.id.morze_output_field);
    }


    private void switchToSettings() {
        settingsImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeActivity(SettingsActivityJava.class);
                finish();
                Log.d(TAG, "onClick: pressed");
            }
        });
    }

    private void changeActivity(Class<?> destinationActivity) {
        Intent intent = new Intent(MainActivityJava.this, destinationActivity);
        startActivity(intent);
    }

    // Здесь происходит и перевод, и вывод в TextView, и отправление сообщения
    private void translateAndShow() {
        MorseTranslator translator = new MorseTranslator();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String input = inputField.getText().toString();
                String res = translator.translate(input);

                morseTextField.setText(res);
                manager.sendMessage(res);

                Log.d(TAG, input);
            }
        });
    }


    private void enableBluetoothFromActivity() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            BluetoothPermissionsUtils.requestPermissionsForBluetooth(this, this);
        }
        adapter = new BluetoothService(this, this.handler, this).getBluetoothAdapterFromContext(this);
        if (adapter != null && !adapter.isEnabled()) {
            // Предлагаем пользователю включить Bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

            if (adapter.isEnabled()) {
                Toast.makeText(this, Constants.BLUETOOTH_CONNECTED, Toast.LENGTH_SHORT).show();
            }
        } else if (adapter == null) {
            Toast.makeText(this, Constants.NOT_SUPPORT, Toast.LENGTH_SHORT).show();
        }
    }

    private void registerAllReceivers() {
        IntentFilter discoveryFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(this.discoveryReciever, discoveryFilter);
    }

    private String getDeviceNameFromIntentExtras() {
        Bundle bundle = getIntent().getExtras();

        // FIXME: 11.12.2023 getString method always returns null
        if (bundle != null) {
            return bundle.getString(Constants.BLUETOOTH_DEVICE);
        }

        return null;
    }


    private void chooseConnectionMethod(String deviceName) {
        if (deviceName != null) {
            BluetoothDevice deviceFromBonded = this.getTargetDeviceFromBonded(deviceName);
            if (deviceFromBonded != null) {
                this.manager.connectDevice(deviceFromBonded, false);
                Log.d(TAG, "Device found in bonded");
            }

            this.startDeviceDiscovery(deviceName);
            Log.d(TAG, "Device discovery started");
        } else {
            Log.e(TAG, "deviceName is null");
        }
    }

    private void startDeviceDiscovery(String deviceName) {
        // Регистрация BroadcastReceiver для обнаружения устройств Bluetooth
        this.discoveryReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ActivityCompat.checkSelfPermission(MainActivityJava.this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    BluetoothPermissionsUtils.requestPermissionsForBluetooth(MainActivityJava.this, MainActivityJava.this);
                }
                final String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    Log.d(TAG, "Action is ACTION_FOUND");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        Log.d(TAG, "Device is not null");
                        if (device.getName().equals(deviceName)) {
                            Toast.makeText(context, "Подключено к устройству: " + device.getName(), Toast.LENGTH_SHORT).show();
                            BluetoothDevice newDevice = adapter.getRemoteDevice(device.getAddress());
                            manager.connectDevice(device, false);
                            Log.d(TAG, String.format("Device with name %s and MAC-address %s connected by BluetoothManager class", device.getName(), device.getAddress()));

                            if (adapter.cancelDiscovery()) {
                                Log.d(TAG, "Discovery cancelled by adapter");
                            }
                        }
                    }

                    Log.e(TAG, "Device is null, couldn't find the device");
                }
            }
        };

        if (this.adapter.startDiscovery()) {
            Log.d(TAG, "Discovery started by BluetoothAdapter");
        }
    }


    /**
     * Method for searching a device in recently connected devices
     * If such not exists, we start start discovering by other method
     *
     * @param deviceName Name of bluetooth device for connection
     * @return Device MAC-Address for connection
     */
    private BluetoothDevice getTargetDeviceFromBonded(String deviceName) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            BluetoothPermissionsUtils.requestPermissionsForBluetooth(this, this);
        }

        Set<BluetoothDevice> devices = this.getBondedDevices();
        if (devices.size() > 0) {
            for (BluetoothDevice device : devices) {
                if (device.getName().equals(deviceName)) {
                    return device;
                }
                return null;
            }
        }

        return null;
    }

    private Set<BluetoothDevice> getBondedDevices() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            BluetoothPermissionsUtils.requestPermissionsForBluetooth(this, this);
        }
        return this.adapter.getBondedDevices();
    }


}
