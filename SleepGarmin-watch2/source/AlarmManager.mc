using Toybox.Timer;

class AlarmManager {

	var ctx;

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
			var alarmTimer = new Timer.Timer();
            alarmTimer.start(method(:startAlarmNow), delay, false);
		}
	
	}
	
	function stopAlarm() {
		self.ctx.attentionSeeker.stopAlarmVibration();
		self.ctx.businessManager.backToMainScreen();
	}
	

	

}