using Toybox.WatchUi;

class AlarmDelegate extends WatchUi.BehaviorDelegate {

	var ctx;
	
    function initialize(ctx) {
        BehaviorDelegate.initialize();
        self.ctx = ctx;
    }

    function showMenu() {
        WatchUi.pushView(new Rez.Menus.AlarmMenu(), new AlarmMenuDelegate(self.ctx), WatchUi.SLIDE_UP);
        return true;
    }

    function onMenu() {
        return showMenu();
    }
    
    function onBack() {
    	return showMenu();
	}
	
    function onKey(keyEvent){
    
        return showMenu();
    }
	

}