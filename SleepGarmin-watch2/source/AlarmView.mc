using Toybox.WatchUi;
using Toybox.System;
using Toybox.Graphics as Gfx;

class AlarmView extends WatchUi.View {

	var ctx;

    function initialize(ctx) {
        self.ctx = ctx;
        
        View.initialize();
    }

    // Load your resources here
    function onLayout(dc) {
        setLayout(Rez.Layouts.AlarmLayout(dc));
		
		 
    }

    // Called when this View is brought to the foreground. Restore
    // the state of this View and prepare it to be shown. This includes
    // loading resources into memory.
    function onShow() {
    }

    // Update the view
    function onUpdate(dc) {
//		updateMainText(self.ctx.state.screenLocked, System.getDeviceSettings().isTouchScreen);
//		updateTimeText();
//		updateAlarmTimeText();
//		dc.clear();
        
		// Call the parent onUpdate function to redraw the layout
        View.onUpdate(dc);        
        
        updateTimeText();
        updateMainText();
        
//		dc.setColor(Gfx.COLOR_BLACK, Gfx.COLOR_WHITE);
//		dc.clear();
    }

    // Called when this View is removed from the screen. Save the
    // state of this View here. This includes freeing resources from
    // memory.
    function onHide() {
    }
    
    private function updateMainText() {
	    var textArea = View.findDrawableById("mainText");
    
		if (self.ctx.hasMenuButton()) {
			textArea.setText(Rez.Strings.mainAlarmTextNoTouch);					
		} else {
			textArea.setText(Rez.Strings.mainAlarmTextTouch);		
		}

    }
    
    private function updateTimeText() {
    	var timeArea = View.findDrawableById("time");
    	timeArea.setText(self.ctx.state.currentTime);
    }
       
}
