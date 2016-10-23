package com.happysoftware.easyble.exception;

/**
 * Created by zhang on 16/7/24.
 */

public class EasyBleScanException extends EasyBleException {

    private com.polidea.rxandroidble.exceptions.BleScanException mInternalScanException;

    public EasyBleScanException(com.polidea.rxandroidble.exceptions.BleScanException mInternalScanException) {
        this.mInternalScanException = mInternalScanException;
    }

    public int getReason() {
        return mInternalScanException.getReason();
    }
}
