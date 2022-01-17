using Toybox.Timer;
using Toybox.Attention;

class AlarmManager {

	var ctx;

	var alarmDelayTimer = new Timer.Timer();
	var alarmDelayTimerRunning = false;

	var alarmTimer = new Timer.Timer();
	var alarmTimerRunning = false;

	var vibrTimer = new Timer.Timer();
	var vibrTimerRunning = false;

	const shortVibe = [new Attention.VibeProfile(100, 200)];

	function initialize(ctx) {
		self.ctx = ctx;
	}
	
	function startAlarmNow() {
	    alarmDelayTimerRunning = false;
		self.ctx.businessManager.switchToAlarmScreen();
		self.ctx.alarmManager.startAlarmVibration();
	}
	
	function startAlarm(delay) {
	    alarmDelayTimerRunning = true;
        alarmDelayTimer.start(method(:startAlarmNow), delay, false);
	}

	function snoozeAlarm() {
		stopAlarm();
	}
	
	function stopAlarm() {
		self.ctx.alarmManager.stopAlarmVibration();
		self.ctx.businessManager.backToMainScreen();
	}

	function hintVibrCallback() {
		self.ctx.state.doingHint = false;
		vibrTimerRunning = false;
	}
		
    function doHint(repeat) {
    	if (!self.ctx.state.canGrabAttention()) { return; }
    
        // Garmin only supports vibrating up to 8 VibeProfiles, so we have to cap repeating on 4
        if (repeat > 4) {
            repeat = 4;
        }

		// Only Edge/Oregon/Rino do not support vibrate according to APIdoc
		// Forerunner does not support vibration patterns
        if (Attention has :vibrate) {
	    	self.ctx.state.doingHint = true;
    
	        var vibrateData = [];
            for ( var i = 0; i < repeat; i += 1) {
                vibrateData.add(new Attention.VibeProfile(  50, 1000));
                vibrateData.add(new Attention.VibeProfile(  0, 1000));
            }
            Attention.vibrate(vibrateData);
            if (!vibrTimerRunning) {            
	            vibrTimer.start(method(:hintVibrCallback), 2000 * repeat, false);
	            vibrTimerRunning = true;
            }
        }
    }

	function vibrateForAlarmOnce() {
        if (Attention has :vibrate ) {
            Attention.vibrate(shortVibe); // length 200 ms
		}	
    }
    
    function startAlarmVibration() {
    	if (!alarmTimerRunning) {
	    	alarmTimerRunning = true;    	
	    	alarmTimer.start(method(:vibrateForAlarmOnce), 1000, true);
    	}
    }
    
    function stopAlarmVibration() {
    	if (alarmTimerRunning) { 
    		alarmTimer.stop(); 
    		alarmTimerRunning = false;
    	}
    }

}
