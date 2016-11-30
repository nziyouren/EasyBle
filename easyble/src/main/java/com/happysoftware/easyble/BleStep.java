package com.happysoftware.easyble;

import java.util.Arrays;

/**
 * Created by zxx on 2016/7/16.
 */
public class BleStep {

    public String action;
    public byte[] rawData;
    public Object data;

    public BleStep(BleStep step){
        this.action = step.action;
        this.rawData = step.rawData.clone();
        this.data = step.data;
    }

    public BleStep(String action, byte[] rawData, Object data) {
        this.action = action;
        this.rawData = rawData;
        this.data = data;
    }

    public BleStep(String action, byte[] rawData) {
        this.action = action;
        this.rawData = rawData;
    }

    public BleStep(String action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return "BleStep{" +
                "action='" + action + '\'' +
                ", rawData=" + Arrays.toString(rawData) +
                ", data=" + data +
                '}';
    }
}
