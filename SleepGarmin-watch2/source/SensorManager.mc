using Toybox.Sensor;
using Toybox.System;

class SensorManager {

	const SENSOR_PERIOD_SEC = 4;
	const OXI_READING_PERIOD_SEC = 4;
	const SENSOR_FREQ = 10;
	const SENSOR_AGGREG_WINDOW_SEC = 10;
	const MAX_ARRAY_SIZE = 127;

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
				:enabled => false
			}};

        Sensor.registerSensorDataListener(method(:onData), options);
    }

	// function stop() {
	// 	Sensor.unregisterSensorDataListener()
	// }




    // argument is of type SensorData
    public function onData(sensorData as Sensor.SensorData) as Void {

		try {
			DebugManager.log("SensorManager onData");
			
			if (self.ctx.state.tracking) {

				if (sensorData.accelerometerData != null && sensorData.accelerometerData.x != null && sensorData.accelerometerData.y != null && sensorData.accelerometerData.z != null) {
					onAccelData(sensorData.accelerometerData.x, sensorData.accelerometerData.y, sensorData.accelerometerData.z);
				}
				
				// if (sensorData has :heartRateData && sensorData.heartRateData != null) {
				// 	onHRData(sensorData.heartRateData.heartBeatIntervals);
				// }	
				
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
		} catch(e) {
    		DebugManager.log("onData error " + e.getErrorMessage());
		}
    }

    function onAccelData(xArr,yArr,zArr) {
    	DebugManager.log("onAccelData");
//        DebugManager.logf("sizes x: $1$ y: $2$ z: $3$", [xArr.size(), yArr.size(), zArr.size()]);
        
		var spaceLeftX = MAX_ARRAY_SIZE - accXBuf.size();

		if (spaceLeftX > 0) {
			accXBuf.addAll(xArr.slice(0, spaceLeftX));        
		}

		var spaceLeftY = MAX_ARRAY_SIZE - accYBuf.size();
		if (spaceLeftY > 0) {
			accYBuf.addAll(yArr.slice(0, spaceLeftY));
		}
		
		var spaceLeftZ = MAX_ARRAY_SIZE - accZBuf.size();
		if (spaceLeftZ > 0) {
			accZBuf.addAll(zArr.slice(0, spaceLeftZ));
		}
        
        var maxCount = SENSOR_AGGREG_WINDOW_SEC * SENSOR_FREQ; // Maximum number of values to go into one aggregate (sampleRate [Hz] x batchPeriod [s])
        
        // Since maximum sensor batching period is 4 seconds and we need to have aggregate period of 10 seconds, we need to aggregate two and half sensor batches. Then we need to retain the remaining half of the third batch.
        // In order to do that, we first add all the data from three batches into one array and then aggregate just first 100 values, deleting them from the batch arrays. 
        // This also means that we will be getting 10s data alternately after 12 and 8 seconds. Sleep as Android should supposedly handle that just fine. 
        if (accXBuf.size() >= maxCount && accYBuf.size() >= maxCount && accZBuf.size() >= maxCount) {
//	        DebugManager.logf("BEFORE SLICE sizes x: $1$ y: $2$ z: $3$", [accXBuf.size(), accYBuf.size(), accZBuf.size()]);

        	var aggregate = DataUtil.computeMaxRawFromArray(
				accXBuf.slice(0, maxCount), 
				accYBuf.slice(0, maxCount), 
				accZBuf.slice(0, maxCount));
			addToAccBatch(aggregate);
			
			accXBuf = accXBuf.slice(maxCount, accXBuf.size());
			accYBuf = accYBuf.slice(maxCount, accYBuf.size());
			accZBuf = accZBuf.slice(maxCount, accZBuf.size());
//	        DebugManager.logf("SLICED sizes x: $1$ y: $2$ z: $3$", [accXBuf.size(), accYBuf.size(), accZBuf.size()]);
        }
    }

    function addToAccBatch(aggregate) {
    	DebugManager.log("addToAccBatch");

		if (accBatch.size() < MAX_ARRAY_SIZE) {
	    	accBatch.add(aggregate);
		}

		var effectiveBatchSize = self.ctx.state.getBatchSize();
		if (effectiveBatchSize > MAX_ARRAY_SIZE) { effectiveBatchSize = MAX_ARRAY_SIZE; }

    	if (accBatch.size() >= effectiveBatchSize) {
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
        
		var spaceLeft = MAX_ARRAY_SIZE - rrIntervalsBuf.size();
		if (spaceLeft > 0) {
			rrIntervalsBuf.addAll(heartBeatIntervalsArray.slice(0, MAX_ARRAY_SIZE - rrIntervalsBuf.size()));
		}
        if (rrIntervalsBuf.size() >= 120 && rrIntervalsBuf.size() < MAX_ARRAY_SIZE) {
	    	DebugManager.log("OnHRData rrIntervalBuf>120");

        	rrIntervalsBuf.add(System.getTimer());
    		self.ctx.businessManager.sendRrIntervalsData(rrIntervalsBuf);
    		rrIntervalsBuf = [];        
        }
        
		if (hrBuf.size() < MAX_ARRAY_SIZE) {
    		hrBuf.add(latestHr);
		}
    	if (hrBuf.size() > (60 / SENSOR_PERIOD_SEC)) { // Minute divided by period 
	    	DebugManager.log("OnHRData hrBuf>12");
    		self.ctx.businessManager.sendHrData(DataUtil.median(hrBuf));
    		hrBuf = [];
    	}
    }
    
    function onOxyData(spo2) {
    	DebugManager.log("onOxyData");
    	
		if (spo2buf.size() < MAX_ARRAY_SIZE) {
	        spo2buf.add(spo2);
		}
        if (spo2buf.size() > 120 / OXI_READING_PERIOD_SEC) {
        	spo2buf.add(OXI_READING_PERIOD_SEC); // add framerate at the end-1 of array
        	spo2buf.add(System.getTimer()); // add timestamp at the end of array
        	
    		self.ctx.businessManager.sendOxyData(spo2buf);
    		spo2buf = [];        
        }
    }

}