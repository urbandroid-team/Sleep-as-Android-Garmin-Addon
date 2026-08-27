using Toybox.System;
using Toybox.WatchUi;
using Toybox.Timer;
using Toybox.Attention;
using Toybox.Math;
using Toybox.Time;
using Toybox.Lang;

class BusinessManager {
 
 	var ctx;

	var lastUpdate = -1;

    var lastUpdateUi = -1;

	var isOnAlarmScreen = false;

	var heartbeatTimer;

 	function initialize(ctx) {
 		self.ctx = ctx;
 	}

	function startHeartbeat() {
		if (heartbeatTimer == null) {
			heartbeatTimer = new Timer.Timer();
			heartbeatTimer.start(method(:heartbeatCallback), 4000, true);
		}
	}

	function stopHeartbeat() {
		if (heartbeatTimer != null) {
			heartbeatTimer.stop();
			heartbeatTimer = null;
		}
	}

	function heartbeatCallback() {
		var now = System.getTimer();
		if (lastUpdate == -1 || (DataUtil.abs(now - lastUpdate) > 3500)) {
			self.ctx.commManager.triggerSend();
		}
		if (lastUpdateUi == -1 || (DataUtil.abs(now - lastUpdateUi) > 5000)) {
			updateTime(true);
			lastUpdateUi = now;
		}
	}

 	// Hook that is called on every data received callback
 	function onDataHook() {
		var now = System.getTimer();

		if (lastUpdate == -1 || (DataUtil.abs(now - lastUpdate) > 1900)) {
 			self.ctx.commManager.triggerSend();
			lastUpdate = now;
		}

		if ((lastUpdateUi == -1) || (DataUtil.abs(now - lastUpdateUi) > 5000)) {
			updateTime(true);
			lockScreen();
			lastUpdateUi = now;
		}
 	}
 	
 	
 	function startComms() {
 		self.ctx.commManager.start();
		startHeartbeat();
 	}
 	
 	function startSensors() {
 		self.ctx.sensorManager.start();
 	}

 	function startTracking() {
 		self.ctx.state.tracking = true;
 		WatchUi.requestUpdate();
 	}

 	function displayOffWhenOnTrackingScreen() {
 		// if (ctx.state.onTrackingScreen) {
		// 	if (Toybox has :Attention) {
		// 		try {
		// 			Attention.backlight(false);
		// 		} catch (e) {}
		// 	}	
 		// }
 	}
 	
 	function confirmConnection() {
 		self.ctx.commManager.enqueue(CommManager.MSG_CONFIRMCHECK);
 	}
 	
 	function sendAccData(dataArray) {
 		self.ctx.commManager.enqueue([CommManager.MSG_DATA, dataArray]);
 	}
 	
 	function sendHrData(hr) {
 		DebugManager.log("sendHrData " + hr);
 		self.ctx.commManager.enqueue([CommManager.MSG_HR, hr]); 		
 	}

 	function sendRrIntervalsData(rr) {
 		DebugManager.log("sendRrData " + rr);
 		self.ctx.commManager.enqueue([CommManager.MSG_RR, rr]);
 	}

 	function sendOxyData(oxygenSaturation) {
 		self.ctx.commManager.enqueue([CommManager.MSG_OXY, oxygenSaturation]);
 	}
 	
 	function sendPause() {
 		self.ctx.commManager.enqueue(CommManager.MSG_PAUSE_TRACKING);
 	}

 	function sendResume() {
 		self.ctx.commManager.enqueue(CommManager.MSG_RESUME_TRACKING);
 	}

 	function sendStop() {
 		self.ctx.commManager.enqueueAsFirst(CommManager.MSG_STOP_TRACKING);
 	}

 	function sendSnoozeAlarm() {
 		self.ctx.alarmManager.snoozeAlarm();
 		self.ctx.commManager.enqueueAsFirst(CommManager.MSG_SNOOZE_ALARM);
 	}

 	function sendDismissAlarm() {
 		self.ctx.commManager.enqueueAsFirst(CommManager.MSG_DISMISS_ALARM); 	
 	}

 	function forceStop() {
		stopHeartbeat();
		self.ctx.sensorManager.stop();
		self.ctx.commManager.stop();
		self.ctx.alarmManager.stopAlarm();
		System.exit();
 	}
 	
 	function startAlarm(delay) {
		logTransmit("BusinessManager#startAlarm");

 		if (!self.ctx.state.isAlarmRunning()) {
	 		self.ctx.alarmManager.startAlarm(delay);
			self.ctx.state.setAlarmRunning(true);	
 		}
 	}
 	
 	function stopAlarm() {
		logTransmit("BusinessManager#stopAlarm isAlarmRunning: " + self.ctx.state.isAlarmRunning());
		self.ctx.alarmManager.stopAlarm();
		self.ctx.state.setAlarmRunning(false);
 	}
 	
 	function setBatchSize(size) {
 		self.ctx.state.setBatchSize(size);
 	}
 	
 	function updateTime(updateUi) {
 		self.ctx.state.updateTime();
 		if (updateUi) { WatchUi.requestUpdate(); }
 	}
 	
 	function setAlarmTime(time, updateUi) {
 		DebugManager.log("BusinessManager setAlarmTime: " + time);
 		self.ctx.state.updateAlarmTime(time);
 		if (updateUi) { WatchUi.requestUpdate(); }
 	}

	function isAroundAlarm() {
		var alarmMs = self.ctx.state.alarmTime;
		if (alarmMs instanceof Lang.Long && alarmMs > 0) {
			// Convert Unix MS to Seconds to compare with Time.now()
			var nowSec = Time.now().value();
			var alarmSec = alarmMs / 1000;
			return (nowSec > (alarmSec - 60)); // Is it within 60 seconds of alarm?
		}
		return false;
	}
 	
 	function unlockScreen() {
 	}
 	
 	function lockScreen() {
 	}
 	
 	function exit() {
		stopHeartbeat();
		self.ctx.sensorManager.stop();
		self.ctx.commManager.stop();
		self.ctx.alarmManager.stopAlarm();

 		System.exit();
 	}

 	function switchToAlarmScreen() {
		if (isOnAlarmScreen) { return; }

		if (Toybox has :Attention) {
			try {
				Attention.backlight(true);
			} catch (e) {}
		}	

 		WatchUi.pushView(new AlarmView(self.ctx), new AlarmDelegate(self.ctx), WatchUi.SLIDE_UP);
		isOnAlarmScreen = true;
 	}

 	function backToMainScreen() {
		if (isOnAlarmScreen) {
	 		WatchUi.popView(WatchUi.SLIDE_DOWN);
			isOnAlarmScreen = false;
		}
 	}
 	
 	function doHint(repeat) {
		self.ctx.alarmManager.doHint(repeat);
 	}

    function logTransmit(message) {
    	if (DebugManager.debug) {
			self.ctx.commManager.enqueueAsFirst("LOG: " + message);
        }
    }

 }