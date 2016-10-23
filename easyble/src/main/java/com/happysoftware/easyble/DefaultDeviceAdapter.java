package com.happysoftware.easyble;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;

import java.util.UUID;

import com.happysoftware.easyble.utils.BleUtil;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by zxx on 2016/7/16.
 */
public abstract class DefaultDeviceAdapter<T> implements DeviceAdapter<T>{

    private final String TAG = getClass().getSimpleName();
    private BleCenterManager mBleCenterManager;
    protected BleDeviceListener mBleDeviceListener;
    protected BleDevice mBleDevice;
    private RxBleDevice mInternalRxBleDevice;

    private Subscription mNotificationSubscription;
    private Subscription mIndicatorSubscription;
    private Subscription mConnectionStateSubscription;

    protected Observable<RxBleConnection> mRxBleConnectionObservable;
    protected RxBleConnection mRxBleConnection;

    @Override
    public Subscription enableNotification() {

        UUID[] notificationUUIDs = notificationUUIDs();

        if (notificationUUIDs == null || notificationUUIDs.length == 0) {
            return null;
        }

        Observable<PairData>[] observables = new Observable[notificationUUIDs.length];

        Subscription subscription = mRxBleConnectionObservable.flatMap(rxBleConnection -> {


            for (int i = 0; i < notificationUUIDs.length; i++) {

                UUID uuid = notificationUUIDs[i];

                Observable<PairData> pairDataObservable = rxBleConnection.setupNotification(uuid)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(observable -> notifyInteractUpdate(mBleDevice, new BleStep("enableNotification " + uuid.toString() + " complete")))
                        .flatMap(notificationObservable -> notificationObservable)
                        .map(bytes -> new PairData(uuid, bytes));

                observables[i] = pairDataObservable;

            }

            return Observable.merge(observables);

        }).observeOn(AndroidSchedulers.mainThread())
                .doOnNext(pairData -> notifyInteractUpdate(mBleDevice, new BleStep(String.format("Get raw data from %s", pairData.uuid.toString()), pairData.data)))
                .subscribe(pairData -> processData(pairData.uuid, pairData.data),
                        throwable -> notifyInteractError(mBleDevice, throwable, null));

        return subscription;
    }

    @Override
    public Subscription enableIndicator() {

        UUID[] indicatorUUIDs = indicatorUUIDs();

        if (indicatorUUIDs == null || indicatorUUIDs.length == 0) {
            return null;
        }

        Observable<PairData>[] observables = new Observable[indicatorUUIDs.length];

        Subscription subscription = mRxBleConnectionObservable.flatMap(rxBleConnection -> {


            for (int i = 0; i < indicatorUUIDs.length; i++) {

                UUID uuid = indicatorUUIDs[i];

                Observable<PairData> pairDataObservable = rxBleConnection.setupIndication(uuid)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(observable -> notifyInteractUpdate(mBleDevice, new BleStep("enableIndicator " + uuid.toString() + " complete")))
                        .flatMap(notificationObservable -> notificationObservable)
                        .map(bytes -> new PairData(uuid, bytes));

                observables[i] = pairDataObservable;

            }

            return Observable.merge(observables);

        }).observeOn(AndroidSchedulers.mainThread())
                .doOnNext(pairData -> notifyInteractUpdate(mBleDevice, new BleStep(String.format("Get raw data from %s", pairData.uuid.toString()), pairData.data)))
                .subscribe(pairData -> processData(pairData.uuid, pairData.data),
                        throwable -> notifyInteractError(mBleDevice, throwable, null));

        return subscription;
    }

    @Override
    public void disconnect() {
        // TODO: 2016/8/2 should check whether subscription has been unsubscribed?
        if (mNotificationSubscription != null && !mNotificationSubscription.isUnsubscribed()){
            mNotificationSubscription.unsubscribe();
        }
        if (mIndicatorSubscription != null && !mIndicatorSubscription.isUnsubscribed()){
            mIndicatorSubscription.unsubscribe();
        }
        if (mConnectionStateSubscription != null && !mConnectionStateSubscription.isUnsubscribed()){
            mConnectionStateSubscription.unsubscribe();
        }
    }

    class PairData{

        UUID uuid;
        byte[] data;

        public PairData(UUID uuid, byte[] data) {
            this.uuid = uuid;
            this.data = data;
        }
    }

    @Override
    public void connectThenStart(BleDevice bleDevice) {
        setupDevice(bleDevice);
        initAdapter();
        mNotificationSubscription = enableNotification();
        mIndicatorSubscription = enableIndicator();
    }

    private void initAdapter() {
        setupConnectionStateChange();
        setupConnection();
    }

    private void setupConnection() {

        boolean enableAutoConnect = mBleCenterManager.getBleConfig().isEnableAutoConnect();
        mRxBleConnectionObservable = mInternalRxBleDevice.establishConnection(mBleCenterManager.getContext(),enableAutoConnect)
                .doOnNext(rxBleConnection -> mRxBleConnection = rxBleConnection);

    }

    public void setupDevice(BleDevice bleDevice){
        this.mBleDevice = bleDevice;
        this.mInternalRxBleDevice = mBleDevice.getInternalRxBleDevice();
    }

    private void setupConnectionStateChange() {
        if (mBleDevice == null || mInternalRxBleDevice == null){
            return;
        }

        mConnectionStateSubscription = mInternalRxBleDevice.observeConnectionStateChanges()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(rxBleConnectionState -> notifyDeviceStateChange(mBleDevice, BleUtil.convertInternalRXState(rxBleConnectionState)),
                        throwable -> notifyInteractError(mBleDevice, throwable, new BleStep("Setup connection change error")));

    }

    @Override
    public void writeCharacteristic(UUID uuid, byte[] data, BleStep step){

        mRxBleConnection.writeCharacteristic(uuid,data)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(bytes -> notifyInteractUpdate(mBleDevice,new BleStep("Begin step "+step.action,step.rawData,step.data)))
                .subscribe(bytes -> notifyInteractUpdate(mBleDevice,new BleStep("Step "+step.action+" complete",step.rawData,step.data))
                        ,throwable -> notifyInteractError(mBleDevice,throwable,step));

    }

    @Override
    public void writeCharacteristic(UUID uuid, byte[] data){

        mRxBleConnection.writeCharacteristic(uuid,data)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(bytes -> notifyInteractUpdate(mBleDevice,new BleStep("Begin write data to UUID :"+uuid.toString())))
                .subscribe(bytes -> notifyInteractUpdate(mBleDevice,new BleStep(String.format("Step write data to UUID: %s complete",uuid.toString())))
                        ,throwable -> notifyInteractError(mBleDevice,throwable,new BleStep(String.format("Step write data to UUID: %s error",uuid.toString()))));

    }

    public void readCharacteristic(UUID uuid, BleStep step){
        mRxBleConnection.readCharacteristic(uuid)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(bytes -> notifyInteractUpdate(mBleDevice,new BleStep("Begin step "+step.action,step.rawData,step.data)))
                .subscribe(bytes -> notifyInteractUpdate(mBleDevice,new BleStep("Step "+step.action+" complete",bytes))
                        ,throwable -> notifyInteractError(mBleDevice,throwable,step));
    }

    public void readCharacteristic(UUID uuid){
        mRxBleConnection.readCharacteristic(uuid)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(bytes -> notifyInteractUpdate(mBleDevice,new BleStep("Begin read data from UUID :"+uuid.toString())))
                .subscribe(bytes -> notifyInteractUpdate(mBleDevice,new BleStep(String.format("Step read data from UUID: %s complete",uuid.toString()),bytes))
                        ,throwable -> notifyInteractError(mBleDevice,throwable,new BleStep(String.format("Step read data from UUID: %s error",uuid.toString()))));
    }

    public DefaultDeviceAdapter(BleCenterManager bleCenterManager){
        mBleCenterManager = bleCenterManager;
        if (mBleCenterManager != null){
            mBleDeviceListener = mBleCenterManager.getInternalBleDeviceListener();
        }
    }

    private BleDeviceListener getBleDeviceListener(){
        return mBleDeviceListener = mBleCenterManager.getInternalBleDeviceListener();
    }

    protected void notifyDeviceStateChange(BleDevice device, BleDeviceState state){
        if (getBleDeviceListener() != null){
            mBleDeviceListener.onDeviceStateChange(device,state);
        }
    }

    protected void notifyDataComing(BleDevice device, BleDataType type, Object data){
        if (getBleDeviceListener() != null){
            mBleDeviceListener.onDataComing(device,type,data);
        }
    }

    protected void notifyInteractComplete(BleDevice device, Object finalResult){
        if (getBleDeviceListener() != null){
            mBleDeviceListener.onInteractComplete(device,finalResult);
        }
    }

    protected void notifyInteractUpdate(BleDevice device, BleStep step){
        if (getBleDeviceListener() != null){
            mBleDeviceListener.onInteractUpdate(device,step);
        }
    }

    protected void notifyInteractError(BleDevice device, Throwable throwable, BleStep step){
        if (getBleDeviceListener() != null){
            mBleDeviceListener.onInteractError(device,throwable,step);
        }
    }

}
