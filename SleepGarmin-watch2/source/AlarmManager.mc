using Toybox.Timer;

class AlarmManager {

	var ctx;
	var alarmDelayTimer = new Timer.Timer();
	var alarmDelayTimerRunning = false;

	function initialize(ctx) {
		self.ctx = ctx;
	}
	
	function startAlarmNow() {
	    alarmDelayTimerRunning = false;
		self.ctx.businessManager.switchToAlarmScreen();
		self.ctx.attentionSeeker.startAlarmVibration();
	}
	
	function startAlarm(delay) {
		if (delay == 0) {
			startAlarmNow();
			return;
		}
		
		if (delay > 0) {
		    alarmDelayTimerRunning = true;
            alarmDelayTimer.start(method(:startAlarmNow), delay, false);
		}
	
	}
	
	function stopAlarm() {
		self.ctx.attentionSeeker.stopAlarmVibration();
		self.ctx.businessManager.backToMainScreen();
	}
	

	

}
