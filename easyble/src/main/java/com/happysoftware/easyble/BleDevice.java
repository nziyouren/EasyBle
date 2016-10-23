package com.happysoftware.easyble;

import com.polidea.rxandroidble.RxBleDevice;

import com.happysoftware.easyble.utils.BleUtil;

/**
 * Created by zxx on 2016/7/16.
 */
public class BleDevice {

    protected RxBleDevice getInternalRxBleDevice() {
        return mRxBleDevice;
    }

    private RxBleDevice mRxBleDevice;

    private String deviceName;

    public String getDeviceMacAddress() {
        return deviceMacAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    private String deviceMacAddress;

    BleDevice(RxBleDevice rxBleDevice){
        this.mRxBleDevice = rxBleDevice;
        deviceName = mRxBleDevice.getName();
        deviceMacAddress = mRxBleDevice.getMacAddress();
    }

    @Override
    public int hashCode() {
        // TODO: 2016/10/15 should change hashcode method in next version
        int result = mRxBleDevice.hashCode();
        result = 31 * result + deviceName.hashCode();
        result = 31 * result + deviceMacAddress.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BleDevice bleDevice = (BleDevice) o;

        if (mRxBleDevice != null ? !mRxBleDevice.equals(bleDevice.mRxBleDevice) : bleDevice.mRxBleDevice != null)
            return false;
        if (deviceName != null ? !deviceName.equals(bleDevice.deviceName) : bleDevice.deviceName != null)
            return false;
        return deviceMacAddress != null ? deviceMacAddress.equals(bleDevice.deviceMacAddress) : bleDevice.deviceMacAddress == null;

    }

    public BleDeviceState getConnectionState(){
        return BleUtil.convertInternalRXState(mRxBleDevice.getConnectionState());
    }

    @Override
    public String toString() {
        return "BleDevice{" +
                "deviceName='" + deviceName + '\'' +
                ", deviceMacAddress='" + deviceMacAddress + '\'' +
                '}';
    }
}
