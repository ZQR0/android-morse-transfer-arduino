package com.morze.morzetransfer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.morze.morzetransfer.bluetooth.BluetoothPermissionsUtils;
import com.morze.morzetransfer.bluetooth.BluetoothService;
import com.morze.morzetransfer.bluetooth.IBluetoothService;
import com.morze.morzetransfer.converter.MorseTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivityJava extends Activity {

    // Constants
    private static final String TAG = "MainActivityJava";
    private static final int REQUEST_ENABLE_BT = 1;

    // Bluetooth objects
    private BluetoothAdapter adapter;
    private BluetoothService bluetoothService;

    // Other stuff
    private static final MorseTranslator TRANSLATOR = new MorseTranslator();

    // Elements from activity xml
    private Button sendButton;
    private TextView deviceNameField;
    private TextView morseTextField;
    private EditText inputField;
    private FrameLayout frameLayout;
    private ListView deviceList;
    private ImageView closeButton;
    private Button translateButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        this.initAllElements();
        this.enableBluetoothFromActivity();
        this.processMorse();
        this.openDevicesList();
        this.closeOverlayButtonOnClick();

        Log.d(TAG, "All services and components enabled");
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
    protected void onStart() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            BluetoothPermissionsUtils.requestPermissionsForBluetooth(this, this);
        }
        super.onStart();
    }

    @Override
    protected void onResume() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            BluetoothPermissionsUtils.requestPermissionsForBluetooth(this, this);
        }
        super.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.bluetoothService.disconnect();
    }


    private void initAllElements() {
        inputField = findViewById(R.id.input_field);
        sendButton = findViewById(R.id.sendButton);
        morseTextField = findViewById(R.id.morze_output_field);
        deviceNameField = findViewById(R.id.device_name);
        deviceList = findViewById(R.id.list_of_devices);
        frameLayout = findViewById(R.id.overlay_fragment_devices_list);
        closeButton = findViewById(R.id.overlay_close_button);
        translateButton = findViewById(R.id.translateButton);

        this.adapter = BluetoothAdapter.getDefaultAdapter();
        this.bluetoothService = new BluetoothService(this.adapter, this);
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


    private void processMorse() {
        this.translateMorse();
        //this.sendMorse();
    }


    private void translateMorse() {
        this.translateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String input = inputField.getText().toString();
                String res = TRANSLATOR.translate(input);
                morseTextField.setText(res);
                Log.d(TAG, input);
            }
        });
    }


    private void sendMorse() {
        this.sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 final String message = morseTextField.getText().toString();
                if (bluetoothService.isConnected()) {
                    // TODO: 18.02.2024 При использовании этого метода не подключается устройство
//                    bluetoothService.sendData(message);
//                    Log.d(TAG, "sendData success");
                    Toast.makeText(MainActivityJava.this, "Данные отправлены", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Device is not connected");
                    Toast.makeText(MainActivityJava.this, "Нет подключенных устройств для отправления", Toast.LENGTH_SHORT).show();
                }
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
     * Method For showing the list of paired devices as the view
     * */
    // TODO: 29.01.2024 Разделить на более маленькие методы всю логику
    @SuppressLint("MissingPermission")
    private void showBluetoothListDialog() {
        this.frameLayout.setVisibility(View.VISIBLE); // Делаем FrameLayout видимым для пользователя

        List<String> deviceNames = new ArrayList<>(); // список имён устройств (просто строчный тип)
        List<BluetoothDevice> devices = new ArrayList<>(this.adapter.getBondedDevices()); // Список устройств из множества устройств

        for (BluetoothDevice device : devices) { // заполняем список с именами и MAC-адресами из списка раннее подключённых устройств
            // Формат Имя/Mac-адрес
            deviceNames.add(String.format("%s/%s", device.getName(), device.getAddress())); // заполняем список имён и MAC для отображения в нашем deviceList ListView
        }

        deviceNames.add("TEST-DEVICE/8B:67:BA:3C:CA:09"); // ONLY FOR DEBUGGING

        this.deviceToastAlert(devices); // просто небольшое уведомление для отладки

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames); // Адаптер для отображения списка

        this.deviceList.setAdapter(arrayAdapter); // установка адаптера в ListView
        this.deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (ActivityCompat.checkSelfPermission(MainActivityJava.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    BluetoothPermissionsUtils.requestPermissionsForBluetooth(MainActivityJava.this, MainActivityJava.this);
                }
                final String MAC = (String) deviceList.getItemAtPosition(i).toString().split("/")[1]; // получаем MAC-адрес из списка по индексу позиции
                BluetoothDevice targetDevice = adapter.getRemoteDevice(MAC); // Находим удалённый девайс по MAC
                Log.d(TAG, String.format("Target device: %s/%s", targetDevice.getName(), targetDevice.getAddress()));

                bluetoothService.connect(targetDevice);
                Toast.makeText(MainActivityJava.this, bluetoothService.getStateAsMessage(), Toast.LENGTH_SHORT).show();
                if (bluetoothService.isConnected()) {
                    bluetoothService.sendData("Device connected / Устройство подключено");
                    closeOverlay();
                    setDeviceNameFieldText(targetDevice.getName());
                    Toast.makeText(MainActivityJava.this, bluetoothService.getStateAsMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivityJava.this, "Не удалось подлючиться к устройству", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void deviceToastAlert(List<BluetoothDevice> devices) {
        if (devices.size() == 0) {
            Toast.makeText(this, "Устройств не обнаружено", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Устройства найдены", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * For closing an overlay with list of devices
     * */
    private void closeOverlayButtonOnClick() {
        this.closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeOverlay();
                Log.d(TAG, "FrameLayout is invisible");
            }
        });
    }


    private void closeOverlay() {
        this.frameLayout.setVisibility(View.INVISIBLE);
    }


    private void setDeviceNameFieldText(String newText) {
        MainActivityJava.this.deviceNameField.setText(newText);
    }
}
