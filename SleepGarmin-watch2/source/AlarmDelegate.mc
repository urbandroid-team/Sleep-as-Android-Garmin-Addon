using Toybox.WatchUi;
using Toybox.Timer;

class AlarmDelegate extends WatchUi.BehaviorDelegate {

	var ctx;

    var viewTransitionTimer = new Timer.Timer();

    var isTransitionPending = false;

    function initialize(ctx) {
        BehaviorDelegate.initialize();
        self.ctx = ctx;
    }


    function safePushMenu() {
        isTransitionPending = false;

        var menu = new Rez.Menus.AlarmMenu();
        WatchUi.pushView(menu, new AlarmMenuDelegate(self.ctx), WatchUi.SLIDE_UP);

        return true;
    }

    function showMenu() {
        if (!isTransitionPending) {
            isTransitionPending = true;
            viewTransitionTimer.start(method(:safePushMenu), 1, false);
        }
        return true;
    }


    function onMenu() {
        showMenu();
        return true;
    }

    function onHold(clickEvent) {
        showMenu();
        return true;
    }

    function onSelect() {
        showMenu();
        return true;
    }

    function onBack() {
        showMenu();
        return true;
    }

    function onKey(keyEvent){
        showMenu();
        return true;
    }


}