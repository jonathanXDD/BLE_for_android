package com.noahliu.ble_example.Controller;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.noahliu.ble_example.Module.Adapter.ExpandableListAdapter;
import com.noahliu.ble_example.Module.Enitiy.ScannedData;
import com.noahliu.ble_example.Module.Enitiy.ServiceInfo;
import com.noahliu.ble_example.Module.Service.BluetoothLeService;
import com.noahliu.ble_example.R;
import java.util.List;

public class DeviceInfoActivity extends AppCompatActivity implements ExpandableListAdapter.OnChildClick {
    public static final String TAG = DeviceInfoActivity.class.getSimpleName()+"My";
    public static final String INTENT_KEY = "GET_DEVICE";
    private BluetoothLeService mBluetoothLeService;
    private ScannedData selectedDevice;
    private TextView tvAddress,tvStatus,tvRespond;
    private ExpandableListAdapter expandableListAdapter;
    private boolean isLedOn = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);
        Toast.makeText(DeviceInfoActivity.this, "d0", Toast.LENGTH_SHORT).show();
        selectedDevice = (ScannedData) getIntent().getSerializableExtra(INTENT_KEY);

        Toast.makeText(DeviceInfoActivity.this, "d1."+selectedDevice.getDeviceByteInfo(), Toast.LENGTH_SHORT).show();
        initBLE();
        Toast.makeText(DeviceInfoActivity.this, "d2", Toast.LENGTH_SHORT).show();
        initUI();
        Toast.makeText(DeviceInfoActivity.this, "d3", Toast.LENGTH_SHORT).show();
    }
    /**初始化藍芽*/
    private void initBLE(){
        /**綁定Service
         * @see BluetoothLeService*/
        Intent bleService = new Intent(this, BluetoothLeService.class);
        bindService(bleService,mServiceConnection,BIND_AUTO_CREATE);
        /**設置廣播*/
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);//連接一個GATT服務
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);//從GATT服務中斷開連接
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);//查找GATT服務
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);//從服務中接受(收)數據
        registerReceiver(mGattUpdateReceiver, intentFilter);
        startService(bleService);
        if (mBluetoothLeService != null) mBluetoothLeService.connect(selectedDevice.getAddress());
        Toast.makeText(DeviceInfoActivity.this, "mBluetoothLeService.."+mBluetoothLeService , Toast.LENGTH_SHORT).show();
    }
    /**初始化UI*/
    private void initUI(){
        expandableListAdapter = new ExpandableListAdapter();
        expandableListAdapter.onChildClick = this::onChildClick;
        ExpandableListView expandableListView = findViewById(R.id.gatt_services_list);
        expandableListView.setAdapter(expandableListAdapter);
        tvAddress = findViewById(R.id.device_address);
        tvStatus = findViewById(R.id.connection_state);
        tvRespond = findViewById(R.id.data_value);
        tvAddress.setText(selectedDevice.getAddress());
        tvStatus.setText("未連線");
        tvRespond.setText("---");
    }
    /**藍芽已連接/已斷線資訊回傳*/
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Toast.makeText(DeviceInfoActivity.this, "onServiceConnected..." , Toast.LENGTH_SHORT).show();
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
                Toast.makeText(DeviceInfoActivity.this, "onServiceConnected...not initialize" , Toast.LENGTH_SHORT).show();
            }
            boolean isConnection = mBluetoothLeService.connect(selectedDevice.getAddress());
            Log.d(TAG, "onServiceConnected...end "+isConnection);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService.disconnect();
            Toast.makeText(DeviceInfoActivity.this, "onServiceDisconnected...end" , Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onServiceDisconnected...end");
        }
    };
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive start, action:" + action);
            int status1= 0;
            /**如果有連接*/
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.d(TAG, "藍芽已連線");
                tvStatus.setText("已連線.1");
                status1 =1;
            }
            /**如果沒有連接*/
            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(TAG, "藍芽已斷開");
                tvStatus.setText("藍芽已斷開.2");
                status1 =2;
            }
            /**找到GATT服務*/
            else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(TAG, "已搜尋到GATT服務");
                List<BluetoothGattService> gattList =  mBluetoothLeService.getSupportedGattServices();
                displayGattAtLogCat(gattList);
                expandableListAdapter.setServiceInfo(gattList);
                tvStatus.setText("已搜尋到GATT服務.3");
                status1 =3;
            }
            /**接收來自藍芽傳回的資料*/
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "接收到藍芽資訊");
                byte[] getByteData = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                StringBuilder stringBuilder = new StringBuilder(getByteData.length);
                for (byte byteChar : getByteData)
                    stringBuilder.append(String.format("%02X ", byteChar));
                String stringData = new String(getByteData);
                Log.d(TAG, "String: "+stringData+"\n"
                        +"byte[]: "+BluetoothLeService.byteArrayToHexStr(getByteData));
                tvRespond.setText("String: "+stringData+"\n"
                        +"byte[]: "+BluetoothLeService.byteArrayToHexStr(getByteData));
                isLedOn = BluetoothLeService.byteArrayToHexStr(getByteData).equals("486173206F6E");
                tvStatus.setText("接收到藍芽資訊.4");
                status1 =4;
            }
            Log.d(TAG, "onReceive end.."+ status1);
        }
    };//onReceive
    /**將藍芽所有資訊顯示在Logcat*/
    private void displayGattAtLogCat(List<BluetoothGattService> gattList){
        for (BluetoothGattService service : gattList){
            Log.d(TAG, "Service: "+service.getUuid().toString());
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()){
                Log.d(TAG, "\tCharacteristic: "+characteristic.getUuid().toString()+" ,Properties: "+
                        mBluetoothLeService.getPropertiesTagArray(characteristic.getProperties()));
                for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()){
                    Log.d(TAG, "\t\tDescriptor: "+descriptor.getUuid().toString());
                }
            }
        }
    }
    /**關閉藍芽*/
    private void closeBluetooth() {
        if (mBluetoothLeService == null) return;
        mBluetoothLeService.disconnect();
        unbindService(mServiceConnection);
        unregisterReceiver(mGattUpdateReceiver);

    }
    @Override
    protected void onStop() {
        super.onStop();
        closeBluetooth();
    }
    /**點擊物件，即寫資訊給藍芽(或直接讀藍芽裝置資訊)*/
    @Override
    public void onChildClick(ServiceInfo.CharacteristicInfo info) {
        Log.d(TAG, "onChildClick." + info + "isLedOn..." + isLedOn);
        String led = "off" ;
        if (!isLedOn) {
            led = "on";
            isLedOn = true ;
        }else{
            isLedOn = false ;
        }

        mBluetoothLeService.sendValue(led,info.getCharacteristic());
    }
}
