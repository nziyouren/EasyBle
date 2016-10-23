package com.happysoftware.easyble;

/**
 * Created by zxx on 2016/8/2.
 */
public final class BleConfig {

    private BleConfig(boolean stopScanAfterConnected, boolean startScanAfterDisconncted, boolean enableAutoConnect) {
        mStopScanAfterConnected = stopScanAfterConnected;
        mStartScanAfterDisconncted = startScanAfterDisconncted;
        mEnableAutoConnect = enableAutoConnect;
    }

    private boolean mStopScanAfterConnected = false;

    private boolean mStartScanAfterDisconncted = false;

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

    public boolean isStartScanAfterDisconncted() {
        return mStartScanAfterDisconncted;
    }

    public void setStartScanAfterDisconncted(boolean startScanAfterDisconncted) {
        mStartScanAfterDisconncted = startScanAfterDisconncted;
    }


    public static class BleConfigBuilder {
        private boolean mInternalStopScanAfterConnected = false;
        private boolean mInternalStartScanAfterDisconncted = false;
        private boolean mInternalEnableAutoConnect = false;

        public BleConfigBuilder setStopScanAfterConnected(boolean internalStopScanAfterConnected) {
            mInternalStopScanAfterConnected = internalStopScanAfterConnected;
            return this;
        }

        public BleConfigBuilder setStartScanAfterDisconncted(boolean internalStartScanAfterDisconncted) {
            mInternalStartScanAfterDisconncted = internalStartScanAfterDisconncted;
            return this;
        }

        public BleConfigBuilder setEnableAutoConnect(boolean internalEnableAutoConnect){
            mInternalEnableAutoConnect = internalEnableAutoConnect;
            return this;
        }

        public BleConfig createBleConfig() {
            return new BleConfig(mInternalStopScanAfterConnected, mInternalStartScanAfterDisconncted, mInternalEnableAutoConnect);
        }

    }

}
