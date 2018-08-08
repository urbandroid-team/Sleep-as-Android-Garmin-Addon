using Toybox.WatchUi as Ui;
using Toybox.Communications as Comm;
using Toybox.System as Sys;
using Toybox.Graphics as Gfx;

class SleepAlarmView extends Ui.View {

    var bkg_alarm = Ui.loadResource( Rez.Drawables.id_bkg_alarm );
    var width;
    var height;
    var screenShape;

    function initialize() {
        View.initialize();
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
        dc.setColor(Gfx.COLOR_TRANSPARENT, Gfx.COLOR_BLACK);
        dc.clear();
        dc.setColor(Gfx.COLOR_TRANSPARENT, Gfx.COLOR_BLACK);

        //dc.drawText(width/2, height/2-15, Gfx.FONT_MEDIUM, "Alarm!!!\n" + timecurrent, Gfx.TEXT_JUSTIFY_CENTER);
        var AlarmText = new Ui.Text({
            :text=>"Alarm!!!\n" + timecurrent,
            :color=>Gfx.COLOR_WHITE,
            :font=>Gfx.FONT_MEDIUM,
            :locX =>WatchUi.LAYOUT_HALIGN_CENTER,
            :locY=>WatchUi.LAYOUT_VALIGN_CENTER
        });
        AlarmText.draw(dc);            



        // Call the parent onUpdate function to redraw (reset) the layout
        // View.onUpdate(dc);
    }

    //! Called when this View is removed from the screen. Save the
    //! state of this View here. This includes freeing resources from
    //! memory.
    function onHide() {
        if (bkg_alarm != null) {
            bkg_alarm = null;
        }
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
