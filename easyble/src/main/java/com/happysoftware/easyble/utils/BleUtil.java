package com.happysoftware.easyble.utils;

import com.polidea.rxandroidble.RxBleConnection;

import com.happysoftware.easyble.BleDeviceState;

/**
 * Created by zxx on 2016/7/19.
 */
public final class BleUtil {

    public static BleDeviceState convertInternalRXState(RxBleConnection.RxBleConnectionState state){

        if (state.equals(RxBleConnection.RxBleConnectionState.CONNECTED)){
            return BleDeviceState.BLE_DEVICE_STATE_CONNECTED;
        }else if (state.equals(RxBleConnection.RxBleConnectionState.CONNECTING)){
            return BleDeviceState.BLE_DEVICE_STATE_CONNECTING;
        }else if (state.equals(RxBleConnection.RxBleConnectionState.DISCONNECTED)){
            return BleDeviceState.BLE_DEVICE_STATE_DISCONNECTED;
        }else if (state.equals(RxBleConnection.RxBleConnectionState.DISCONNECTING)){
            return BleDeviceState.BLE_DEVICE_STATE_DISCONNECTING;
        }

        //should not happen here
        return BleDeviceState.BLE_DEVICE_STATE_DISCONNECTED;
    }

}
