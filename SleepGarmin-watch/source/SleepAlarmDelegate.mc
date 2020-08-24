using Toybox.WatchUi as Ui;

class SleepAlarmDelegate extends Ui.BehaviorDelegate {

    function initialize() {
        BehaviorDelegate.initialize();
    }

    function onMenu() {
    	// log("OnMenu");
        Ui.pushView(new Rez.Menus.AlarmMenu(), new SleepAlarmMenuDelegate(), Ui.SLIDE_UP);
        return true;
    }

    function onBack() {
      return true; //! disables back button
    }

    function onSelect() {
    	return true;
    }

    function onKey(keyEvent){
        var k = keyEvent.getKey();
    	// log("onKey: " + k);

    	if (k == KEY_ENTER) {
			// log("KEY_ENTER pressed");
			onMenu();
			return true;
    	}
    	return false;
	}

}