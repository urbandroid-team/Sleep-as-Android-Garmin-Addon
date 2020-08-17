using Toybox.System as Sys;
using Toybox.Communications as Comm;

class CommListener extends Comm.ConnectionListener {

	var queue;
	var state;

    function initialize(queue, state) {
       Comm.ConnectionListener.initialize();
       self.queue = queue;
       self.state = state;
    }

    function onComplete() {
    	DebugManager.log("CommListener onComplete");
		queue.removeFirst();
    	state.deliveryInProgress = false;
    }

    function onError() {
    	DebugManager.log("CommListener onError");
    	state.deliveryInProgress = false;
    	state.deliveryErrorCount++;
    }
}
