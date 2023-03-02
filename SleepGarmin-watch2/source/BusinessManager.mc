using Toybox.System;
using Toybox.WatchUi;
using Toybox.Timer;
using Toybox.Attention;
		
class BusinessManager {
 
 	var ctx;

 	function initialize(ctx) {
 		self.ctx = ctx;
 	}
 	
 	// Hook that is called on every data received callback - we use this so that we do not have to have our own Timer, which is presumably battery intensive.
 	function onDataHook() {
 		DebugManager.log("BusinessManager onDataHook");
 		self.ctx.commManager.triggerSend();
 		updateTime(true);
 		lockScreen();
 	}
 	
 	
 	function startComms() {
 		self.ctx.commManager.start();
 	}
 	
 	function startSensors() {
 		self.ctx.sensorManager.start();
 	}
 	function startTracking() {
 		self.ctx.state.tracking = true;
 		WatchUi.requestUpdate();
 		
 	}

 	function displayOffWhenOnTrackingScreen() {
 		if (ctx.state.onTrackingScreen) {	
	 		Attention.backlight(false); 	
 		}
 	}
 	
 	function confirmConnection() {
 		self.ctx.commManager.enqueue(CommManager.MSG_CONFIRMCHECK);
 	}
 	
 	function sendAccData(dataArray) {
 		self.ctx.commManager.enqueue([CommManager.MSG_DATA, dataArray.toString()]);
 	}
 	
 	function sendHrData(hr) {
 		DebugManager.log("sendHrData " + hr);
 		self.ctx.commManager.enqueue([CommManager.MSG_HR, hr]); 		
 	}
 	function sendRrIntervalsData(rr) {
 		DebugManager.log("sendRrData " + rr);
 		self.ctx.commManager.enqueue([CommManager.MSG_RR, rr.toString()]); 	
 	}
 	function sendOxyData(oxygenSaturation) {
 		self.ctx.commManager.enqueue([CommManager.MSG_OXY, oxygenSaturation.toString()]);
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
		System.exit();
 	}
 	
 	function startAlarm(delay) {
 		if (!self.ctx.state.isAlarmRunning()) {
	 		self.ctx.alarmManager.startAlarm(delay);	
 		}
 	}
 	
 	function stopAlarm() {
 		if (self.ctx.state.isAlarmRunning()) {
 			self.ctx.alarmManager.stopAlarm();
 		} else {
			self.ctx.alarmManager.cancelAlarms();
		}
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
		if (self.ctx.state.alarmTime instanceof Lang.Long) {
			return (System.getTimer() > (self.ctx.state.alarmTime - 60000));
		} else {
			return false;
		}
	}
 	
 	function unlockScreen() {
 		if (self.ctx.state.screenLocked) {
			self.ctx.state.screenLocked = false;
			self.ctx.state.screenLockedAt = System.getTimer();
	 		DebugManager.log("UnlockScreen");
	 		WatchUi.requestUpdate();
	 	}
 	}
 	
 	function lockScreen() {
 		if (self.ctx.state.tracking && !self.ctx.state.screenLocked && (System.getTimer() - self.ctx.state.screenLockedAt > 5000)) {
	 		self.ctx.state.screenLocked = true;
	 		WatchUi.requestUpdate(); 		
 		}
 	}
 	
 	function exit() {
 		DebugManager.log("BusinessManager exit");
 		System.exit();
 	}

 	function switchToAlarmScreen() {
		try {
 			Attention.backlight(true);
		} catch (e) {

		}
 		WatchUi.pushView(new AlarmView(self.ctx), new AlarmDelegate(self.ctx), WatchUi.SLIDE_UP);
 	}

 	function backToMainScreen() {
 		WatchUi.popView(WatchUi.SLIDE_DOWN);
 	}
 	
 	function doHint(repeat) {
		self.ctx.alarmManager.doHint(repeat);
 	}

 }