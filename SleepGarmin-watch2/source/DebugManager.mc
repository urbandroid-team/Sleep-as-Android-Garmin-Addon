using Toybox.System;
using Toybox.Lang;
using Toybox.Time;
using Toybox.Time.Gregorian;

class DebugManager {
    const debug = false; // must be false for production
    const commDebug = false;

    static function log(message) {
    	if (DebugManager.debug) {
    		var currentTime = Gregorian.info(Time.now(), Time.FORMAT_MEDIUM);
    		var dateString = Lang.format("$1$:$2$:$3$", [currentTime.hour,currentTime.min,currentTime.sec]);
	        System.println(dateString + " " + message);
    	}
    }

    static function logf(msg, variables) {
        DebugManager.log(Lang.format(msg, variables));
    }
}