package com.happysoftware.easyble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import com.happysoftware.easyble.exception.EasyBleException;
import com.happysoftware.easyble.exception.EasyBleScanException;
import com.happysoftware.easyble.exception.EasyBleUnsupportedDeviceException;
import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.internal.RxBleLog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by zxx on 2016/7/16.
 */
public class BleCenterManager {

    private static final String TAG = BleCenterManager.class.getSimpleName();

    private Context mContext;

    private BleConfig mBleConfig;

    private RxBleClient mRxBleClient;
    private Subscription mScanSubscription;

    private BleDeviceListener mInternalBleDeviceListener = null;

    private List<BleDeviceListener> mBleDeviceListenerList;

    private BluetoothAdapter mBluetoothAdapter;

    private HashMap<BleDevice,DeviceAdapter> mBleDeviceDeviceAdapterHashMap = new HashMap<>();

    private volatile BleDevice mConnectedDevice = null;

    private volatile DeviceAdapter mConnectedDeviceAdapter = null;

    private List<DeviceAdapter.Factory> mDeviceAdapterFactories = new ArrayList<>();

    private List<DeviceAdapter> mDeviceAdapters = new ArrayList<>();

    private BleCenterManager(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        initInternalListener();
    }


    /**
     * init BleCenterManager with any Android context, can only call once
     * @param context
     */
    public void init(Context context) {
        mContext = context;
        mRxBleClient = RxBleClient.create(mContext);
        RxBleClient.setLogLevel(RxBleLog.DEBUG);
        setBleConfig(new BleConfig.BleConfigBuilder().setStartScanAfterDisconnected(false).setStopScanAfterConnected(false).createBleConfig());
    }

    /**
     * init BleCenterManager with any Android context and ble config, can only call once
     * @param context
     */
    public void init(Context context,BleConfig bleConfig){
        mContext = context;
        mRxBleClient = RxBleClient.create(mContext);
        RxBleClient.setLogLevel(RxBleLog.DEBUG);
        setBleConfig(bleConfig);
    }

    public void setBleConfig(BleConfig bleConfig) {
        mBleConfig = bleConfig;
    }

    public BleConfig getBleConfig() {
        return mBleConfig;
    }

    private static class SingletonHolder{

        private static BleCenterManager instance = new BleCenterManager();
    }

    public static BleCenterManager getInstance(){
        return SingletonHolder.instance;
    }


    public Context getContext() {
        return mContext;
    }


    private final static int CMD_START_SCAN = 1;
    private final static int CMD_STOP_SCAN = 2;
    private final static int CMD_DISCONNECT_DEVICE = 3;
    private final static int CMD_CLOSE_BLUETOOTH = 4;
    private final static int CMD_OPEN_BLUETOOTH = 5;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case CMD_START_SCAN:{
                    startScan();
                    break;
                }
                case CMD_STOP_SCAN:{
                    stopScan();
                    break;
                }
            }
            super.handleMessage(msg);
        }
    };

    private void initInternalListener() {

        mBleDeviceListenerList = new ArrayList<>();

        mInternalBleDeviceListener = new BleDeviceListener() {
            @Override
            public void onDeviceStateChange(BleDevice device, BleDeviceState state) {
                if (state == BleDeviceState.BLE_DEVICE_STATE_CONNECTED) {
                    mConnectedDevice = device;
                    mConnectedDeviceAdapter = getBoundAdapter(mConnectedDevice);
                    if (mBleConfig.isStopScanAfterConnected()) {
                        if (mHandler.hasMessages(CMD_STOP_SCAN)) {
                            mHandler.removeMessages(CMD_STOP_SCAN);
                        }
                        mHandler.sendEmptyMessageDelayed(CMD_STOP_SCAN, 500);
                    }
                } else {
                    if (device.equals(mConnectedDevice)) {
                        mConnectedDevice = null;
                    }
                    if (state == BleDeviceState.BLE_DEVICE_STATE_DISCONNECTED) {
                        // TODO: 2016/10/29 why should I just add disconnectDevice here? I don't remember? may introduce issue?
                        // disconnectDevice(mConnectedDevice);
                        if (mBleConfig.isStartScanAfterDisconnected()) {
                            if (mHandler.hasMessages(CMD_START_SCAN)) {
                                mHandler.removeMessages(CMD_START_SCAN);
                            }
                            mHandler.sendEmptyMessageDelayed(CMD_START_SCAN, 500);
                        }
                    }
                }
                notifyDeviceStateChange(device, state);
            }


            @Override
            public void onDataComing(BleDevice device, BleDataType type, Object data) {
                notifyDataComing(device, type, data);
            }



            @Override
            public void onInteractComplete(BleDevice device, Object finalResult) {
                notifyInteractComplete(device, finalResult);
            }



            @Override
            public void onInteractUpdate(BleDevice device, BleStep step) {
                notifyInteractUpdate(device, step);
            }


            @Override
            public void onInteractError(BleDevice device, Throwable throwable, BleStep step) {
                notifyInteractError(device, throwable, step);
            }

            @Override
            public void onScanStart() {
                notifyScanStart();
            }

            @Override
            public void onScanStop() {
                notifyScanStop();
            }

            @Override
            public void onScanUpdate(BleScanResult scanResult) {
                notifyScanUpdate(scanResult);
            }

            @Override
            public void onScanError(Throwable throwable) {
                notifyScanError(throwable);
            }

        };
    }

    protected BleDeviceListener getInternalBleDeviceListener(){
        return mInternalBleDeviceListener;
    }

    public void addBleDeviceListener(BleDeviceListener bleDeviceListener) {
        if (!mBleDeviceListenerList.contains(bleDeviceListener)){
            mBleDeviceListenerList.add(bleDeviceListener);
        }
    }

    public boolean removeBleDeviceListener(BleDeviceListener bleDeviceListener) {
        if (mBleDeviceListenerList.isEmpty()){
            return false;
        }else if (mBleDeviceListenerList.contains(bleDeviceListener)){
            return mBleDeviceListenerList.remove(bleDeviceListener);
        }else {
            return false;
        }
    }

    public boolean isSupportBLE() {
        return mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * Only can use in UI main thread
     * @param activity
     * @throws EasyBleException
     */
    public void openBluetooth(Activity activity, int requestCode) throws EasyBleException {

        if (!isSupportBLE()){
            throw new EasyBleException("Not supported Android BLE");
        }

        if (!isBluetoothOpen()){
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, requestCode);
        }

    }

    /**
     * Only can use in UI main thread
     * @param fragment
     * @throws EasyBleException
     */
    public void openBluetooth(Fragment fragment, int requestCode) throws EasyBleException {

        if (!isSupportBLE()){
            throw new EasyBleException("Not supported Android BLE");
        }

        if (!isBluetoothOpen()){
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            fragment.startActivityForResult(enableBtIntent, requestCode);
        }

    }
    public boolean isBluetoothOpen(){
        if (mBluetoothAdapter != null){
            return mBluetoothAdapter.isEnabled();
        }
        return false;
    }


    public void closeBluetooth() throws EasyBleException {

        if (!isSupportBLE()){
            throw new EasyBleException("Not supported Android BLE");
        }

        if (isBluetoothOpen()){
            mBluetoothAdapter.disable();
        }

    }

    public int getBluetoothAdapterState(){
        if (mBluetoothAdapter != null){
            return mBluetoothAdapter.getState();
        }else {
            throw new IllegalStateException("Not supported ble");
        }
    }
    public void prepareClose(){

        stopScan();
        disconnectCurrentDeviceIfPossible();

    }


    public void disconnectCurrentDeviceIfPossible(){

        // TODO: 2016/10/29 can't use mConnectedDevice here
        if (mConnectedDevice != null){
            Log.e(TAG,"disconnect already connected device...");
            disconnectDevice(mConnectedDevice);
            mConnectedDevice = null;
            mConnectedDeviceAdapter = null;
            return;
        }

        if (mConnectedDeviceAdapter != null){
            Log.e(TAG,"connected device adapter not close yet, so close device adapter...");
            mConnectedDeviceAdapter.disconnect();
            mConnectedDeviceAdapter = null;
        }

        Log.e(TAG,"no connected device or device adapter need to close, so don't need to disconnect");

    }

    public BleDevice getConnectedDevice(){
        return mConnectedDevice;
    }

    public DeviceAdapter getConnectedDeviceAdapter(){
        return getBoundAdapter(getConnectedDevice());
    }

    public void disconnectDevice(BleDevice device){
        DeviceAdapter deviceAdapter = getBoundAdapter(device);
        if (deviceAdapter != null){
            Log.e(TAG,"disconnect already connected device in adapter...");
            deviceAdapter.disconnect();
            if (device.equals(mConnectedDevice)){
                mConnectedDevice = null;
                mConnectedDeviceAdapter = null;
            }
        }else {
            Log.e(TAG,"disconnect no adapter error...");
        }
    }

    public void startScan(){

        if (mScanSubscription != null && !mScanSubscription.isUnsubscribed()){
            return;
        }
        mScanSubscription = mRxBleClient.scanBleDevices()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(rxBleScanResult -> {

                    notifyScanUpdate(new BleScanResult(rxBleScanResult));

                },this::onScanFailure);

        notifyScanStart();

    }


    public boolean isScanning() {
        return mScanSubscription != null && !mScanSubscription.isUnsubscribed();
    }


    private void onScanFailure(Throwable throwable) {
        notifyScanFailure(throwable);
    }

    private void notifyScanFailure(Throwable throwable) {
        for (BleDeviceListener deviceListener: mBleDeviceListenerList){
            if (deviceListener != null){
                if (throwable instanceof BleScanException){
                    deviceListener.onScanError(new EasyBleScanException((BleScanException) throwable));
                }else {
                    deviceListener.onScanError(throwable);
                }
            }
        }
    }

    public void stopScan(){

        if (isScanning()){
            mScanSubscription.unsubscribe();
        }
        mScanSubscription = null;
        notifyScanStop();
    }

    public void connectThenStart(BleDevice device) throws EasyBleException {

        DeviceAdapter appropriateDeviceAdapter= findAppropriateDeviceAdapter(device);
        if (appropriateDeviceAdapter != null){
            appropriateDeviceAdapter.connectThenStart(device);
            boundDeviceWithAdapter(device,appropriateDeviceAdapter);
        }else {

        }

    }

    private void boundDeviceWithAdapter(BleDevice device, DeviceAdapter deviceAdapter){
        mBleDeviceDeviceAdapterHashMap.put(device,deviceAdapter);
    }

    private DeviceAdapter getBoundAdapter(BleDevice device){
        return mBleDeviceDeviceAdapterHashMap.get(device);
    }

    private DeviceAdapter findAppropriateDeviceAdapter(BleDevice bleDevice) throws EasyBleException {

        if (mDeviceAdapterFactories == null || mDeviceAdapterFactories.isEmpty()){
            throw new EasyBleException("Device adapter factories empty!");
        }

        for (DeviceAdapter adapter:mDeviceAdapters){
            String[] nameList = adapter.supportedNames();
            if (nameList != null && nameList.length > 0){
                for (String name:nameList){
                    if (bleDevice.getDeviceName().equalsIgnoreCase(name)){
                        return adapter;
                    }
                }
            }
            String[] nameRegExpList = adapter.supportedNameRegExps();
            if (nameRegExpList != null && nameRegExpList.length >0){
                for (String nameRegExp:nameRegExpList){
                    if (Pattern.matches(nameRegExp,bleDevice.getDeviceName())){
                        return adapter;
                    }
                }
            }

        }

        throw new EasyBleUnsupportedDeviceException(bleDevice);

    }

    public void addDeviceAdapterFactory(DeviceAdapter.Factory factory){

        if (factory == null){
            throw new NullPointerException("DeviceAdapter factory is null!");
        }

        mDeviceAdapterFactories.add(factory);
        mDeviceAdapters.add(factory.buildDeviceAdapter());
    }

    /**
     * remove device adapter factory. Not exposed to user, need to rethink about this method. Whether we should provide this method to user?
     * @param factory
     */
    private void removeDeviceAdatperFactory(DeviceAdapter.Factory factory){

        if (factory == null){
            throw new NullPointerException("DeviceAdapter factory is null!");
        }
        mDeviceAdapterFactories.remove(factory);
    }

    public List<DeviceAdapter.Factory> getDeviceAdapterFactories() {
        return Collections.unmodifiableList(mDeviceAdapterFactories);
    }

    private void notifyDeviceStateChange(BleDevice device, BleDeviceState state) {
        for (BleDeviceListener deviceListener: mBleDeviceListenerList){
            deviceListener.onDeviceStateChange(device, state);
        }
    }

    private void notifyDataComing(BleDevice device, BleDataType type, Object data) {
        for (BleDeviceListener deviceListener: mBleDeviceListenerList){
            deviceListener.onDataComing(device,type,data);
        }
    }

    private void notifyInteractComplete(BleDevice device, Object finalResult) {
        for (BleDeviceListener deviceListener: mBleDeviceListenerList){
            deviceListener.onInteractComplete(device,finalResult);
        }
    }

    private void notifyInteractUpdate(BleDevice device, BleStep step) {
        for (BleDeviceListener deviceListener: mBleDeviceListenerList){
            deviceListener.onInteractUpdate(device,step);
        }
    }

    private void notifyInteractError(BleDevice device, Throwable throwable, BleStep step) {
        for (BleDeviceListener deviceListener: mBleDeviceListenerList){
            deviceListener.onInteractError(device,throwable,step);
        }
    }

    private void notifyScanStart() {
        for (BleDeviceListener deviceListener: mBleDeviceListenerList){
            deviceListener.onScanStart();
        }
    }

    private void notifyScanStop() {
        for (BleDeviceListener deviceListener: mBleDeviceListenerList){
            deviceListener.onScanStop();
        }
    }

    private void notifyScanUpdate(BleScanResult scanResult) {
        for (BleDeviceListener deviceListener: mBleDeviceListenerList){
            deviceListener.onScanUpdate(scanResult);
        }
    }

    private void notifyScanError(Throwable throwable) {
        for (BleDeviceListener deviceListener: mBleDeviceListenerList){
            deviceListener.onScanError(throwable);
        }
    }
}
