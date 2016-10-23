package com.happysoftware.easyble.utils;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by zhang on 16/7/18.
 */

public class ByteUtil {


    /**
     * Returns the size of a give value type.
     */
    public static int getTypeLen(int formatType) {
        return formatType & 0xF;
    }

    /**
     * Convert a signed byte to an unsigned int.
     */
    public static int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }

    /**
     * Convert signed bytes to a 16-bit unsigned int.
     */
    public static int unsignedBytesToInt(byte b0, byte b1) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8));
    }

    /**
     * Convert signed bytes to a 32-bit unsigned int.
     */
    public static int unsignedBytesToInt(byte b0, byte b1, byte b2, byte b3) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8))
                + (unsignedByteToInt(b2) << 16) + (unsignedByteToInt(b3) << 24);
    }

    /**
     * Convert an unsigned integer value to a two's-complement encoded
     * signed value.
     */
    public static int unsignedToSigned(int unsigned, int size) {
        if ((unsigned & (1 << size-1)) != 0) {
            unsigned = -1 * ((1 << size-1) - (unsigned & ((1 << size-1) - 1)));
        }
        return unsigned;
    }


    /**
     * Convert signed bytes to a 16-bit short float value.
     */
    public static float bytesToFloat(byte b0, byte b1) {
        int mantissa = unsignedToSigned(unsignedByteToInt(b0)
                + ((unsignedByteToInt(b1) & 0x0F) << 8), 12);
        int exponent = unsignedToSigned(unsignedByteToInt(b1) >> 4, 4);
        return (float)(mantissa * Math.pow(10, exponent));
    }

    /**
     * Convert signed bytes to a 32-bit short float value.
     */
    public static float bytesToFloat(byte b0, byte b1, byte b2, byte b3) {
        int mantissa = unsignedToSigned(unsignedByteToInt(b0)
                + (unsignedByteToInt(b1) << 8)
                + (unsignedByteToInt(b2) << 16), 24);
        return (float)(mantissa * Math.pow(10, b3));
    }

    
    /**
     * Return the stored value of this characteristic.
     *
     * <p>The formatType parameter determines how the characteristic value
     * is to be interpreted. For example, settting formatType to
     * {@link #FORMAT_UINT16} specifies that the first two bytes of the
     * characteristic value at the given offset are interpreted to generate the
     * return value.
     *
     * @param formatType The format type used to interpret the characteristic
     *                   value.
     * @param offset Offset at which the integer value can be found.
     * @return Cached value of the characteristic or null of offset exceeds
     *         value size.
     */
    public static Integer getIntValue(byte[] data, int formatType, int offset) {
        if ((offset + getTypeLen(formatType)) > data.length) return null;

        switch (formatType) {
            case BluetoothGattCharacteristic.FORMAT_UINT8:
                return unsignedByteToInt(data[offset]);

            case BluetoothGattCharacteristic.FORMAT_UINT16:
                return unsignedBytesToInt(data[offset], data[offset+1]);

            case BluetoothGattCharacteristic.FORMAT_UINT32:
                return unsignedBytesToInt(data[offset],   data[offset+1],
                        data[offset+2], data[offset+3]);
            case BluetoothGattCharacteristic.FORMAT_SINT8:
                return unsignedToSigned(unsignedByteToInt(data[offset]), 8);

            case BluetoothGattCharacteristic.FORMAT_SINT16:
                return unsignedToSigned(unsignedBytesToInt(data[offset],
                        data[offset+1]), 16);

            case BluetoothGattCharacteristic.FORMAT_SINT32:
                return unsignedToSigned(unsignedBytesToInt(data[offset],
                        data[offset+1], data[offset+2], data[offset+3]), 32);
        }

        return null;
    }


    /**
     * Return the stored value of this characteristic.
     * <p>See {@link #getValue} for details.
     *
     * @param formatType The format type used to interpret the characteristic
     *                   value.
     * @param offset Offset at which the float value can be found.
     * @return Cached value of the characteristic at a given offset or null
     *         if the requested offset exceeds the value size.
     */
    public static Float getFloatValue(byte[] data, int formatType, int offset) {
        if ((offset + getTypeLen(formatType)) > data.length) return null;

        switch (formatType) {
            case BluetoothGattCharacteristic.FORMAT_SFLOAT:
                return bytesToFloat(data[offset], data[offset+1]);

            case BluetoothGattCharacteristic.FORMAT_FLOAT:
                return bytesToFloat(data[offset],   data[offset+1],
                        data[offset+2], data[offset+3]);
        }

        return null;
    }


}
