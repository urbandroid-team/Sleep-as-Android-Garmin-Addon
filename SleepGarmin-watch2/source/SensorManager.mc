using Toybox.Sensor;
using Toybox.System;

class SensorManager {

	const SENSOR_PERIOD_SEC = 4;
	const OXI_READING_PERIOD_SEC = 4;
	const SENSOR_FREQ = 10;
	const SENSOR_AGGREG_WINDOW_SEC = 10;

	var ctx;
	
	var accXBuf = [];
	var accYBuf = [];
	var accZBuf = [];
	var accBatch = [];
	
	var hrBuf = [];
	var rrIntervalsBuf = [];
	
	var spo2buf = [];
	
	var lastOximeterReadingSec = 0;

    function initialize(ctx) {
        DebugManager.log("SensorManager initialized");
        self.ctx = ctx;
        var maxSampleRate = Sensor.getMaxSampleRate();
        DebugManager.log("MaxSampleRate " + maxSampleRate);
    }
	
	function startHr() {
		Sensor.setEnabledSensors([Sensor.SENSOR_HEARTRATE]);
	}

	function startOxi() {
		Sensor.setEnabledSensors([Sensor.SENSOR_PULSE_OXIMETRY]);
	}

    function start() {
        DebugManager.log("SensorManager startAccelerometer");

		var options = {
			:period => SENSOR_PERIOD_SEC,
			:accelerometer => {
				:enabled => true,
				:sampleRate => SENSOR_FREQ
			},
			:heartBeatIntervals => {
				:enabled => true
			}};

        Sensor.registerSensorDataListener(SensorManager.method(:onData), options);
    }

	// function stop() {
	// 	Sensor.unregisterSensorDataListener()
	// }

    // argument is of type SensorData
    public function onData(sensorData as Sensor.SensorData) as Void {
        DebugManager.log("SensorManager onData");
        
        if (self.ctx.state.tracking) {
	        onAccelData(sensorData.accelerometerData.x, sensorData.accelerometerData.y, sensorData.accelerometerData.z);
	        
	        if (sensorData has :heartRateData && sensorData.heartRateData != null) {
	 	    	onHRData(sensorData.heartRateData.heartBeatIntervals);
			}	
			
			if (lastOximeterReadingSec >= OXI_READING_PERIOD_SEC) {
				lastOximeterReadingSec = 0;
				var sensorInfo = Sensor.getInfo();
			    if (sensorInfo has :oxygenSaturation && sensorInfo.oxygenSaturation != null) {
		    	    onOxyData(sensorInfo.oxygenSaturation);		    	    
			    }
			}
			lastOximeterReadingSec = lastOximeterReadingSec + SENSOR_PERIOD_SEC;
        }
        
		self.ctx.businessManager.onDataHook();
    }

    function onAccelData(xArr,yArr,zArr) {
    	DebugManager.log("onAccelData");
//        DebugManager.logf("sizes x: $1$ y: $2$ z: $3$", [xArr.size(), yArr.size(), zArr.size()]);
        
        accXBuf.addAll(xArr);        
        accYBuf.addAll(yArr);
        accZBuf.addAll(zArr);
        
        var maxCount = SENSOR_AGGREG_WINDOW_SEC * SENSOR_FREQ; // Maximum number of values to go into one aggregate (sampleRate [Hz] x batchPeriod [s])
        
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
    
    // Gathers both rr intervals and computes hr
    function onHRData(heartBeatIntervalsArray) {
    	DebugManager.log("OnHRData");
//    	DebugManager.log("HeartIntervals " + heartBeatIntervalsArray);

    	// Intervals are in ms
    	var latestHr = DataUtil.hrFromBeatIntervals(heartBeatIntervalsArray);
    	if (latestHr == null) { return; }
//        DebugManager.log("hr " + latestHr);
        
        rrIntervalsBuf.addAll(heartBeatIntervalsArray);
        if (rrIntervalsBuf.size() > 120) {
	    	DebugManager.log("OnHRData rrIntervalBuf>120");

        	rrIntervalsBuf.add(System.getTimer());
    		self.ctx.businessManager.sendRrIntervalsData(rrIntervalsBuf);
    		rrIntervalsBuf = [];        
        }
        
    	hrBuf.add(latestHr);
    	if (hrBuf.size() > (60 / SENSOR_PERIOD_SEC)) { // Minute divided by period 
	    	DebugManager.log("OnHRData hrBuf>12");
    		self.ctx.businessManager.sendHrData(DataUtil.median(hrBuf));
    		hrBuf = [];
    	}
    }
    
    function onOxyData(spo2) {
    	DebugManager.log("onOxyData");
    	
        spo2buf.add(spo2);
        if (spo2buf.size() > 120 / OXI_READING_PERIOD_SEC) {
        	spo2buf.add(OXI_READING_PERIOD_SEC); // add framerate at the end-1 of array
        	spo2buf.add(System.getTimer()); // add timestamp at the end of array
        	
    		self.ctx.businessManager.sendOxyData(spo2buf);
    		spo2buf = [];        
        }
    }

}