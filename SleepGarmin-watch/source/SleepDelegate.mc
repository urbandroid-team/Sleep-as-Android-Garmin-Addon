using Toybox.WatchUi as Ui;

class SleepDelegate extends Ui.BehaviorDelegate {

    function initialize() {
        BehaviorDelegate.initialize();
    }

    function onMenu() { // Change behaviour
        log("onMenus");
        Ui.pushView(new Rez.Menus.MainMenu(), new SleepMenuDelegate(), Ui.SLIDE_UP);
        return true;
    }

    function onBack() {
    	log("onBack");
    	return true; //! disables back button
    }
    
    function onNextPage(){
    	log("onNextPage");
    }

	function onNextMode(){
		log("onNextMode");
	}

	function onPreviousPage(){ // Up is pressed
		log("onPreviousPage");
	}

	function onPreviousMode(){
		log("onPreviousMode");
	}

	function onSelect(){
		log("onSelect");
	}
	
	/*************************************
	onNextPage 			Down 					(Bottom Left)
	onPreviousPage 		up/menu 				(Middle Left)
	onSelect			start button 				(top right)
	onBack	 			back/lap 				(bottom right)
	onMenu 				up/menu [long press] 	(Middle LEft)
*************************************/	
	
}	