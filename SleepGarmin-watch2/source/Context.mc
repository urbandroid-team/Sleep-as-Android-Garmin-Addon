using Toybox.System;

class Context {
    var commManager;
    var sensorManager;
    var businessManager;
    var state;
    var attentionSeeker;
    var alarmManager;

    function initialize() {
        DebugManager.log("Context initialized");
        // TODO create all instances needed
        self.state = new State(self);
        self.commManager = new CommManager(self);
        self.sensorManager = new SensorManager(self);
        self.businessManager = new BusinessManager(self);
        self.attentionSeeker = new AttentionSeeker(self);
        self.alarmManager = new AlarmManager(self);
    }
    
	function hasMenuButton() {
		var mySettings = System.getDeviceSettings();
		
//		DebugManager.log("inputButtons " + mySettings.inputButtons);
//		DebugManager.log("BUTTON_INPUT_MENU " + System.BUTTON_INPUT_MENU);
//		DebugManager.log(mySettings.inputButtons & System.BUTTON_INPUT_MENU);
		
		if (mySettings.inputButtons == 0) { return true; } // Workaround for Venu which has a button but reports 0
		
		return ((mySettings.inputButtons & System.BUTTON_INPUT_MENU) != 0);
	}

	
}