using Toybox.System;
using Toybox.Lang;

class DebugManager {
    const debug = true;
    
    static function log(message) {
    	if (DebugManager.debug) {
	        System.println(message);
    	}
    }

    static function logf(msg, variables) {
        DebugManager.log(Lang.format(msg, variables));
    }
}