package com.happysoftware.easyble;

import java.util.UUID;

import com.happysoftware.easyble.exception.EasyBleException;
import rx.Subscription;

/**
 * Created by zxx on 2016/7/16.
 */
public interface DeviceAdapter<T> {

    Subscription enableNotification();

    Subscription enableIndicator();

    void processData(UUID uuid, byte[] data);

    T parseData(UUID uuid, byte[] data);

    UUID[] notificationUUIDs();

    UUID[] indicatorUUIDs();

    String[] supportedNames();

    String[] supportedNameRegExps();
    void connectThenStart(BleDevice bleDevice);

    void disconnect();

    void resetAdapter();
    public void writeCharacteristic(UUID uuid, byte[] data, BleStep step);

    public void writeCharacteristic(UUID uuid, byte[] data);

    public void readCharacteristic(UUID uuid, BleStep step);

    public void readCharacteristic(UUID uuid);

    void executeCmd(int cmd) throws EasyBleException;

    abstract class Factory{

        protected BleCenterManager mBleCenterManager;

        public Factory(BleCenterManager bleCenterManager) {
            mBleCenterManager = bleCenterManager;
        }

        public abstract DeviceAdapter<?> buildDeviceAdapter();

        @Override
        public String toString() {
            return "Factory{}"+getClass().getName();
        }
    }
}
