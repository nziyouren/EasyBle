package com.happysoftware.easyble;

import java.util.concurrent.TimeUnit;
/**
 * Created by zxx on 2016/8/2.
 */
public final class BleConfig {

    private BleConfig(boolean stopScanAfterConnected, boolean startScanAfterDisconncted, boolean enableAutoConnect) {
        mStopScanAfterConnected = stopScanAfterConnected;
        mStartScanAfterDisconnected = startScanAfterDisconncted;
        mEnableAutoConnect = enableAutoConnect;
    }

    private BleConfig(boolean stopScanAfterConnected, boolean startScanAfterDisconncted, boolean enableAutoConnect, boolean writeDelay, long delay, TimeUnit delayUnit) {
        mStopScanAfterConnected = stopScanAfterConnected;
        mStartScanAfterDisconnected = startScanAfterDisconncted;
        mEnableAutoConnect = enableAutoConnect;
        mWriteDelay = writeDelay;
        mDelay = delay;
        mDelayUnit = delayUnit;
    }

    private boolean mStopScanAfterConnected = false;

    private boolean mStartScanAfterDisconnected = false;

    private boolean mEnableAutoConnect = false;

    private boolean mWriteDelay = false;

    private long mDelay = 0;

    private TimeUnit mDelayUnit = TimeUnit.MILLISECONDS;

    public boolean isWriteDelay() {
        return mWriteDelay;
    }

    public void setWriteDelay(boolean writeDelay) {
        this.mWriteDelay = writeDelay;
    }

    public long getDelay() {
        return mDelay;
    }

    public void setDelay(long delay) {
        this.mDelay = delay;
    }

    public TimeUnit getDelayUnit() {
        return mDelayUnit;
    }

    public void setDelayUnit(TimeUnit delayUnit) {
        this.mDelayUnit = delayUnit;
    }

    public boolean isEnableAutoConnect() {
        return mEnableAutoConnect;
    }

    public void setEnableAutoConnect(boolean enableAutoConnect) {
        mEnableAutoConnect = enableAutoConnect;
    }

    public boolean isStopScanAfterConnected() {
        return mStopScanAfterConnected;
    }

    public void setStopScanAfterConnected(boolean stopScanAfterConnected) {
        mStopScanAfterConnected = stopScanAfterConnected;
    }

    public boolean isStartScanAfterDisconnected() {
        return mStartScanAfterDisconnected;
    }

    public void setStartScanAfterDisconnected(boolean startScanAfterDisconnected) {
        mStartScanAfterDisconnected = startScanAfterDisconnected;
    }


    public static class BleConfigBuilder {
        private boolean mInternalStopScanAfterConnected = false;
        private boolean mInternalStartScanAfterDisconnected = false;
        private boolean mInternalEnableAutoConnect = false;
        private boolean mInternalWriteDelay = false;
        private long mInternalDelay = 0;
        private TimeUnit mInternalDelayTimeUnit = TimeUnit.MILLISECONDS;

        public BleConfigBuilder setStopScanAfterConnected(boolean internalStopScanAfterConnected) {
            mInternalStopScanAfterConnected = internalStopScanAfterConnected;
            return this;
        }

        public BleConfigBuilder setStartScanAfterDisconnected(boolean internalStartScanAfterDisconnected) {
            mInternalStartScanAfterDisconnected = internalStartScanAfterDisconnected;
            return this;
        }

        public BleConfigBuilder setEnableAutoConnect(boolean internalEnableAutoConnect){
            mInternalEnableAutoConnect = internalEnableAutoConnect;
            return this;
        }

        public BleConfigBuilder setWriteDelay(boolean internalWriteDelay){
            mInternalWriteDelay = internalWriteDelay;
            return this;
        }

        public BleConfigBuilder setDelay(long internalDelay){
            mInternalDelay = internalDelay;
            return this;
        }

        public BleConfigBuilder setDelayTimeUnit(TimeUnit internalDelayTimeUnit){
            mInternalDelayTimeUnit = internalDelayTimeUnit;
            return this;
        }

        public BleConfig createBleConfig() {
            return new BleConfig(mInternalStopScanAfterConnected, mInternalStartScanAfterDisconnected, mInternalEnableAutoConnect, mInternalWriteDelay, mInternalDelay, mInternalDelayTimeUnit);
        }

    }

}
