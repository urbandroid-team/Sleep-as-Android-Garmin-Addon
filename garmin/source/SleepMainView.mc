using Toybox.WatchUi as Ui;
using Toybox.Communications as Comm;
using Toybox.System as Sys;
using Toybox.Graphics as Gfx;

// Globals

var globString;
// var menuArray = [Ui.loadResource(Rez.Strings.menu_label_1),Ui.loadResource(Rez.Strings.menu_label_2),timecurrent,Ui.loadResource(Rez.Strings.menu_label_3),Ui.loadResource(Rez.Strings.menu_label_4)];
var menuArray = [Ui.loadResource(Rez.Strings.menu_label_1),Ui.loadResource(Rez.Strings.menu_label_2),timecurrent,Ui.loadResource(Rez.Strings.menu_label_3),Ui.loadResource(Rez.Strings.menu_label_4)];
var arrayIndex = 2;

var width; var height; var shape;

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
		log("SleepMainView intiilalize");
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
    	log("")
    	// bkg_night = Ui.loadResource( Rez.Drawables.id_bkg_night );
    	log(arrayIndex);
    }

    //! Update the view
    function onUpdate(dc) {
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
