using Toybox.System;

class Context {
    var commManager;
    var sensorManager;
    var businessManager;
    var state;
    var alarmManager;
    var featureFlags;

    function initialize() {
        DebugManager.log("Context initialized");
        // TODO create all instances needed
        self.featureFlags = new FeatureFlags();
        self.state = new State();
        self.commManager = new CommManager(self);
        self.sensorManager = new SensorManager(self);
        self.businessManager = new BusinessManager(self);
        self.alarmManager = new AlarmManager(self);
    }
}