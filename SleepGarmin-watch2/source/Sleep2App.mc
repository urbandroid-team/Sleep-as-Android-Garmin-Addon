using Toybox.Application;
using Toybox.WatchUi;
using Toybox.System;

class Sleep2App extends Application.AppBase {

    var ctx;

    function initialize() {
        AppBase.initialize();
    }

    // onStart() is called on application start up
    function onStart(state) {
        DebugManager.log("----- App onStart ------");
        self.ctx = new Context();
        
        // init comms
        self.ctx.businessManager.startComms();        
        self.ctx.businessManager.startSensors();
    }

    // onStop() is called when your application is exiting
    function onStop(state) {
    }

    // Return the initial view of your application here
    function getInitialView() {
        return [ new Sleep2View(self.ctx), new Sleep2Delegate(self.ctx) ];
//        return [ new AlarmView(self.ctx), new AlarmDelegate(self.ctx) ];
    }

}