using Toybox.WatchUi;

class Sleep2Delegate extends WatchUi.BehaviorDelegate {

	var ctx;
	
    function initialize(ctx) {
        BehaviorDelegate.initialize();
        self.ctx = ctx;
    }

    function onMenu() {
    	DebugManager.log("onMenu");
    	if (self.ctx.state.screenLocked) {
    		self.ctx.businessManager.unlockScreen();
    		return true;
    	}

        var menu = new Rez.Menus.MainMenu();
        menu.addItem(Rez.Strings.version, :version);
    	
        WatchUi.pushView(menu, new Sleep2MenuDelegate(self.ctx), WatchUi.SLIDE_UP);
        return true;
    }
    
    function onBack() {
    	return true;
    }
    
    function onKey(keyEvent){
    	var k = keyEvent.getKey();

    	// Prevents exiting from the app
    	if (k == WatchUi.KEY_ESC || k == WatchUi.KEY_ENTER) { return true; }

    	return false;
    }

}