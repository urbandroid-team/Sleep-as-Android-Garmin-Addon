using Toybox.Attention;
using Toybox.Timer;

class AttentionSeeker {

    const shortPulse = [new Attention.VibeProfile(100, 200)];

	var ctx;
	var alarmTimer = new Timer.Timer();
	var alarmTimerRunning = false;

	var vibrTimer = new Timer.Timer();
	var vibrTimerRunning = false;

	function initialize(ctx) { self.ctx = ctx; }
	
	function hintVibrCallback() {
		self.ctx.state.doingHint = false;
		vibrTimerRunning = false;
	}
	
    function doHint(repeat) {
    	if (!self.ctx.state.canGrabAttention()) { return; }
    
    	self.ctx.state.doingHint = true;
    
        // Garmin only supports vibrating up to 8 VibeProfiles, so we have to cap repeating on 4
        if (repeat > 4) {
            repeat = 4;
        }

		// Only Edge/Oregon/Rino do not support vibrate according to APIdoc
		// Forerunner does not support vibration patterns
        if (Attention has :vibrate) {
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
            Attention.vibrate(shortPulse); // length 200 ms
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