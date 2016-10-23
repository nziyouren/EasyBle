package com.happysoftware.easyble;

/**
 * Created by zxx on 2016/7/16.
 */
public interface BleDeviceListener {

    void onDeviceStateChange(BleDevice device, BleDeviceState state);

    void onDataComing(BleDevice device, BleDataType type, Object data);

    void onInteractComplete(BleDevice device, Object finalResult);

    void onInteractUpdate(BleDevice device, BleStep step);

    void onInteractError(BleDevice device, Throwable throwable, BleStep step);

    void onScanUpdate(BleScanResult scanResult);

    void onScanError(Throwable throwable);
}
