package com.morze.morzetransfer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.lights.LightState;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.morze.morzetransfer.bluetooth.BluetoothPermissionsUtils;
import com.morze.morzetransfer.bluetooth.BluetoothService;
import com.morze.morzetransfer.bluetooth.BluetoothServiceManager;
import com.morze.morzetransfer.converter.MorseTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class MainActivityJava extends Activity {

    // Constants
    private static final String TAG = "MainActivityJava";
    private static final int REQUEST_ENABLE_BT = 1;

    private final FragmentManager fragmentManager = this.getFragmentManager();

    // Bluetooth objects
    private BluetoothAdapter adapter;
    private BroadcastReceiver discoveryReciever;
    private BluetoothServiceManager manager;
    private Handler handler;


    // Elements from activity xml
    private ImageView settingsImage;
    private Button sendButton;
    private TextView deviceNameField;
    private TextView morseTextField;
    private EditText inputField;
    private FrameLayout frameLayout;
    private ListView deviceList;
    private ImageView closeButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        Log.d(TAG, "onCreate: activity created, logs work");
        System.out.println("onCreate: activity created, logs work");

        this.initAllElements();
        BluetoothPermissionsUtils.requestPermissionsForBluetooth(this, this);
        this.enableBluetoothFromActivity();
        this.translateAndShow();
        this.openDevicesList();
        this.closeOverlayButtonOnClick();

        this.manager = new BluetoothServiceManager(this, handler, this);
        Log.d(TAG, "Bluetooth manager initialized");
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
//        settingsImage = (ImageView) findViewById(R.id.settings_image);
        inputField = (EditText) findViewById(R.id.input_field);
        sendButton = (Button) findViewById(R.id.sendButton);
        morseTextField = (TextView) findViewById(R.id.morze_output_field);
        deviceNameField = findViewById(R.id.device_name);
        deviceList = findViewById(R.id.list_of_devices);
        frameLayout = findViewById(R.id.overlay_fragment_devices_list);
        closeButton = findViewById(R.id.overlay_close_button);
    }


    private void openDevicesList() {
        this.deviceNameField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Device name field clicked");
                showBluetoothListDialog();
            }
        });
    }

//    private void switchToSettings() {
//        settingsImage.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                changeActivity(SettingsActivityJava.class);
//                finish();
//                Log.d(TAG, "onClick: pressed");
//            }
//        });
//    }


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


    /**
     * Method for enabling bluetooth from activity, if it's not enabled yet
     * */
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



    /**
     * @return set of paired bluetooth devices
     * */
    private Set<BluetoothDevice> getBondedDevices() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            BluetoothPermissionsUtils.requestPermissionsForBluetooth(this, this);
        }
        return this.adapter.getBondedDevices();
    }


    /**
     * Method for showing the list of paired devices as the view
     * */
    private void showBluetoothListDialog() {
        Log.d(TAG, "");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            BluetoothPermissionsUtils.requestPermissionsForBluetooth(this, this);
        }

        this.frameLayout.setVisibility(View.VISIBLE); // Делаем FrameLayout видимым для пользователя

        List<String> deviceNames = new ArrayList<>();
        List<BluetoothDevice> devices = new ArrayList<>();

        for (BluetoothDevice device : this.getBondedDevices()) {
            deviceNames.add(device.getName());
            devices.add(device);
        }

        //Сделать предупреждение о пустом и не пустом списке через виджеты Toast
        if (devices.size() == 0) {
            Toast.makeText(this, "Устройств не обнаружено", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Устройства найдены", Toast.LENGTH_SHORT).show();
        }

        final String[] deviceArray = deviceNames.toArray(new String[deviceNames.size()]);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceArray);

        this.deviceList.setAdapter(arrayAdapter);
        this.deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedItem = (String) adapterView.getItemAtPosition(i);
                Log.d(TAG, String.format("Выбран элемент %s", selectedItem));
                /// TODO: 26.01.2024 connection to devices
            }
        });
    }

    /**
     * Method for overlay closing
     * */
    private void closeOverlayButtonOnClick() {
        this.closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                frameLayout.setVisibility(View.INVISIBLE);
                Log.d(TAG, "FrameLayout is invisible");
            }
        });
    }


}
