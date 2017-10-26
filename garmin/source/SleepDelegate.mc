using Toybox.WatchUi as Ui;

class SleepDelegate extends Ui.BehaviorDelegate {

    function initialize() {
        BehaviorDelegate.initialize();
    }

    function onMenu() {
        Ui.pushView(new Rez.Menus.MainMenu(), new SleepMenuDelegate(), Ui.SLIDE_UP);
        return true;
    }

    function onBack() {
    	return true; //! disables back button
    }

}