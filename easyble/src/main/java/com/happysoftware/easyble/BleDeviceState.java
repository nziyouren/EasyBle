package com.happysoftware.easyble;

/**
 * Created by zhang on 16/7/17.
 */

public enum BleDeviceState {

    BLE_DEVICE_STATE_CONNECTED(1),
    BLE_DEVICE_STATE_CONNECTING(2),
    BLE_DEVICE_STATE_DISCONNECTED(0),
    BLE_DEVICE_STATE_DISCONNECTING(3);

    private int mState;

    private BleDeviceState(int state) {
        mState = state;
    }
}
