using Toybox.Sensor;
using Toybox.System;
using Toybox.Math;
using Toybox.Lang;
using Toybox.ActivityRecording;
using Toybox.Position;

class SensorManager {

	const SENSOR_PERIOD_SEC = 2;
	const OXI_READING_PERIOD_SEC = 2;
	const SENSOR_FREQ = 10;
	const SENSOR_AGGREG_WINDOW_SEC = 10;
	const MAX_ARRAY_SIZE = 127;

	var ctx;
	
	var accBatch = [];

	var aggregateTime = -1;

	var aggregateMax = -1;

	var hrMedianBuf = [];
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

	// var session = null;

    function start() {
        DebugManager.log("SensorManager startAccelerometer");

		// if (Toybox has :ActivityRecording) {
		// 	if (session == null) {
		// 		session = ActivityRecording.createSession({
		// 			:name => "Sleep Tracking",
		// 			:sport => ActivityRecording.SPORT_GENERIC,
		// 			:subSport => ActivityRecording.SUB_SPORT_GENERIC,
		// 			:autoPause => false,
		// 		});
		// 		session.start();
		// 	} 

		// }

		var options = {
			:period => SENSOR_PERIOD_SEC,
			:accelerometer => {
				:enabled => true,
				:sampleRate => SENSOR_FREQ
			},
			:heartRate => {
				:enabled => true
			}};

		if (DebugManager.hrv) {
			options = {
				:period => SENSOR_PERIOD_SEC,
				:accelerometer => {
					:enabled => true,
					:sampleRate => SENSOR_FREQ
				},
				:heartBeatIntervals => {
					:enabled => true
				}};

		}


        Sensor.registerSensorDataListener(self.method(:onData), options);

		aggregateTime = System.getTimer();
    }

	function stop() {
		Sensor.unregisterSensorDataListener();
		// if (Toybox has :ActivityRecording) {
		// 	if (session == null) {
		// 		session.stop();
		// 		session.discard();
		// 		session = null;
		// 	} 

		// }
	}

    // argument is of type SensorData
    public function onData(sensorData as Sensor.SensorData) as Void {
		// DebugManager.log("SensorManager onData");

		try {
			
			if (self.ctx.state.tracking) {

				// DebugManager.log("SensorManager trackig");

				if (sensorData.accelerometerData != null && sensorData.accelerometerData.x != null && sensorData.accelerometerData.y != null && sensorData.accelerometerData.z != null) {

					// DebugManager.log("SensorManager hasAccelData");

					onAccelData(sensorData.accelerometerData.x, sensorData.accelerometerData.y, sensorData.accelerometerData.z);
				}

				// We check Sensor.getInfo() because the listener's HR array is often null on this device
                var info = Sensor.getInfo();
                if (info != null && info.heartRate != null) {
                    self.ctx.businessManager.sendHrData(info.heartRate);
                }

				if (sensorData has :heartRateData && sensorData.heartRateData != null) {

					if (DebugManager.hrv) {
						if (sensorData.heartRateData.heartBeatIntervals != null) {
							var hrvArray = sensorData.heartRateData.heartBeatIntervals;
							if (hrvArray != null && hrvArray.size() > 0) {
								onHRVData(hrvArray);
							}
						}
					} else {
						if (sensorData.heartRateData.heartRate != null) {
							var hrArray = sensorData.heartRateData.heartRate;

							if (hrArray != null && hrArray.size() > 0) {
								onHRData(hrArray);
							}
						}
					}
				}	

				if (info has :oxygenSaturation && info.oxygenSaturation != null) {
					var spo2 = info.oxygenSaturation;
					if (spo2 != null) {
						onOxyData(spo2);
					}
				}
			} else {
				DebugManager.log("SensorManager not tracking");
			}
			
			self.ctx.businessManager.onDataHook();
		} catch(e) {
    		DebugManager.log("onData error " + e.getErrorMessage());
		}
    }

    function onAccelData(xArr,yArr,zArr) {
    	DebugManager.log("onAccelData");
//        DebugManager.logf("sizes x: $1$ y: $2$ z: $3$", [xArr.size(), yArr.size(), zArr.size()]);
        
		var max = computeMaxRawFromArray(xArr, yArr, zArr);

		// DebugManager.log("SensorManager max " + max);

		if (max > aggregateMax) {
			aggregateMax = max;
		}

		// DebugManager.log("SensorManager aggr max " + aggregateMax);
 
		// DebugManager.log("SensorManager time window > 950 " + DataUtil.abs(System.getTimer() - aggregateTime));

		if (DataUtil.abs(System.getTimer() - aggregateTime) > 9500) {

			if (aggregateMax != -1) {

				// DebugManager.log("SensorManager addToAccBatch " + aggregateMax);

				addToAccBatch(aggregateMax);
				aggregateTime = System.getTimer();
				aggregateMax = -1;
			}
		}

    }

	function computeMaxRawFromArray(xArr, yArr, zArr) {
		var xSize = xArr.size();
		var ySize = yArr.size();
		var zSize = zArr.size();

		// FIND THE MINIMUM SIZE: This prevents the IAOOB error
		var size = xSize;
		if (ySize < size) { size = ySize; }
		if (zSize < size) { size = zSize; }

		if (size == 0) { 
			return null; 
		}	

		var maxSquaredMag = 0.0;

		for (var i = 0; i < size; i++) { 
			var x = xArr[i];
			var y = yArr[i];
			var z = zArr[i];

			if (x == null || y == null || z == null) {
				continue; 
			}
			var currentSquaredMag = (x * x) + (y * y) + (z * z);

			if (currentSquaredMag > maxSquaredMag) {
				maxSquaredMag = currentSquaredMag;
			}
		}

    	return Math.sqrt(maxSquaredMag);
	}


    function addToAccBatch(aggregate) {

		if (accBatch.size() < MAX_ARRAY_SIZE) {
	    	accBatch.add(aggregate);
		}
    	DebugManager.log("SensorManager addToAccBatchSize " + accBatch.size());

		var effectiveBatchSize = self.ctx.state.getBatchSize();
		if (effectiveBatchSize > MAX_ARRAY_SIZE) { effectiveBatchSize = MAX_ARRAY_SIZE; }

    	if (accBatch.size() >= effectiveBatchSize) {
	    	DebugManager.log("SensorManager send accel data " + accBatch.size());
    		self.ctx.businessManager.sendAccData(accBatch);
    		accBatch = [];
    	}
    }
    
    // // Gathers both rr intervals and computes hr
    // function onHRVData(heartBeatIntervalsArray) {
    // 	DebugManager.log("OnHRVData");

	// 	self.ctx.businessManager.sendRrIntervalsData(heartBeatIntervalsArray);
	// }

   // Gathers both just HR
    function onHRData(hrArray) {
    	DebugManager.log("OnHRVData");

		if (hrArray == null || hrArray.size() < 1) {
			return;
		}
		var hr = hrArray[0];

    	DebugManager.log("OnHRVData hr " + hr);

		self.ctx.businessManager.sendHrData(hr);
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


	// Gathers both rr intervals and computes hr
    function onHRVData(heartBeatIntervalsArray) {
    	DebugManager.log("OnHRVData");

		var hr = hrFromBeatIntervals(heartBeatIntervalsArray);

		if (hr != null && hr > 0) {
			self.ctx.businessManager.sendHrData(hr);
		}

		self.ctx.businessManager.sendRrIntervalsData(heartBeatIntervalsArray);
	}


	function hrFromBeatIntervals(beatIntervalArray) {
		DebugManager.log("hrFromBeatIntervals");
		if (beatIntervalArray == null) { return null; }
		
		var size = beatIntervalArray.size();

        if (size == 0) { return null; }		

		var sum = 0;
		for (var i = 0; i < size; i++) { sum += beatIntervalArray[i]; }
		return (60000 * size) / sum;

	}
}