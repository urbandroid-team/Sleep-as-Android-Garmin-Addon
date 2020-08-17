using Toybox.WatchUi;

class AlarmDelegate extends WatchUi.BehaviorDelegate {

	var ctx;
	
    function initialize(ctx) {
        BehaviorDelegate.initialize();
        self.ctx = ctx;
    }

    function onMenu() {
        WatchUi.pushView(new Rez.Menus.AlarmMenu(), new AlarmMenuDelegate(self.ctx), WatchUi.SLIDE_UP);
        return true;
    }
    
    function onBack() {
    	return true;
	}

}