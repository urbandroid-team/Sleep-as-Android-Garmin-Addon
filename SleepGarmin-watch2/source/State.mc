using Toybox.System;
using Toybox.WatchUi;

class State {

	var ctx;
	
	private var batchSize = 1;
	var deliveryInProgress = false;
    var deliveryErrorCount = 0;
    var deliveryPauseCount = 0;
	var lastTransmitTs = 0;
		
	var doingHint = false;
	var doingAlarm = false;
	var tracking = false;
	
	var screenLocked = true;
	var onTrackingScreen = true;
	
	var currentTime;
	var alarmTime = "--:--";
	
	var httpCommunicationMode = false; // Tracks how we currently communicate with the phone
	
	function initialize(ctx) {
		DebugManager.log("State initialized");
		self.ctx = ctx;
		
		updateTime();
	}
	
	function setBatchSize(batchSize) {
		self.batchSize = batchSize;
	}
	
	function getBatchSize() {
		return self.batchSize;
	}
	
	function canGrabAttention() {
		return !self.doingHint && !self.doingAlarm;
	}
	
	function updateTime() {
        var now = System.getClockTime();
        currentTime = now.hour + ":" + now.min.format("%02d");	
	}
	
	function updateAlarmTime(time) {
		self.alarmTime = time;
	}
	
	function isAlarmRunning() {
		return self.doingAlarm;
	}
	
	function isHttpCommunicationMode() {
		return self.httpCommunicationMode;
	}

}