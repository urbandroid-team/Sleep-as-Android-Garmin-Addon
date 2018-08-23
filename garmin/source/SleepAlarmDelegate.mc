using Toybox.WatchUi as Ui;

class SleepAlarmDelegate extends Ui.BehaviorDelegate {

    function initialize() {
        BehaviorDelegate.initialize();
    }

    function onMenu() {
        Ui.pushView(new Rez.Menus.AlarmMenu(), new SleepAlarmMenuDelegate(), Ui.SLIDE_UP);
        return true;
    }

    function onBack() {
      return true; //! disables back button
    }
    
    function onSelect() {
    	return true;
    }

}