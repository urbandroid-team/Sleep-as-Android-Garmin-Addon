using Toybox.WatchUi as Ui;
using Toybox.System as Sys;
using Toybox.Communications as Comm;

class SleepAlarmMenuDelegate extends Ui.MenuInputDelegate {

    function initialize() {
        MenuInputDelegate.initialize();
    }

    function onMenuItem(item) {
        if (item == :item_alarm_1) {
            sendSnoozeAlarm();
        } else if (item == :item_alarm_2) {
            sendDismissAlarm();
        }
    }
}