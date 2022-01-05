using Toybox.System;

class SystemUtil {
	static function hasMenuButton() {
		var mySettings = System.getDeviceSettings();
		if (mySettings.inputButtons == 0) { return true; } // Workaround for Venu which has a button but reports 0
		return ((mySettings.inputButtons & System.BUTTON_INPUT_MENU) != 0);
	}
}