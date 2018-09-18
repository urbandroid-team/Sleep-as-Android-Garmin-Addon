using Toybox.Graphics as Gfx;
using Toybox.WatchUi as Ui;
using Toybox.System as Sys;
using Toybox.Communications as Comm;

class SleepMainDelegate extends Ui.InputDelegate  {

    function initialize() {
			log("SleepMainDelegate initialize");
      //  BehaviorDelegate.initialize();
       InputDelegate.initialize();
    }

    function onSelect(){
    	log("onSelect");

    	switch (arrayIndex) {
    		case 0:
    			sendPauseTracking();
    			break;
    		case 1:
    			sendResumeTracking();
    			break;
    		case 2:
    			break;
    		case 3:
    			if (trackingBool){
    				normalExit();
    			} else {
    				forceExit();
    			}
    			break;
		}
		returnCentre();
    }

    function onSwipe(swipeEvent){
    	var s = swipeEvent.getDirection();
		log("onSwipe: " + s);
	    if (swipeEvent.getDirection() == SWIPE_DOWN ){
	    	log("Swipe Down");
	    	scrollDown();
	    }
	    if (swipeEvent.getDirection() == SWIPE_UP ){
	    	log("Swipe Up");
	    	scrollUp();
	    }
	    return true;
    }

	function onTap(clickEvent) {
		var x = clickEvent.getCoordinates()[0];
		var y = clickEvent.getCoordinates()[1];
		log("onTap at: " + clickEvent.getCoordinates());
		if (y <= height*.25){scrollDown();}
		if (y >= height*.75){scrollUp();}
		if (y > height*.25 && y< height * .75){onSelect();}
		return true;
    }

    function onKey(keyEvent){
        var k = keyEvent.getKey();
    	log("onKey: " + k);

    	switch (k) {
    		case KEY_ENTER:
    			onSelect();
    			break;
    		case KEY_UP:
    			scrollDown();
    			break;
    		case KEY_DOWN:
    			scrollUp();
    			break;
    		case KEY_ESC:
    			log("Escape Key");
    			returnCentre();
    			break;
    		case KEY_MENU:
    		    log("Menu Key");
    			break;
		}
    	return true;
    }

    function scrollUp(){
		log("scrollUp");
    	arrayIndex = moduloPosArith( arrayIndex+1,menuArray.size());
 			// switchToView(new SleepMainView(),new SleepMainDelegate(),Ui.SLIDE_UP );
			 refreshView();
    }

    function scrollDown(){
    	log("scrollDown");
    	arrayIndex = moduloPosArith( arrayIndex-1,menuArray.size());
    	// switchToView(new SleepMainView(),new SleepMainDelegate(),Ui.SLIDE_DOWN );
			refreshView();
    }

    function returnCentre(){
    	log("returnCentre");
    	arrayIndex = 2;
    	refreshView();
    }

    function refreshView(){
    	log("Refreshing View");
    	arrayIndex = moduloPosArith( arrayIndex,menuArray.size());
    	Ui.requestUpdate();
    }

	function pauseSelected(){}
}
/*************************************
	onNextPage 			Down 					(Bottom Left)
	onPreviousPage 		up/menu 				(Middle Left)
	onSelect			start button 				(top right)
	onBack	 			back/lap 				(bottom right)
	onMenu 				up/menu [long press] 	(Middle LEft)
*************************************/

/************************************
Possible Button Press Key
	KEY_POWER = 0
	KEY_LIGHT = 1
	KEY_ZIN = 2
	KEY_ZOUT = 3
	KEY_ENTER = 4
	KEY_ESC = 5
	KEY_FIND = 6
	KEY_MENU = 7
	KEY_DOWN = 8
	KEY_DOWN_LEFT = 9
	KEY_DOWN_RIGHT = 10
	KEY_LEFT = 11
	KEY_RIGHT = 12
	KEY_UP = 13
	KEY_UP_LEFT = 14
	KEY_UP_RIGHT = 15
	EXTENDED_KEYS = 16
	KEY_PAGE = 17
	KEY_START = 18
	KEY_LAP = 19
	KEY_RESET = 20
	KEY_SPORT = 21
	KEY_CLOCK = 22
	KEY_MODE = 23
	*************************************/