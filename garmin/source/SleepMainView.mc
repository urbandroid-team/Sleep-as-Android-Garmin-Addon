using Toybox.WatchUi as Ui;
using Toybox.Communications as Comm;
using Toybox.System as Sys;
using Toybox.Graphics as Gfx;

// Globals

var globString;
var Time;
var menuArray = [Rez.Strings.menu_label_1,Rez.Strings.menu_label_2,Time,Rez.Strings.menu_label_3,Rez.Strings.menu_label_4];
var index;
var callbackOnUpdate;
var DeviceContext;

class SleepMainView extends Ui.View {

    var bkg_night;
    var width; var height; var shape;

    function initialize() {
        View.initialize();
        callbackOnUpdate = method(:onUpdate);
    }

    function onLayout(dc) {
        setLayout( Rez.Layouts.MainLayout(dc) );
        shape = Sys.getDeviceSettings().screenShape;
        DeviceContext = dc;
    }

    //! Called when this View is brought to the foreground. Restore
    //! the state of this View and prepare it to be shown. This includes
    //! loading resources into memory.
    function onShow() {
    	// bkg_night = Ui.loadResource( Rez.Drawables.id_bkg_night );
        index = 2;
    }

    //! Update the view
    function onUpdate(dc) {
    	Time = timecurrent;
    	log("onUpdate");
        width = dc.getWidth();
        height = dc.getHeight();
        dc.setColor(Gfx.COLOR_TRANSPARENT, Gfx.COLOR_BLACK);
        dc.clear();
        dc.setColor(Gfx.COLOR_WHITE, Gfx.COLOR_TRANSPARENT);  
           
        var upText = new Ui.Text({
        		:text=>"upText",
        		:color=>Gfx.COLOR_WHITE,
        		:font=>Gfx.FONT_MEDIUM  ,
        		:locX =>WatchUi.LAYOUT_HALIGN_CENTER,
        		:locY=>WatchUi.LAYOUT_VALIGN_TOP 
        }); upText.draw(dc);
        
		var mainText = new Ui.Text({
        		:text=>"mainText",
        		:color=>Gfx.COLOR_WHITE,
        		:font=>Gfx.FONT_LARGE ,
        		:locX =>WatchUi.LAYOUT_HALIGN_CENTER,
        		:locY=>WatchUi.LAYOUT_VALIGN_CENTER
        }); mainText.draw(dc);
        
        var downText = new Ui.Text({
        		:text=>"mainText",
  	    		:color=>Gfx.COLOR_WHITE,
        		:font=>Gfx.FONT_MEDIUM  ,
        		:locX =>WatchUi.LAYOUT_HALIGN_CENTER,
           		:locY=>WatchUi.LAYOUT_VALIGN_BOTTOM 
        });downText.draw(dc);


    }

    //! Called when this View is removed from the screen. Save the
    //! state of this View here. This includes freeing resources from
    //! memory.
    function onHide() {
        if (bkg_night != null) {
            bkg_night = null;
        }
    }
    
    function viewRefresh(){
    	onUpdate(dc);
    }
    
}


class SleepMainDelegate extends Ui.BehaviorDelegate {

using Toybox.Graphics as Gfx;

    function initialize() {
        BehaviorDelegate.initialize();
    }

    function onMenu() { // Change behaviour
        return true;
    }

    function onBack() {
    	log("onBack");
    }
    
    function onSelect(){
    	log("onSelect");
    	refreshView();
    }
    
    function refreshView(){
    	log("Refreshing View");
    	callbackOnUpdate.invoke(DeviceContext);
    }
	
	/*************************************
	onNextPage 			Down 					(Bottom Left)
	onPreviousPage 		up/menu 				(Middle Left)
	onSelect			start button 				(top right)
	onBack	 			back/lap 				(bottom right)
	onMenu 				up/menu [long press] 	(Middle LEft)
*************************************/	
	
}