# EasyBle
Multi device process Ble library for Android

中文文档 [README_CN.md](https://github.com/nziyouren/EasyBle/blob/master/README_CN.md)


# Why I develop this library?
## Current Ble difficulties

* When app interacts with multiple devices, App UI may tightly coupled with bluetooth interact logic. It's difficult to maintain
* BLE command must executed one by one. If not, many strange issues may happen such as packet drop, gatt 133
* Devices made by different manufacturers have differences in detailed Ble stack implementations. We need to deal with these situations specially. Such as on Samsung device, characteristic write operations must be executed on Android main thread.
* Some peripheral devices can't consume lots of data in a very short time, so we need to control send speed in producer side.

So I develop this library to solve above issues.

## EasyBle advantages

* Decouple App UI and bluetooth interact logic totally
* Simple API. App just need to register one listener to listen all bluetooth events
* Based on Powerful [RXAndroidBle](https://github.com/Polidea/RxAndroidBle) (https://github.com/Polidea/RxAndroidBle). Improve stablities a lot, easy thread switch, clean code.

## Architecture

Architecture of this library

<br /> 
<img src="http://oiburv3oy.bkt.clouddn.com/framework.png" alt="Drawing"/>

<br /> 

# Download
## Gradle

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
```
	if (bleCenterManager.isBluetoothOpen()){
    try {
         bleCenterManager.prepareClose();//disconnect device
 		   bleCenterManager.closeBluetooth();//close Bluetooth
 		 } catch (EasyBleException e) {
        e.printStackTrace();
 		}
	}
```


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
	
	
## How to integrate new device
#### You just need 3 steps to integrate new device:

1. Create custom AdapterFactory and implement buildDeviceAdapter method

	```	
	public class BloodPressureFactory extends DeviceAdapter.Factory{

	 //static create Factory method
    public static BloodPressureFactory create(BleCenterManager bleCenterManager){				
	        return new BloodPressureFactory(bleCenterManager);
	 }
	
    public BloodPressureFactory(BleCenterManager bleCenterManager) {
	        super(bleCenterManager);
    }

	 //Create deviceAdapter
    @Override
    public DeviceAdapter<T> buildDeviceAdapter() {
        return new BloodPressureDeviceAdapter(mBleCenterManager);
	}
	}
	``` 
2.  Create your custom deviceAdapter inherited from DeviceAdatper and implements some methods
    
    ##### public String[] supportedNames() 

	EasyBle find appropriate deviceAdapter to process your device by name. So your deviceAdapter need to tell EasyBle framework the device name you can process. If your deviceAdapter supports more than one device, return all names. If you'r not sure about the name, you can use supportedNameRegExps() instead. supportedNameRegExps() support regular expressions.
	
	```
	@Override
	public String[] supportedNames() {
	    return new String[]{"Model-name-old","Model-name-new"};//return device advertised names
	}
	```

    ##### public UUID[] notificationUUIDs()
	
	If you want to set UUID notifications, just return UUID[]

	```
	@Override
	public UUID[] notificationUUIDs() {
    	return new UUID[]{UUID.fromString("UUID-1"),UUID.fromString("UUID-2")};// return UUIDs which need to enable notification
	}
	```

    ##### public UUID[] indicatorUUIDs()
	
	If you want to set UUID indications, just return UUID[]
	
	```
	@Override
	public UUID[] indicatorUUIDs() {
    return new UUID[]{UUID.fromString("UUID-1"),UUID.fromString("UUID-2")};//return UUIDs which need to enable indication
	}
	```
    ##### public void processData(UUID uuid, byte[] data) 
	
	Put your peripherals and device protocol logic here. The second parameter data is the data you received from peripherals. You can parse, process and send some other data to peripherals. 

	```
	@Override
	public void processData(UUID uuid, byte[] data) {

		//Process data you received from peripherals.
		byte cmd = data[0];
        if (cmd == 1){
            //Notify APP UI, we've reached step1. NotifyInteractUpdate is wrapper of BleDeviceListener.onInteractUpdate.
            notifyInteractUpdate(mBleDevice,new BleStep("Interact update on step1",data));
        }else if (cmd == 2){
            //Notify APP UI, we've reached step2
            notifyInteractUpdate(mBleDevice,new BleStep("Interact update on step2",data));
        }else if (cmd == 3){
            //Notify APP UI, we've reached step3
            notifyInteractUpdate(mBleDevice,new BleStep("Interact update on step3",data));
            notifyDataComing(mBleDevice,BleDataType.BLE_DATA_TPYE_CONTINUOUS,data);
        }else if (cmd == 4){
            //Notify APP UI, we've finished protocol, and pass data to APP UI through callback method. So APP UI can parse data and update UI
            notifyInteractComplete(mBleDevice,data);
        }

	}
	```
    #### What we need to do in processData(UUID uuid, byte[] data) method?
	
	**Because different peripheral equipment has different protocols, so in every key step of protocols interaction, we need to invoke according BleDeviceListener method manually. As a result the APP UI(Activity) can receive BLE event callback and process received data.**
    
    	
3. 	Register custom device factory to BLECenterManager.

	```
    bleCenterManager.addDeviceAdapterFactory(BloodPressureFactory.create(bleCenterManager));
	```

	Wow! Now EasyBle can process and interact with your new peripheral equipment.

	


# Contact
You can reach me by email nziyouren@gmail.com

# License:
Apache License, Version 2.0


