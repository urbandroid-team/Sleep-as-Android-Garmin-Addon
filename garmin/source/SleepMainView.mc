using Toybox.WatchUi as Ui;
using Toybox.Communications as Comm;
using Toybox.System as Sys;
using Toybox.Graphics as Gfx;

// Globals

var globString;
var menuArray = [Ui.loadResource(Rez.Strings.menu_label_1),Ui.loadResource(Rez.Strings.menu_label_2),timecurrent,Ui.loadResource(Rez.Strings.menu_label_3),Ui.loadResource(Rez.Strings.menu_label_4)];
var arrayIndex = 2;

var width; var height; var shape;
var DeviceContext;

function moduloPosArith(i,m){
	//log("i: " + i + "  m: " + m);
	if (i < 0){
		i = i+menuArray.size();
	}
	//log("i%m: " + i%m);
	return i%m;
}

class SleepMainView extends Ui.View {

    var bkg_night;


    function initialize() {
        View.initialize();
    }

    function onLayout(dc) {
        setLayout( Rez.Layouts.MainLayout(dc) );
        shape = Sys.getDeviceSettings().screenShape;

		width = dc.getWidth();
        height = dc.getHeight();
		
    }

    //! Called when this View is brought to the foreground. Restore
    //! the state of this View and prepare it to be shown. This includes
    //! loading resources into memory.
    function onShow() {
    	// bkg_night = Ui.loadResource( Rez.Drawables.id_bkg_night );
    	log(arrayIndex);
    }

    //! Update the view
    function onUpdate(dc) {
    	DeviceContext = dc;
    	menuArray[2] = timecurrent;
    	log("onUpdate");
        dc.setColor(Gfx.COLOR_TRANSPARENT, Gfx.COLOR_BLACK);
        dc.clear();
        dc.setColor(Gfx.COLOR_WHITE, Gfx.COLOR_TRANSPARENT);  
       
        var upTextString = menuArray[ moduloPosArith( arrayIndex-1,menuArray.size() ) ];
        var upText = new Ui.Text({
        		:text=>upTextString,
        		:color=>Gfx.COLOR_DK_GRAY,
        		:font=>Gfx.FONT_MEDIUM,
        		:locX =>Ui.LAYOUT_HALIGN_CENTER,
        		:locY=>Ui.LAYOUT_VALIGN_TOP 
        });

        
        var mainTextString = menuArray[arrayIndex];
		var mainText = new Ui.Text({
        		:text=>mainTextString,
        		:color=>Gfx.COLOR_WHITE,
        		:font=>Gfx.FONT_LARGE,
        		:locX =>Ui.LAYOUT_HALIGN_CENTER,
        		:locY=>Ui.LAYOUT_VALIGN_CENTER
        }); 


		var downTextString = menuArray[ moduloPosArith( arrayIndex+1,menuArray.size() ) ];
        var downText = new Ui.Text({
        		:text=>downTextString,
  	    		:color=>Gfx.COLOR_DK_GRAY,
        		:font=>Gfx.FONT_MEDIUM,
        		:locX =>Ui.LAYOUT_HALIGN_CENTER,
           		:locY=>Ui.LAYOUT_VALIGN_BOTTOM 
        });
        
        // Change font size of number
    	if (arrayIndex==1 ){downText.setFont(Gfx.FONT_NUMBER_MEDIUM);}
    	if (arrayIndex==2 ){mainText.setFont(Gfx.FONT_NUMBER_HOT);}
    	if (arrayIndex==3 ){upText.setFont(Gfx.FONT_NUMBER_MEDIUM);}
    	
    	upText.draw(dc);
    	mainText.draw(dc);
    	downText.draw(dc);
    }

    //! Called when this View is removed from the screen. Save the
    //! state of this View here. This includes freeing resources from
    //! memory.
    function onHide() {
        if (bkg_night != null) {
            bkg_night = null;
        }
    }
}

class SleepMainDelegate extends Ui.InputDelegate  {

using Toybox.Graphics as Gfx;
using Toybox.WatchUi as Ui;

    function initialize() {
       BehaviorDelegate.initialize();
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
    			if (Sys.getDeviceSettings().phoneConnected && !fakeTransmit) {
                Comm.transmit("STOPPING", null, new SleepNowListener("STOPPING"));
                
            	} else if (!Sys.getDeviceSettings().phoneConnected){
				Sys.exit();
				}
    			break;
    		case 4:
    			log("Force stopped via menu");
            	if (Sys.getDeviceSettings().phoneConnected && !fakeTransmit) {
            		Comm.transmit("STOPPING", null, new SleepNowListener("STOPPING"));
            	}
            	Sys.exit();
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
		if (y> height*.25 && y< height * .75){onSelect();}
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
 		switchToView(new SleepMainView(),new SleepMainDelegate(),Ui.SLIDE_UP );
    }
    
    function scrollDown(){
    	log("scrollDown");
    	arrayIndex = moduloPosArith( arrayIndex-1,menuArray.size());
    	switchToView(new SleepMainView(),new SleepMainDelegate(),Ui.SLIDE_DOWN );
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