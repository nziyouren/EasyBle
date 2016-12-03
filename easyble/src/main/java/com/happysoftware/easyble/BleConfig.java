package com.happysoftware.easyble;

/**
 * Created by zxx on 2016/8/2.
 */
public final class BleConfig {

    private BleConfig(boolean stopScanAfterConnected, boolean startScanAfterDisconncted, boolean enableAutoConnect) {
        mStopScanAfterConnected = stopScanAfterConnected;
        mStartScanAfterDisconnected = startScanAfterDisconncted;
        mEnableAutoConnect = enableAutoConnect;
    }

    private boolean mStopScanAfterConnected = false;

    private boolean mStartScanAfterDisconnected = false;

    private boolean mEnableAutoConnect = false;

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

        public BleConfig createBleConfig() {
            return new BleConfig(mInternalStopScanAfterConnected, mInternalStartScanAfterDisconnected, mInternalEnableAutoConnect);
        }

    }

}
