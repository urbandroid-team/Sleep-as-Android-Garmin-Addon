using Toybox.WatchUi as Ui;
using Toybox.System as Sys;
using Toybox.Communications as Comm;

class SleepAlarmMenuDelegate extends Ui.MenuInputDelegate {

    function initialize() {
        MenuInputDelegate.initialize();
    }

    function onMenuItem(item) {
        if (item == :item_alarm_1) {
            sendSnoozeAlarm();
        } else if (item == :item_alarm_2) {
            sendDismissAlarm();
        }
    }
    
    /**** 	Callbacks do not work on MenuDelegate ****
    function onBack() {
    //	log("onBack");
    	return true; //! disables back button
    }
    
    
    
     function onMenu() {
    	log("onMenu");
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
	*/
	
}

