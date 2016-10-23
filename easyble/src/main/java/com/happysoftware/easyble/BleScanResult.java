package com.happysoftware.easyble;

import com.polidea.rxandroidble.RxBleScanResult;

/**
 * Created by zxx on 2016/7/16.
 */
public class BleScanResult {

    public BleDevice getBleDevice() {
        return bleDevice;
    }

    public int getRssi() {
        return rssi;
    }

    public byte[] getScanRecord() {
        return scanRecord;
    }

    private final BleDevice bleDevice;
    private final int rssi;
    private final byte[] scanRecord;

    BleScanResult(RxBleScanResult rxBleScanResult){

        bleDevice = new BleDevice(rxBleScanResult.getBleDevice());
        this.rssi = rxBleScanResult.getRssi();
        this.scanRecord = rxBleScanResult.getScanRecord();

    }
}
