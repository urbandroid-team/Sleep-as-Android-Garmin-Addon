using Toybox.System;
using Toybox.WatchUi;

class State {

	private var batchSize = 1;
	var deliveryInProgress = false;
    var deliveryErrorCount = 0;
    var deliveryPauseCount = 0;
	var lastDeliveryTs = 0;
		
	var doingHint = false;
	var doingAlarm = false;
	var tracking = false;
	
	var screenLocked = true;
	var screenLockedAt = 0;
	
	var onTrackingScreen = true;
	
	var currentTime;
	var alarmTime = -1;
	
	var httpCommunicationMode = false; // Tracks how we currently communicate with the phone
	
	function initialize() {
		DebugManager.log("State initialized");
		updateTime();
	}
	
	function setBatchSize(batchSize) {
		self.batchSize = batchSize;
	}
	
	function getBatchSize() {
		if (self.batchSize < 1) {
			return 1;
		}

		if (self.batchSize > 12) {
			return 12;
		}

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

	function setAlarmRunning(ringing) {
		self.doingAlarm = ringing;
	}
	
	function isHttpCommunicationMode() {
		return self.httpCommunicationMode;
	}
}