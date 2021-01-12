using Toybox.Timer;

class AlarmManager {

	var ctx;
	var alarmDelayTimer;

	function initialize(ctx) {
		self.ctx = ctx;
	}
	
	function startAlarmNow() {
		self.ctx.businessManager.switchToAlarmScreen();
		self.ctx.attentionSeeker.startAlarmVibration();
	}
	
	function startAlarm(delay) {
		if (delay == 0) {
			startAlarmNow();
			return;
		}
		
		if (delay > 0) {
			alarmDelayTimer = new Timer.Timer();
            alarmDelayTimer.start(method(:startAlarmNow), delay, false);
		}
	
	}
	
	function stopAlarm() {
		self.ctx.attentionSeeker.stopAlarmVibration();
		self.ctx.businessManager.backToMainScreen();
	}
	

	

}
