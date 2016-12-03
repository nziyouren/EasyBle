package com.happysoftware.easyble;

import com.happysoftware.easyble.exception.EasyBleException;

import java.util.UUID;

import rx.Subscription;

/**
 * Created by zxx on 2016/7/16.
 */
public interface DeviceAdapter<T> {

    Subscription enableNotification();

    Subscription enableIndicator();

    void processData(UUID uuid, byte[] data);

    // TODO: 2016/12/3 need to remove parseData
    T parseData(UUID uuid, byte[] data);

    UUID[] notificationUUIDs();

    UUID[] indicatorUUIDs();

    String[] supportedNames();

    String[] supportedNameRegExps();
    void connectThenStart(BleDevice bleDevice);

    void disconnect();

    void resetAdapter();

    void writeCharacteristic(UUID uuid, byte[] data, BleStep step);

    void writeCharacteristic(UUID uuid, byte[] data);

    void readCharacteristic(UUID uuid, BleStep step);

    void readCharacteristic(UUID uuid);

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
