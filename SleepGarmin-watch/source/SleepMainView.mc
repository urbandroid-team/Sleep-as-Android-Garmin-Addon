using Toybox.WatchUi as Ui;
using Toybox.Communications as Comm;
using Toybox.System as Sys;
using Toybox.Graphics as Gfx;

// Globals

var globString;
// var menuArray = [Ui.loadResource(Rez.Strings.menu_label_1),Ui.loadResource(Rez.Strings.menu_label_2),timecurrent,Ui.loadResource(Rez.Strings.menu_label_3),Ui.loadResource(Rez.Strings.menu_label_4)];
var menuArray = [Ui.loadResource(Rez.Strings.menu_label_1),Ui.loadResource(Rez.Strings.menu_label_2),timecurrent,Ui.loadResource(Rez.Strings.menu_label_3)];
var arrayIndex = 2;


var width; var height; var shape;

function moduloPosArith(i,m){
	if (i < 0){i = i+menuArray.size();}
	return i%m;
}

class SleepMainView extends Ui.View {

    var bkg_night;
    var upText;
    var mainText;
    var downText;
    
	var startingStr = Ui.loadResource(Rez.Strings.starting);
	var trackingStr = Ui.loadResource(Rez.Strings.tracking);
	var stoppingStr = Ui.loadResource(Rez.Strings.stopping);

    function initialize() {
		log("SleepMainView initialize");
        View.initialize();
        
        upText = new Ui.Text({
        		:text=>null,
        		:color=>Gfx.COLOR_DK_GRAY,
        		:font=>Gfx.FONT_XTINY,
        		:locX =>Ui.LAYOUT_HALIGN_CENTER,
        		:locY=>Ui.LAYOUT_VALIGN_TOP
        });

		mainText = new Ui.Text({
        		:text=>null,
        		:color=>Gfx.COLOR_WHITE,
        		:font=>Gfx.FONT_LARGE,
        		:locX =>Ui.LAYOUT_HALIGN_CENTER,
        		:locY=>Ui.LAYOUT_VALIGN_CENTER
        });

        downText = new Ui.Text({
        		:text=>null,
  	    		:color=>Gfx.COLOR_DK_GRAY,
        		:font=>Gfx.FONT_XTINY,
        		:locX =>Ui.LAYOUT_HALIGN_CENTER,
           		:locY=>Ui.LAYOUT_VALIGN_BOTTOM
        });
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
    	if (trackingBool) {
    		menuArray[2] = timecurrent + "\n" + trackingStr;
    		log("changed view to tracking");
    	} 
 		if (!trackingBool) {
    		menuArray[2] = timecurrent + "\n" + startingStr;
    	}
    	if (stoppingBool) {
	    	menuArray[2] = timecurrent + "\n" + stoppingStr;
    	}
    	
    	
    	log("onUpdate");
        dc.setColor(Gfx.COLOR_TRANSPARENT, Gfx.COLOR_BLACK);
        dc.clear();
        dc.setColor(Gfx.COLOR_WHITE, Gfx.COLOR_TRANSPARENT);

        upText.setText(menuArray[ moduloPosArith( arrayIndex-1,menuArray.size() ) ]);
 	    downText.setText(menuArray[ moduloPosArith( arrayIndex+1,menuArray.size() ) ]);
 	    mainText.setText(menuArray[arrayIndex]);

        // Change font size of number
    	if (arrayIndex==1 ){downText.setFont(Gfx.FONT_XTINY);}
    	if (arrayIndex==2 ){mainText.setFont(Gfx.FONT_LARGE);}
    	if (arrayIndex==3 ){upText.setFont(Gfx.FONT_XTINY);}

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
		if (width != null) {
            width = null;
        }
        if (height != null) {
            height = null;
        }
        if (shape != null) {
            shape = null;
        }
    }
}
