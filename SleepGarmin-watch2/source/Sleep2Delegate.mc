using Toybox.WatchUi;

class Sleep2Delegate extends WatchUi.BehaviorDelegate {

	var ctx;
	
    function initialize(ctx) {
        BehaviorDelegate.initialize();
        self.ctx = ctx;
    }

    function onMenu() {
        showMenu();
        return true;
    }

    function showMenu() {
      	// DebugManager.log("showMenu");

        var menu = new Rez.Menus.MainMenu();
        menu.addItem(WatchUi.loadResource(Rez.Strings.version), :version);
    	
        WatchUi.pushView(menu, new Sleep2MenuDelegate(self.ctx), WatchUi.SLIDE_UP);
        
        return true;
    } 

    function onHold() {
        return showMenu();
    }

    function onSelect() {
        return showMenu();
    }

    function onBack() {
    	return showMenu();
    }
    
    function onKey(keyEvent){
    	// var k = keyEvent.getKey();

    	// Prevents exiting from the app
    	// if (k == WatchUi.KEY_ESC || k == WatchUi.KEY_ENTER) { 
            return showMenu();
        //     return true; 
        // }
    }

}