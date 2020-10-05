using Toybox.WatchUi;
using Toybox.System;

class Sleep2View extends WatchUi.View {

	var ctx;
	var lockedTextArea;

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
	    var textArea = View.findDrawableById("lockedTextArea");
    
		if (isScreenLocked) {
			if (self.ctx.hasMenuButton()) {
				textArea.setText(Rez.Strings.lockedNoTouch);		
			} else {			
				textArea.setText(Rez.Strings.lockedTouch);		
			}

		} else {				
			if (self.ctx.hasMenuButton()) {
				textArea.setText(Rez.Strings.unlockedNoTouch);					
			} else {
				textArea.setText(Rez.Strings.unlockedTouch);		
			}
		}    
    }
    
    private function updateTimeText() {
    	var timeArea = View.findDrawableById("time");
    	timeArea.setText(self.ctx.state.currentTime);
    }

    private function updateAlarmTimeText() {
    	var alarmTimeArea = View.findDrawableById("alarmTime");
    	
    	alarmTimeArea.setText(DateUtil.msTimestampToHHMM(self.ctx.state.alarmTime));
    }

    
}
