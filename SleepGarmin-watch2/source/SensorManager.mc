using Toybox.Sensor;

class SensorManager {

	const SENSOR_PERIOD_SEC = 4; 

	var ctx;
	
	var accXBuf = [];
	var accYBuf = [];
	var accZBuf = [];
	var accBatch = [];
	
	var hrBuf = [];

    function initialize(ctx) {
        DebugManager.log("SensorManager initialized");
        self.ctx = ctx;
        var maxSampleRate = Sensor.getMaxSampleRate();
        DebugManager.log("MaxSampleRate " + maxSampleRate);
    }

    function start() {
        DebugManager.log("SensorManager startAccelerometer");

		var options = {
			:period => SENSOR_PERIOD_SEC,
			:accelerometer => {
				:enabled => true,
				:sampleRate => 10
			},
			:heartBeatIntervals => {
				:enabled => true
			}
		};
        Sensor.registerSensorDataListener(method(:onData), options);
    }

    // argument is of type SensorData
    function onData(sensorData) {
        DebugManager.log("SensorManager onData");
        
        if (self.ctx.state.tracking) {
	        onAccelData(sensorData.accelerometerData.x, sensorData.accelerometerData.y, sensorData.accelerometerData.z);
	        
	        if (sensorData has :heartRateData && sensorData.heartRateData != null) {
	 	    	onHRData(sensorData.heartRateData.heartBeatIntervals);
			}        
        }
		
		self.ctx.businessManager.onDataHook();
    }

    function onAccelData(xArr,yArr,zArr) {
    	DebugManager.log("onAccelData");
//        DebugManager.logf("sizes x: $1$ y: $2$ z: $3$", [xArr.size(), yArr.size(), zArr.size()]);
        
        accXBuf.addAll(xArr);        
        accYBuf.addAll(yArr);
        accZBuf.addAll(zArr);
        
        var maxCount = 100; // Maximum number of values to go into one aggregate (sampleRate [Hz] x batchPeriod [s])
        
        // Since maximum sensor batching period is 4 seconds and we need to have aggregate period of 10 seconds, we need to aggregate two and half sensor batches. Then we need to retain the remaining half of the third batch.
        // In order to do that, we first add all the data from three batches into one array and then aggregate just first 100 values, deleting them from the batch arrays. 
        // This also means that we will be getting 10s data alternately after 12 and 8 seconds. Sleep as Android should supposedly handle that just fine. 
        if (accXBuf.size() >= maxCount) {
//	        DebugManager.logf("BEFORE SLICE sizes x: $1$ y: $2$ z: $3$", [accXBuf.size(), accYBuf.size(), accZBuf.size()]);

        	var aggregate = DataUtil.computeMaxRawFromArray(xArr.slice(0, maxCount), yArr.slice(0, maxCount), zArr.slice(0, maxCount));
			addToAccBatch(aggregate);
			
			accXBuf = accXBuf.slice(maxCount, null);
			accYBuf = accYBuf.slice(maxCount, null);
			accZBuf = accZBuf.slice(maxCount, null);
//	        DebugManager.logf("SLICED sizes x: $1$ y: $2$ z: $3$", [accXBuf.size(), accYBuf.size(), accZBuf.size()]);
        }
    }
    
    function addToAccBatch(aggregate) {
    	DebugManager.log("addToAccBatch");
    	accBatch.add(aggregate);
    	if (accBatch.size() >= self.ctx.state.getBatchSize()) {
    		self.ctx.businessManager.sendAccData(accBatch);
    		accBatch = [];
    	}
    }
    
    function onHRData(heartBeatIntervalsArray) {
    	DebugManager.log("OnHRData");
//    	DebugManager.log("HeartIntervals " + heartBeatIntervalsArray);

    	// Intervals are in ms
    	var latestHr = DataUtil.hrFromBeatIntervals(heartBeatIntervalsArray);
    	if (latestHr == null) { return; }
//        DebugManager.log("hr " + latestHr);
        
    	hrBuf.add(latestHr);
    	if (hrBuf.size() > (60 / SENSOR_PERIOD_SEC)) { // Minute divided by period 
    		self.ctx.businessManager.sendHrData(DataUtil.median(hrBuf));
    		hrBuf = [];
    	}
    }

}