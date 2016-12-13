package com.happysoftware.easyble;

import com.happysoftware.easyble.utils.BleUtil;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.observables.SyncOnSubscribe;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

/**
 * Created by zxx on 2016/7/16.
 */
public abstract class DefaultDeviceAdapter<T> implements DeviceAdapter<T>{

    private final String TAG = getClass().getSimpleName();
    private BleCenterManager mBleCenterManager;
    protected BleDeviceListener mBleDeviceListener;
    protected BleDevice mBleDevice;
    private RxBleDevice mInternalRxBleDevice;

    private Subscription mConnectionSubscription;
    private Subscription mNotificationSubscription;
    private Subscription mIndicatorSubscription;
    private Subscription mConnectionStateSubscription;

    protected RxBleConnection mRxBleConnection;

    private BehaviorSubject<RxBleConnection> mConnectionBehaviorSubject = null;
    @Override
    public Subscription enableNotification() {

        UUID[] notificationUUIDs = notificationUUIDs();

        if (notificationUUIDs == null || notificationUUIDs.length == 0) {
            return null;
        }

        Observable<PairData>[] observables = new Observable[notificationUUIDs.length];

        Subscription subscription = mConnectionBehaviorSubject.flatMap(rxBleConnection -> {


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

        Subscription subscription = mConnectionBehaviorSubject.flatMap(rxBleConnection -> {


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

    private volatile boolean mInitiativeDisconnect = false;
    @Override
    public void disconnect() {

        mInitiativeDisconnect = true;

        // TODO: 2016/8/2 should check whether subscription has been unsubscribed?
        if (mConnectionSubscription != null && !mConnectionSubscription.isUnsubscribed()){
            mConnectionSubscription.unsubscribe();
        }
        if (mNotificationSubscription != null && !mNotificationSubscription.isUnsubscribed()){
            mNotificationSubscription.unsubscribe();
        }
        if (mIndicatorSubscription != null && !mIndicatorSubscription.isUnsubscribed()){
            mIndicatorSubscription.unsubscribe();
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
        mConnectionBehaviorSubject = BehaviorSubject.create();
        resetAdapter();
        setupConnectionStateChange();
        setupConnection();
    }

    @Override
    public void resetAdapter() {

    }

    private void setupConnection() {

        boolean enableAutoConnect = mBleCenterManager.getBleConfig().isEnableAutoConnect();
        mConnectionSubscription = mInternalRxBleDevice.establishConnection(mBleCenterManager.getContext(),enableAutoConnect)
                .doOnNext(rxBleConnection -> mRxBleConnection = rxBleConnection)
                .subscribe(mConnectionBehaviorSubject);

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
                .subscribe(rxBleConnectionState ->
                {
                    notifyDeviceStateChange(mBleDevice, BleUtil.convertInternalRXState(rxBleConnectionState));
                    if (mInitiativeDisconnect){
                        mConnectionStateSubscription.unsubscribe();
                        mInitiativeDisconnect = false;
                    }
                },
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

    @Override
    public void writeCharacteristic(UUID uuid, byte[] longData, int maxLengthPerPacket, BleStep step) {
        Observable.create(new SyncOnSubscribe<ByteBuffer, byte[]>() {

            @Override
            protected ByteBuffer generateState() {
                return ByteBuffer.wrap(longData);
            }

            @Override
            protected ByteBuffer next(ByteBuffer state, Observer<? super byte[]> observer) {
                final int chunkLength = Math.min(state.remaining(), maxLengthPerPacket);
                if (chunkLength > 0) {
                    final byte[] bytesChunk = new byte[chunkLength];
                    state.get(bytesChunk);
                    observer.onNext(bytesChunk);
                }
                if (chunkLength == 0 || state.remaining() == 0) {
                    observer.onCompleted();
                }
                return state;
            }
        })
                .doOnSubscribe(() -> notifyInteractUpdate(mBleDevice, new BleStep("Begin step "+step.action,step.rawData,step.data)))
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .flatMap(bytes -> mRxBleConnection.writeCharacteristic(uuid, bytes), 1)
                .doOnNext(bytes -> notifyInteractUpdate(mBleDevice, new BleStep("Begin write data piece to UUID :" + uuid.toString())))
                .subscribe(bytes -> notifyInteractUpdate(mBleDevice, new BleStep("Step "+step.action+" complete",step.rawData,step.data))
                        , throwable -> notifyInteractError(mBleDevice, throwable, new BleStep(String.format("Step write data to UUID: %s error", uuid.toString()))));

    }

    @Override
    public void writeCharacteristic(UUID uuid, byte[] longData, int maxLengthPerPacket, BleStep step, int delay, TimeUnit timeUnit) {
        Observable.create(new SyncOnSubscribe<ByteBuffer, byte[]>() {

            @Override
            protected ByteBuffer generateState() {
                return ByteBuffer.wrap(longData);
            }

            @Override
            protected ByteBuffer next(ByteBuffer state, Observer<? super byte[]> observer) {
                final int chunkLength = Math.min(state.remaining(), maxLengthPerPacket);
                if (chunkLength > 0) {
                    final byte[] bytesChunk = new byte[chunkLength];
                    state.get(bytesChunk);
                    observer.onNext(bytesChunk);
                }
                if (chunkLength == 0 || state.remaining() == 0) {
                    observer.onCompleted();
                }
                return state;
            }
        })
                .doOnSubscribe(() -> notifyInteractUpdate(mBleDevice, new BleStep("Begin step "+step.action,step.rawData,step.data)))
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .flatMap(bytes -> mRxBleConnection.writeCharacteristic(uuid, bytes).delay(delay,timeUnit), 1)
                .doOnNext(bytes -> notifyInteractUpdate(mBleDevice, new BleStep("Begin write data piece to UUID :" + uuid.toString())))
                .subscribe(bytes -> notifyInteractUpdate(mBleDevice, new BleStep("Step "+step.action+" complete",step.rawData,step.data))
                        , throwable -> notifyInteractError(mBleDevice, throwable, new BleStep(String.format("Step write data to UUID: %s error", uuid.toString()))));
    }

    @Override
    public void writeCharacteristic(UUID uuid, byte[] longData, int maxLengthPerPacket) {
        Observable.create(new SyncOnSubscribe<ByteBuffer, byte[]>() {

            @Override
            protected ByteBuffer generateState() {
                return ByteBuffer.wrap(longData);
            }

            @Override
            protected ByteBuffer next(ByteBuffer state, Observer<? super byte[]> observer) {
                final int chunkLength = Math.min(state.remaining(), maxLengthPerPacket);
                if (chunkLength > 0) {
                    final byte[] bytesChunk = new byte[chunkLength];
                    state.get(bytesChunk);
                    observer.onNext(bytesChunk);
                }
                if (chunkLength == 0 || state.remaining() == 0) {
                    observer.onCompleted();
                }
                return state;
            }
        })
                .doOnSubscribe(() -> notifyInteractUpdate(mBleDevice,new BleStep("Begin write long data to UUID :"+uuid.toString())))
                .flatMap(bytes ->  mRxBleConnection.writeCharacteristic(uuid,bytes),1)
                .doOnNext(bytes -> notifyInteractUpdate(mBleDevice,new BleStep("Begin write data piece to UUID :"+uuid.toString())))
                .subscribe(bytes -> notifyInteractUpdate(mBleDevice,new BleStep(String.format("Step write data piece to UUID: %s complete",uuid.toString())))
                        ,throwable -> notifyInteractError(mBleDevice,throwable,new BleStep(String.format("Step write data to UUID: %s error",uuid.toString()))));

    }

    @Override
    public void writeCharacteristic(UUID uuid, byte[] longData, int maxLengthPerPacket, int delay, TimeUnit timeUnit) {
        Observable.create(new SyncOnSubscribe<ByteBuffer, byte[]>() {

            @Override
            protected ByteBuffer generateState() {
                return ByteBuffer.wrap(longData);
            }

            @Override
            protected ByteBuffer next(ByteBuffer state, Observer<? super byte[]> observer) {
                final int chunkLength = Math.min(state.remaining(), maxLengthPerPacket);
                if (chunkLength > 0) {
                    final byte[] bytesChunk = new byte[chunkLength];
                    state.get(bytesChunk);
                    observer.onNext(bytesChunk);
                }
                if (chunkLength == 0 || state.remaining() == 0) {
                    observer.onCompleted();
                }
                return state;
            }
        })
                .doOnSubscribe(() -> notifyInteractUpdate(mBleDevice,new BleStep("Begin write long data to UUID :"+uuid.toString())))
                .flatMap(bytes ->  mRxBleConnection.writeCharacteristic(uuid,bytes).delay(delay,timeUnit),1)
                .doOnNext(bytes -> notifyInteractUpdate(mBleDevice,new BleStep("Begin write data piece to UUID :"+uuid.toString())))
                .subscribe(bytes -> notifyInteractUpdate(mBleDevice,new BleStep(String.format("Step write data piece to UUID: %s complete",uuid.toString())))
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
