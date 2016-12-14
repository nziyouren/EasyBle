# EasyBle
Multi device process Ble library for Android


# Why I develop this library?
##Current Ble difficulties

* When app interacts with multiple devices, App UI may tightly coupled with bluetooth interact logic. It's difficult to maintain
* BLE command must executed one by one. If not, many strange issues may happen such as packet drop, gatt 133
* Devices made by different manufacturers have differences in detailed Ble stack implementations. We need to deal with these situations specially. Such as on Samsung device, characteristic write operations must be executed on Android main thread.
* Some peripheral devices can't consume lots of data in a very short time, so we need to control send speed in producer side.

So I develop this library to solve above issues.

## EasyBle advantages

* Decouple App UI and bluetooth interact logic totally
* Simple API. App just need to register one listener to listen all bluetooth events
* Based on Powerful [RXAndroidBle](https://github.com/Polidea/RxAndroidBle) (https://github.com/Polidea/RxAndroidBle). Improve stablities a lot, easy thread switch, clean code.



#Download
##Gradle

    compile 'com.happysoftware.easyble:easyble:0.3.3'

# Sample

## Bluetooth init

	BleCenterManager bleCenterManager = BleCenterManager.getInstance();//APP keep singleton
	BleConfig config = new BleConfig.BleConfigBuilder()
        .setStopScanAfterConnected(true)
        .setStartScanAfterDisconncted(true)
        .createBleConfig();//create config
	bleCenterManager.init(this,config);//init config
	
## Open Bluetooth
	try {
    bleCenterManager.openBluetooth(this, requestCode);
	} catch (EasyBleException e) {
    e.printStackTrace();
	}

## Disconnect device and close Bluetooth
	if (bleCenterManager.isBluetoothOpen()){
    try {
        bleCenterManager.prepareClose();//disconnect device
 		 bleCenterManager.closeBluetooth();//close Bluetooth
 		 } catch (EasyBleException e) {
        e.printStackTrace();
 		}
	}


## Scan Device
	bleCenterManager.startScan();
	
## Stop scan
	bleCenterManager.stopScan();


## Set listener and listen all Bluetooth events

```
bleCenterManager.setBleDeviceListener(new BleDeviceListener() {
    @Override
 public void onDeviceStateChange(BleDevice device, BleDeviceState state) {
        //Device state change callback. BLE_DEVICE_STATE_CONNECTED(1),
 		// BLE_DEVICE_STATE_CONNECTING(2),
 		// BLE_DEVICE_STATE_DISCONNECTED(0),
 		// BLE_DEVICE_STATE_DISCONNECTING(3);
 		if (state == BleDeviceState.BLE_DEVICE_STATE_CONNECTED){

        }
        if (state == BleDeviceState.BLE_DEVICE_STATE_DISCONNECTED){

        }
    }

    @Override
 public void onDataComing(BleDevice device, BleDataType type, Object data) {
        //data coming callback
		 Log.e(TAG,"onDataComing "+type+ "rawData: "+data.toString());
 		if (type == BleDataType.BLE_DATA_TPYE_CONTINUOUS){
            //data type is BleDataType.BLE_DATA_TPYE_CONTINUOUS
 		}else (type == BleDataType.BLE_DATA_TPYE_SINGLE){
            //data type is BleDataType.BLE_DATA_TPYE_SINGLE
 		}
    }

    @Override
 public void onInteractComplete(BleDevice device, Object finalResult) {
        //interact complete callback
 		Log.e(TAG,"onInteractComplete "+device.getDeviceName()+ "state: "+finalResult);
 }

    @Override
 public void onInteractUpdate(BleDevice device, BleStep step) {
        //interact update callback
 		Log.e(TAG,"onInteractUpdate "+device.getDeviceName()+ " step: "+step.action);
 }

    @Override
 public void onInteractError(BleDevice device, Throwable throwable, BleStep step) {
        //interact error callback
 }
 
 @Override
 public void onScanStart(){
 	 	//scan start callback
  }

@Override
public void onScanStop(){
		//scan stop callback
}


    @Override
 public void onScanUpdate(BleScanResult scanResult) {
        //scan device callback

 }

    @Override
 public void onScanError(Throwable throwable) {
        //scan error callback
}
});

```

## Write data to characteristic
	public void writeCharacteristic(UUID uuid, byte[] data);
	
## Write lots of data to characteristic and chunkify data automatically
	public void writeCharacteristic(UUID uuid, byte[] longData, int maxLengthPerPacket);
	
## Read data from characteristic
	public void readCharacteristic(UUID uuid);
	
	
## More detailed sample
	Coming soon...


#Contact
You can reach me by email nziyouren@gmail.com

#License:
Apache License, Version 2.0
