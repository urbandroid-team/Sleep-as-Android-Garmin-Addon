using Toybox.WatchUi;
using Toybox.System;
using Toybox.Time;

class Sleep2View extends WatchUi.View {

	var ctx;
	var messageArea;

    function initialize(ctx) {
        self.ctx = ctx;
        
        View.initialize();
    }

    // Load your resources here
    function onLayout(dc) {
        setLayout(Rez.Layouts.MainLayout(dc));
    }

    // Called when this View is brought to the foreground. Restore
    // the state of this View and prepare it to be shown. This includes
    // loading resources into memory.
    function onShow() {
    	self.ctx.state.onTrackingScreen = true;
    }

    // Update the view
    function onUpdate(dc) {
    
    	if (self.ctx.state.tracking) {
			updateMainText(self.ctx.state.screenLocked);
		}

		updateTimeText();
		updateAlarmTimeText();
        
		// Call the parent onUpdate function to redraw the layout
        View.onUpdate(dc);        
    }

    // Called when this View is removed from the screen. Save the
    // state of this View here. This includes freeing resources from
    // memory.
    function onHide() {
    	self.ctx.state.onTrackingScreen = false;
    }
    
    private function updateMainText(isScreenLocked) {
	    var textArea = View.findDrawableById("message");
        if (textArea != null) {
            textArea.setText(Rez.Strings.tracking);
        }
    }
    
    private function updateTimeText() {
    	var timeArea = View.findDrawableById("time");
        if (timeArea != null && self.ctx.state.currentTime != null) {
    	    timeArea.setText(self.ctx.state.currentTime);
        }
    }

    private function updateAlarmTimeText() {
    	var alarmTimeArea = View.findDrawableById("alarmTime");
        if (alarmTimeArea == null) {
            return;
        }
        
        var alarmTime = self.ctx.state.alarmTime;
        if (alarmTime > -1) {
            if ((alarmTime / 1000) - (Time.now().value().toLong()) > 86400) {
                alarmTimeArea.setText(DateUtil.msTimestampToDDDHHMM(self.ctx.state.alarmTime));
            } else {
                alarmTimeArea.setText(DateUtil.msTimestampToHHMM(self.ctx.state.alarmTime));
            }
        } else {
            alarmTimeArea.setText("---");
        }
    }

    
}
