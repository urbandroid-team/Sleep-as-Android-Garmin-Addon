using Toybox.Sensor;
using Toybox.System;
using Toybox.Math;
using Toybox.Lang;
using Toybox.ActivityRecording;
using Toybox.Position;

class SensorManager {

    private const SENSOR_PERIOD_SEC = 2;
    private const SENSOR_FREQ = 10;
    private const MAX_ARRAY_SIZE = 127;

    // Explicitly derive how many ticks make up our 10-second window
    private const TICKS_PER_AGGREG_WINDOW = 5; // 5 ticks * 2 seconds = 10 seconds

    private var ctx;
    private var dataListenerMethod;

    private var accBatch = [];
    private var spo2buf = [];

    private var tickCounter = 0;
    private var aggregateMax = -1.0;

    function initialize(ctx) {
        DebugManager.log("SensorManager initialized");
        self.ctx = ctx;
        // Cache our method pointer inside the constructor to save high-frequency allocation overhead
        self.dataListenerMethod = method(:onData);
    }

    public function startHr() {
        Sensor.setEnabledSensors([Sensor.SENSOR_HEARTRATE]);
    }

    public function startOxi() {
        Sensor.setEnabledSensors([Sensor.SENSOR_PULSE_OXIMETRY]);
    }

    public function start() {
        DebugManager.log("SensorManager tracking sequence starting");

        var options = {
            :period => SENSOR_PERIOD_SEC,
            :accelerometer => {
                :enabled => true,
                :sampleRate => SENSOR_FREQ
            }
        };

        // Inject either granular Heart Beat Intervals (HRV) or standard Heart Rate based on system state
        if (DebugManager.hrv) {
            options[:heartBeatIntervals] = { :enabled => true };
        } else {
            options[:heartRate] = { :enabled => true };
        }

        Sensor.registerSensorDataListener(self.dataListenerMethod, options);

        self.tickCounter = 0;
        self.aggregateMax = -1.0;
    }

    public function stop() {
        Sensor.unregisterSensorDataListener();
    }

    // argument is of type SensorData
    public function onData(sensorData as Sensor.SensorData) as Void {
		// DebugManager.log("SensorManager onData");

		try {

			if (self.ctx.state.tracking) {

				// DebugManager.log("SensorManager trackig");

            // 1. Process Accelerometer Vector Payloads safely
            var acc = sensorData.accelerometerData;
            if (acc != null && acc.x != null && acc.y != null && acc.z != null) {
                processAccelerometer(acc.x, acc.y, acc.z);
            }

				// We check Sensor.getInfo() because the listener's HR array is often null on this device
                var info = Sensor.getInfo();
                if (info != null && info.heartRate != null) {
                    self.ctx.businessManager.sendHrData(info.heartRate);
                }

// 2. Process Biometric Heart Rate Streams via scoped sub-properties
            if (sensorData has :heartRateData && sensorData.heartRateData != null) {
                var hrData = sensorData.heartRateData;

                if (DebugManager.hrv && hrData.heartBeatIntervals != null) {
                    var hrvArray = hrData.heartBeatIntervals;
                    if (hrvArray.size() > 0) {
                        processHRVData(hrvArray);
                    }
                } else if (hrData.heartRate != null) {
                    var hrArray = hrData.heartRate;
                    if (hrArray.size() > 0 && hrArray[0] != null) {
                        self.ctx.businessManager.sendHrData(hrArray[0]);
                    }
                }
            }

            // 3. Process Pulse Oximetry Data safely via global system sensors
            var info = Sensor.getInfo();
            if (info has :oxygenSaturation && info.oxygenSaturation != null) {
                processOxyData(info.oxygenSaturation);
            }

            self.ctx.businessManager.onDataHook();
        } catch(e) {
            DebugManager.log("Exception handled in Sensor execution loop: " + e.getErrorMessage());
        }
    }

    private function processAccelerometer(xArr, yArr, zArr) {
        var size = xArr.size();
        if (yArr.size() < size) { size = yArr.size(); }
        if (zArr.size() < size) { size = zArr.size(); }
        if (size == 0) { return; }

        var maxSquaredMag = 0.0;
        for (var i = 0; i < size; i++) {
            var x = xArr[i]; var y = yArr[i]; var z = zArr[i];
            if (x == null || y == null || z == null) { continue; }

            var currentSquaredMag = (x * x) + (y * y) + (z * z);
            if (currentSquaredMag > maxSquaredMag) {
                maxSquaredMag = currentSquaredMag;
            }
        }

        var currentMaxMag = Math.sqrt(maxSquaredMag);
        if (currentMaxMag > self.aggregateMax) {
            self.aggregateMax = currentMaxMag;
        }

        // Track time slots cleanly via integer ticks instead of fluctuating millisecond timers
        self.tickCounter++;
        if (self.tickCounter >= TICKS_PER_AGGREG_WINDOW) {
            if (self.aggregateMax != -1.0) {
                commitAccelToBatch(self.aggregateMax);
            }
            self.tickCounter = 0;
            self.aggregateMax = -1.0;
        }
    }

    private function commitAccelToBatch(aggregateValue) {
        if (accBatch.size() < MAX_ARRAY_SIZE) {
            accBatch.add(aggregateValue);
        }

        var effectiveBatchSize = self.ctx.state.getBatchSize();
        if (effectiveBatchSize > MAX_ARRAY_SIZE) { effectiveBatchSize = MAX_ARRAY_SIZE; }

        if (accBatch.size() >= effectiveBatchSize) {
            self.ctx.businessManager.sendAccData(accBatch);
            accBatch = []; // Purge allocations immediately
        }
    }

    private function processHRVData(heartBeatIntervalsArray) {
        // Safe linear check to compute structural Heart Rate values straight from Raw Pulse intervals
        var totalIntervals = heartBeatIntervalsArray.size();
        var sum = 0;
        for (var i = 0; i < totalIntervals; i++) {
            sum += heartBeatIntervalsArray[i];
        }

        if (sum > 0) {
            var calculatedHr = (60000 * totalIntervals) / sum;
            self.ctx.businessManager.sendHrData(calculatedHr);
        }
        self.ctx.businessManager.sendRrIntervalsData(heartBeatIntervalsArray);
    }
    
    private function processOxyData(spo2) {
        if (spo2buf.size() < MAX_ARRAY_SIZE) {
            spo2buf.add(spo2);
        }

        // Accumulate roughly 60 samples before wrapping structural packets cleanly
        if (spo2buf.size() > 60) {
            var structuredPayload = {
                "samples" => spo2buf,
                "period" => SENSOR_PERIOD_SEC,
                "timestamp" => System.getTimer()
            };

            self.ctx.businessManager.sendOxyData(structuredPayload);
            spo2buf = [];
        }
    }
}