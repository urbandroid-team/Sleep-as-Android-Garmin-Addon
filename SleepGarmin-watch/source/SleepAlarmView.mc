using Toybox.WatchUi as Ui;
using Toybox.System as Sys;
using Toybox.Graphics as Gfx;

class SleepAlarmView extends Ui.View {
    var width;
    var height;
    var screenShape;
    var alarmIcon;
    


    function initialize() {
        View.initialize();
        
        alarmIcon = new Ui.Bitmap({
    		:rezId=>Rez.Drawables.id_alarm_icon,
    		:locX=>Ui.LAYOUT_HALIGN_CENTER,
    		:locY=>((Ui.LAYOUT_VALIGN_CENTER)+20),
		});

    }

    function onLayout(dc) {
        // setLayout(Rez.Layouts.MainLayout(dc));
        screenShape = Sys.getDeviceSettings().screenShape;
    }

    //! Called when this View is brought to the foreground. Restore
    //! the state of this View and prepare it to be shown. This includes
    //! loading resources into memory.
    function onShow() {
        alarmViewActive = true;
    }

    //! Update the view
    function onUpdate(dc) {
        width = dc.getWidth();
        height = dc.getHeight();
        dc.setColor(Gfx.COLOR_TRANSPARENT, Gfx.COLOR_WHITE);
        dc.clear();
        dc.setColor(Gfx.COLOR_TRANSPARENT, Gfx.COLOR_WHITE);

        //dc.drawText(width/2, height/2-15, Gfx.FONT_MEDIUM, "Alarm!!!\n" + timecurrent, Gfx.TEXT_JUSTIFY_CENTER);

			
        var AlarmText = new Ui.Text({
            :text=>timecurrent,
            :color=>Gfx.COLOR_BLACK,
            :font=>Gfx.FONT_LARGE,
            :locX =>WatchUi.LAYOUT_HALIGN_CENTER,
            :locY=>WatchUi.LAYOUT_VALIGN_CENTER
        });
//        dc.drawBitmap(0,0,alarmIcon);
		alarmIcon.draw(dc);
        AlarmText.draw(dc);            



        // Call the parent onUpdate function to redraw (reset) the layout
        // View.onUpdate(dc);
    }

    //! Called when this View is removed from the screen. Save the
    //! state of this View here. This includes freeing resources from
    //! memory.
    function onHide() {

        if (width != null) {
            width = null;
        }
        if (height != null) {
            height = null;
        }
        if (screenShape != null) {
            screenShape = null;
        }
    }
}
