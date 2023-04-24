using Toybox.Timer;
using Toybox.Attention;

class AlarmManager {

	var ctx;

	// Note: Running timers cannot be stopped in CIQ. Nonrepeating timer ignores stop() calls completely, while repeating timers will execute one last time after stop() is called on them.
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
		self.ctx.businessManager.logTransmit("AlarmManager#startAlarmNow");
	    alarmDelayTimerRunning = false;
		self.ctx.businessManager.switchToAlarmScreen();
		self.ctx.alarmManager.startAlarmVibration();
	}

	function startAlarmAfterDelay() {
		self.ctx.businessManager.logTransmit("AlarmManager#startAlarmAfterDelay");
		if (alarmDelayTimerRunning) {
			startAlarmNow();
		}
	}
	
	function startAlarm(delay) {
		self.ctx.businessManager.logTransmit("AlarmManager#startAlarm, delay: " + delay);
	    alarmDelayTimerRunning = true;
        alarmDelayTimer.start(method(:startAlarmAfterDelay), delay, false);
	}

	function snoozeAlarm() {
		self.ctx.businessManager.logTransmit("AlarmManager#snoozeAlarm");
		stopAlarm();
	}
	
	function stopAlarm() {
		self.ctx.businessManager.logTransmit("AlarmManager#stopAlarm");
		cancelAlarms();

		self.ctx.alarmManager.stopAlarmVibration();
		self.ctx.businessManager.backToMainScreen();
	}

	function cancelAlarms() {
		self.ctx.businessManager.logTransmit("AlarmManager#cancelAlarms, alarmDelayTimerRunning: " + alarmDelayTimerRunning);

		if (alarmDelayTimerRunning) {
			alarmDelayTimer.stop();
			alarmDelayTimerRunning = false;
		}
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
		self.ctx.businessManager.logTransmit("AlarmManager#startAlarmVibration");
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
