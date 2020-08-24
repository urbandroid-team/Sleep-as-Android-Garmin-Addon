using Toybox.WatchUi;
using Toybox.System;

class Sleep2MenuDelegate extends WatchUi.MenuInputDelegate {

	var ctx;

    function initialize(ctx) {
        MenuInputDelegate.initialize();
        self.ctx = ctx;
    }

    function onMenuItem(item) {
        if (item == :item_1) {
        	self.ctx.businessManager.sendPause();
        } else if (item == :item_2) {
        	self.ctx.businessManager.sendStop();
        } else if (item == :item_3) {
        	self.ctx.businessManager.forceStop();
        }
    }

}