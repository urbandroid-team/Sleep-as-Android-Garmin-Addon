using Toybox.Timer;

class AlarmManager {

	var ctx;
	var alarmDelayTimer;
	var alarmDelayTimerRunning = false;

	function initialize(ctx) {
		self.ctx = ctx;
		self.alarmDelayTimer =  new Timer.Timer();
	}
	
	function startAlarmNow() {
	    alarmDelayTimerRunning = false;
		self.ctx.businessManager.switchToAlarmScreen();
		self.ctx.attentionSeeker.startAlarmVibration();
	}
	
	function startAlarm(delay) {
	    alarmDelayTimerRunning = true;
        alarmDelayTimer.start(method(:startAlarmNow), delay, false);
	}
	
	function stopAlarm() {
		self.ctx.attentionSeeker.stopAlarmVibration();
		self.ctx.businessManager.backToMainScreen();
	}

}
