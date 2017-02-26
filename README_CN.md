# EasyBle-Android蓝牙BLE集成多设备库实践


## 背景

公司开发了一款健康类APP，用户可以通过APP连接外部蓝牙BLE设备采集血糖，血压，体重等多个常见健康类指标。因此APP需要同时集成多款设备（多个品牌的血糖仪，血压计，体脂秤）。众所周知，Android系统上的蓝牙BLE是个大坑，存在很多问题，除了系统本身的bug外，各个手机厂商针对蓝牙这块也都做了定制，修改了标准系统蓝牙的一些行为。

### 常见问题

在开发过程中遇到的常见问题和难点，总结如下：

1. 同时集成多个设备场景下，跟外部设备协议交互的逻辑代码容易跟UI界面紧耦合，代码维护成本较高。
2. BLE操作指令需要严格按照队列排队处理，如果不处理的话，蓝牙会存在丢包，GATT 133等问题。
3. 不同手机型号对蓝牙协议栈实现有区别，有时候需要针对特别不同手机做特殊处理。比如三星手机characteristic 写数据的时候必须在UI主线程。
4. 有些外部设备对于短时间过快的数据发送会消化不了，因此在APP端需要进行发送速度的控制。

### 解决方法

针对上述问题，在[RXAndroidBle](https://github.com/Polidea/RxAndroidBle) (https://github.com/Polidea/RxAndroidBle) 的基础上设计并开发了EasyBle蓝牙库，解决上述问题。

EasyBle优点：

1. **专门为集成多款设备场景设计**，设备间协议交互与UI界面彻底解耦。通过Adapter扩展APP集成外部设备的能力。
2. 精简的API设计，APP只需注册一个BleDeviceListener即可得到蓝牙所有操作的回调，并进行相应处理。
3. 得益于强大的RXAndroidBle底层库，对外接设备的读写稳定性得到大幅提高。 因为RxAndroidBle使用RX实现，因此线程切换也变得很简单，无需多余的胶水代码。

<br /> 

## 整体设计

首先看下整体架构图

<br /> 
<img src="http://oiburv3oy.bkt.clouddn.com/framework.png" alt="Drawing"/>

<br /> 

### 重要模块

#### BleCenterManager:
核心管理类，主要进行蓝牙状态管理，监听器管理，及集成设备能力的管理，作为单例使用。

BleCenterManager公开了用户常用的蓝牙操作API：

* 打开蓝牙，关闭蓝牙
* 扫描设备，停止扫描
* 对接设备adapter的注册及删除
* 连接并处理外部设备，断开设备
* 设备状态监听

#### DeviceAdapter:
设备协议处理接口，这个是除了BleCenterManager外最重要的一个interface。

一个合格且完整的DeviceAdater，包含了以下几方面信息：

1. **它能处理哪些设备？**
2. **它该如何与外部设备进行协议交互及数据处理？**

DeviceAdapter 主要方法如下：

* UUID[] notificationUUIDs()   //需要设置notification的UUID数组

* UUID[] indicatorUUIDs()		  //需要设置indicator的UUID数组

* String[] supportedNames()    //支持设备名称列表，通过设备名精确匹配

* String[] supportedNameRegExps() //支持设备名称列表，正则表达式匹配

* void connectThenStart(BleDevice bleDevice) //连接设备并进行协议交互

* void disconnect()			//断开设备

* void writeCharacteristic(UUID uuid, byte[] data) //向指定UUID的Characteristic写入数据

* void writeCharacteristic(UUID uuid, byte[] longData, int maxLengthPerPacket) //向指定UUID的Characteristic分包写入数据，maxLengthPerPacket参数为每包的字节数

* void writeCharacteristic(UUID uuid, byte[] longData, int maxLengthPerPacket, int delay, TimeUnit timeUnit) //功能同上，可以设置每个数据包之间的写入时间间隔 delay 时间间隔， timeUnit 为时间类型

* void readCharacteristic(UUID uuid) //从指定UUID的Characteristic中读取数据

* void executeCmd(int cmd) throws EasyBleException //执行命令接口

* abstract class Factory DeviceAdapterFactory类，用户可以继承这个工厂类，自由实现创建DeviceAdapter的方法

#### BleDeviceListener：
BLE设备监听器，APP只需要向BleCenterManager注册一个BleDeviceListener，就可以得到蓝牙状态及所有协议交互过程中的回调，并进行相应处理。

BleDeviceListener 主要方法：

    void onDeviceStateChange(BleDevice device, BleDeviceState state);
    设备状态变化回调

    void onDataComing(BleDevice device, BleDataType type, Object data);
    数据上报接口回调

    void onInteractComplete(BleDevice device, Object finalResult);
    协议交互完毕回调

    void onInteractUpdate(BleDevice device, BleStep step);
    协议交互更新回调

    void onInteractError(BleDevice device, Throwable throwable, BleStep step);
    协议交互错误回调

    void onScanStart();
    扫描开始回调

    void onScanStop();
    扫描停止回调

    void onScanUpdate(BleScanResult scanResult);
    扫描设备更新回调

    void onScanError(Throwable throwable);
    扫描错误回调
    

#### BleScanResult
 BLE扫描结果类，通过这个类的get方法，我们可以获取扫描到设备的Rssi值，设备名等信息

<br /> 


## 常用使用场景及代码示例

### 蓝牙初始化
```
BleCenterManager bleCenterManager = BleCenterManager.getInstance();//APP自己维护单例
BleConfig config = new BleConfig.BleConfigBuilder()
        .setStopScanAfterConnected(true)
        .setStartScanAfterDisconncted(true)
        .createBleConfig();//创建蓝牙配置
bleCenterManager.init(this,config);//初始化配置
```


### 打开蓝牙
```
try {
    bleCenterManager.openBluetooth(this, requestCode);
} catch (EasyBleException e) {
    e.printStackTrace();
}
```



### 断开设备连接，关闭蓝牙
```
if (bleCenterManager.isBluetoothOpen()){
    try {
		bleCenterManager.prepareClose();//断开正在连接的设备
		bleCenterManager.closeBluetooth();//关闭蓝牙
 } catch (EasyBleException e) {
        e.printStackTrace();
 }
}
```


### 开始扫描
```
bleCenterManager.startScan();
```

### 停止扫描
```
bleCenterManager.stopScan();
```


### 注册蓝牙监听器，回调方法处理
```
bleCenterManager.setBleDeviceListener(new BleDeviceListener() {
 @Override
 public void onDeviceStateChange(BleDevice device, BleDeviceState state) {
        //设备状态变化时回调，一共四种状态BLE_DEVICE_STATE_CONNECTED(1),
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
        //蓝牙设备数据主动上报回调
        Log.i(TAG,"onDataComing "+type+ "rawData: "+data.toString());
        if (type == BleDataType.BLE_DATA_TPYE_CONTINUOUS){
            //蓝牙回调数据类型为中间连续数据
 		}else (type == BleDataType.BLE_DATA_TPYE_SINGLE){
            //蓝牙回调数据类型为单个或最终数据
 		}
    }

 @Override
 public void onInteractComplete(BleDevice device, Object finalResult) {
        //蓝牙与手机协议交互完成回调，finalResult为设备上报的最终值
 		Log.i(TAG,"onInteractComplete "+device.getDeviceName()+ "state: "+finalResult);
 }

 @Override
 public void onInteractUpdate(BleDevice device, BleStep step) {
        //蓝牙与手机协议交互更新回调
 		Log.i(TAG,"onInteractUpdate "+device.getDeviceName()+ " step: "+step.action);
 }

 @Override
 public void onInteractError(BleDevice device, Throwable throwable, BleStep step) {
        //蓝牙与手机协议交互错误回调
 }
 
 @Override
 public void onScanStart(){
 		 //扫描启动
  }

@Override
public  void onScanStop(){
		//扫描停止
}


    @Override
 public void onScanUpdate(BleScanResult scanResult) {
        //扫描到蓝牙设备，更新到UI

 }

    @Override
 public void onScanError(Throwable throwable) {
        //扫描错误，进行后续错误处理
 if (throwable instanceof EasyBleScanException) {
            handleBleScanException((EasyBleScanException) throwable);
 }
    }
});
```


### 写入数据到指定的UUID
```
public void writeCharacteristic(UUID uuid, byte[] data);
```

### 分片写入大批量数据到指定的UUID，maxLengthPerPacket参数为每片数据包长度
```
public void writeCharacteristic(UUID uuid, byte[] longData, int maxLengthPerPacket);
```

### 从指定UUID读取数据，读取的数据通过BleDeviceListener的onDataComing回调方法上报。
```
public void readCharacteristic(UUID uuid);
```

<br /> 

## 如何集成新设备

快速集成新的设备，只需要三个步骤

1.	创建自定义Adapter Factory，并实现创建Adapter的相关方法。

	```	
	public class BloodPressureFactory extends DeviceAdapter.Factory{

	 //静态创建Factory方法
    public static BloodPressureFactory create(BleCenterManager bleCenterManager){				
	        return new BloodPressureFactory(bleCenterManager);
	 }
	
    public BloodPressureFactory(BleCenterManager bleCenterManager) {
	        super(bleCenterManager);
    }

	 //创建DeviceAdapter
    @Override
    public DeviceAdapter<T> buildDeviceAdapter() {
        return new BloodPressureDeviceAdapter(mBleCenterManager);
	}
	}
	```

2. 继承DefaultDeviceAdapter，并且实现Adapter的相关方法

	supportedNames() 

	DeviceAdapter需要告诉框架，它能处理哪些设备，对于设备的识别，通过设备名字来进行。如果设备新旧不同型号，有多个名字呢？没关系返回数组即可。注意：这个方法返回的设备名字是需要精确匹配的。如果需要模糊匹配的话请使用supportedNameRegExps()方法，支持正则表达式。
	
	```
	@Override
	public String[] supportedNames() {
	    return new String[]{"Model-name-old","Model-name-new"};//返回设备名字
	}
	```

	notificationUUIDs()
	
	对于需要设定notification的UUID只需要通过enableNotificationUUIDs（）方法返回UUID数组即可

	```
	@Override
	public UUID[] notificationUUIDs() {
    	return new UUID[]{UUID.fromString("UUID-1"),UUID.fromString("UUID-2")};// 返回需启用通知的UUID
	}
	```

	indicatorUUIDs()
	
	对于需要设定indicator的UUID只需要通过enableIndicatorUUIDs（）方法返回UUID数组即可
	
	```
	@Override
	public UUID[] indicatorUUIDs() {
    return new UUID[]{UUID.fromString("UUID-1"),UUID.fromString("UUID-2")};//返回启用indicator的UUID
	}
	```
	processData()方法

	```
	@Override
	public void processData(UUID uuid, byte[] data) {

		//处理设备上报的数据,data即为设备发送给APP的数据
		byte cmd = data[0];
        if (cmd == 1){
            //通知APP UI 协议交互步骤更新到步骤1
            notifyInteractUpdate(mBleDevice,new BleStep("Interact update on step1",data));
        }else if (cmd == 2){
            //通知APP UI 协议交互步骤更新到步骤2
            notifyInteractUpdate(mBleDevice,new BleStep("Interact update on step2",data));
        }else if (cmd == 3){
            //通知APP UI 协议交互步骤更新到步骤3，且通知UI外部设备已经给我们发送数据data
            notifyInteractUpdate(mBleDevice,new BleStep("Interact update on step3",data));
            notifyDataComing(mBleDevice,BleDataType.BLE_DATA_TPYE_CONTINUOUS,data);
        }else if (cmd == 4){
            //通知APP UI 协议交互步骤完成，最终数据为data
            notifyInteractComplete(mBleDevice,data);
        }

	}
	```
	**对processData()方法实现的约束：**
	
	**因为每个外接蓝牙设备协议不同，所以协议交互过程的数据上传，交互更新，交互完毕关键时间点需要开发人员调用，这样BleCenterManager的关键过程回掉才能被调用，然后被前台UI感知并进行相应处理。**


3. 注册刚刚自定义的factory到BLECenterManager

	```
bleCenterManager.addDeviceAdapterFactory(BloodPressureFactory.create(bleCenterManager));
	```


	OK！现在EasyBle已经具有处理指定新设备的能力了！是不是很简单？
	
<br /> 

## 核心解析
上面讲述了常见的使用场景及代码示例，下面我们来分析这个库最核心的一部分：EasyBle是如何让设备间协议交互与UI界面解耦的？

秘诀就是前面出现过多次的DeviceAdapter。DeviceAdapter提供集成外部设备并与外部设备通信的能力。BleCenterManager内部维护一个Factory列表，由Factory创建DeviceAdapter，并同样以列表的形式保存在BleCenterManager内部。当我们在APP应用层面，调用BleCenterManager的connectThenStart连接并处理设备的时候，BleCenterManager会首先判断注册的Factory列表是否为空，如果为空直接抛出Factory empty异常。如果不为空，则从缓存的adapter列表，查找能处理该设备的adapter，并调用adapter的connectThenStart(BleDevice bleDevice)进行处理，如果找不到则抛出EasyBleUnsupportedDeviceException异常。

前面已经提到adapter自身能够处理哪种设备是靠设备名判断的。文件名匹配支持两种维度，一种是精确匹配，另外一种是正则表达式匹配，分别通过supportedNames()和supportedNameRegExps()返回。查找过程先遍历supportedNames()返回的名称列表进行精确匹配，如果匹配不成功，再遍历supportedNameRegExps()返回的正则表达式列表进行模糊匹配，只要匹配到一个就返回。

我们看下源代码：
BleCenterManager连接设备方法

	public void connectThenStart(BleDevice device) throws EasyBleException {

        DeviceAdapter appropriateDeviceAdapter= findAppropriateDeviceAdapter(device);
        if (appropriateDeviceAdapter != null){
            appropriateDeviceAdapter.connectThenStart(device);
            boundDeviceWithAdapter(device,appropriateDeviceAdapter);
        }else {

        }

    }


查找Adapter方法

```	
private DeviceAdapter findAppropriateDeviceAdapter(BleDevice bleDevice) throws EasyBleException {
		  //先判断factory是否为空
        if (mDeviceAdapterFactories == null || mDeviceAdapterFactories.isEmpty()){
            throw new EasyBleException("Device adapter factories empty!");
        }
 		 //遍历adapter列表
        for (DeviceAdapter adapter:mDeviceAdapters){
            String[] nameList = adapter.supportedNames();
            if (nameList != null && nameList.length > 0){
                for (String name:nameList){
                    if (bleDevice.getDeviceName().equalsIgnoreCase(name)){
                        return adapter;
                    }
                }
            }
            String[] nameRegExpList = adapter.supportedNameRegExps();
            if (nameRegExpList != null && nameRegExpList.length >0){
                for (String nameRegExp:nameRegExpList){
                    if (Pattern.matches(nameRegExp,bleDevice.getDeviceName())){
                        return adapter;
                    }
                }
            }

        }

        throw new EasyBleUnsupportedDeviceException(bleDevice);

    }
```

流程图如下：

<br /> 
<img src="http://oiburv3oy.bkt.clouddn.com/flow.png" alt="Drawing"/>


看完Adapter这部分，很多人都会觉得有些熟悉，这个设计跟Retrofit的CallAdapter很类似。对的，好的设计都是相通的，只是换了个形式，都是常用设计模式：适配器，工厂，单例等的组合。


## 结束语：
EasyBle的主要模块就是以上这些了，已经满足了蓝牙开发的基本需求。目前库还处于初级阶段，后续逐步会加一些功能，比如从网络加载adapter，如何在APP不升级版本的情况下，动态扩展集成能力。

库已经开源到Github：[EasyBLE](https://github.com/nziyouren/EasyBle) (https://github.com/nziyouren/EasyBle) ,欢迎大家contribute。


### 作者信息
* 姓名： 章星星 康之元信息技术有限公司就职，担任APP技术负责人 关注领域：Android，JAVA
* 银行账号： 招商银行 账号6225880285380136
* 身份证： 320682198710207451
* Email: nziyouren@gmail.com
* 电话：13679043608














