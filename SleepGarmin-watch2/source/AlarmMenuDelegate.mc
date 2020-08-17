using Toybox.WatchUi;
using Toybox.System;

class AlarmMenuDelegate extends WatchUi.MenuInputDelegate {

	var ctx;

    function initialize(ctx) {
        MenuInputDelegate.initialize();
        self.ctx = ctx;
    }

    function onMenuItem(item) {
        if (item == :item_1) {
        	self.ctx.businessManager.sendSnoozeAlarm();
        } else if (item == :item_2) {
        	self.ctx.businessManager.sendDismissAlarm();
        }
    }

}