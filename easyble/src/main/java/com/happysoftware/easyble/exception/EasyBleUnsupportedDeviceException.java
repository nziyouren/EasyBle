package com.happysoftware.easyble.exception;

import com.happysoftware.easyble.BleDevice;

/**
 * Created by zhang on 16/7/24.
 */

public class EasyBleUnsupportedDeviceException extends EasyBleException {

    public EasyBleUnsupportedDeviceException(String detailMessage) {
        super(detailMessage);
    }

    public EasyBleUnsupportedDeviceException(BleDevice device){
        this("UnsupportedDevice : "+device.getDeviceName());
    }
}
