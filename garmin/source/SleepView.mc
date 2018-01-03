using Toybox.WatchUi as Ui;
using Toybox.Communications as Comm;
using Toybox.System as Sys;
using Toybox.Graphics as Gfx;

class SleepView extends Ui.View {

    var bkg_night;
    var width;
    var height; var shape;

    function initialize() {
        View.initialize();
    }

    function onLayout(dc) {
        setLayout( Rez.Layouts.MainLayout(dc) );
        shape = Sys.getDeviceSettings().screenShape;
    }

    //! Called when this View is brought to the foreground. Restore
    //! the state of this View and prepare it to be shown. This includes
    //! loading resources into memory.
    function onShow() {
    	// bkg_night = Ui.loadResource( Rez.Drawables.id_bkg_night );
    }

    //! Update the view
    function onUpdate(dc) {
        width = dc.getWidth();
        height = dc.getHeight();
        dc.setColor(Gfx.COLOR_TRANSPARENT, Gfx.COLOR_BLACK);
        dc.clear();
        dc.setColor(Gfx.COLOR_WHITE, Gfx.COLOR_TRANSPARENT);


		if(Sys.SCREEN_SHAPE_ROUND == shape) {
	        // dc.drawBitmap(width/2-80, height/2-74, bkg_night);
            dc.drawText(width/2, height/3+10, Gfx.FONT_NUMBER_HOT, timecurrent, Gfx.TEXT_JUSTIFY_CENTER);
            if (beta == true) {
            	dc.drawText(width/2, height/1.2, Gfx.FONT_MEDIUM, "HR>>" + current_heartrate, Gfx.TEXT_JUSTIFY_CENTER);
        	}
		} else if (Sys.SCREEN_SHAPE_RECTANGLE == shape) {
			if (height > width) { //vivoactive HR
				// dc.drawBitmap(width/2-80, height/2-74, bkg_night);
                dc.drawText(width/2, 0, Gfx.FONT_NUMBER_HOT, timecurrent, Gfx.TEXT_JUSTIFY_CENTER);
                if (beta == true) {
                	dc.drawText(width/2, height/1.2, Gfx.FONT_MEDIUM, "HR>>" + current_heartrate, Gfx.TEXT_JUSTIFY_CENTER);
            	}
			} else { //vivoactive
				// dc.drawBitmap((width/2)-80, (height/2)-72, bkg_night);
                dc.drawText(width-20, 10, Gfx.FONT_NUMBER_HOT, timecurrent, Gfx.TEXT_JUSTIFY_RIGHT);
                if (beta == true) {
                	dc.drawText(width/2, height/1.2, Gfx.FONT_MEDIUM, "HR>>" + current_heartrate, Gfx.TEXT_JUSTIFY_CENTER);
            	}			}
		} else if (Sys.SCREEN_SHAPE_SEMI_ROUND == shape) {
	        // dc.drawBitmap(width/2-80, height/2-74, bkg_night);
            dc.drawText(width/2, height/3, Gfx.FONT_NUMBER_HOT, timecurrent, Gfx.TEXT_JUSTIFY_LEFT);
            if (beta == true) {
            	dc.drawText(width/2, height/1.2, Gfx.FONT_MEDIUM, "HR>>" + current_heartrate, Gfx.TEXT_JUSTIFY_CENTER);
        	}
		}

        dc.drawText(width/2, height/2, Gfx.FONT_TINY, notice, Gfx.TEXT_JUSTIFY_CENTER);

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
