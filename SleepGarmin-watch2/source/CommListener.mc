using Toybox.System as Sys;
using Toybox.Communications as Comm;

class CommListener extends Comm.ConnectionListener {

	var queue;
	var ctx;

    function initialize(queue, ctx) {
       Comm.ConnectionListener.initialize();
       self.queue = queue;
       self.ctx = ctx;
    }

    function onComplete() {
    	DebugManager.log("CommListener onComplete");
		queue.removeFirst();
    	self.ctx.state.deliveryInProgress = false;
    	self.ctx.commManager.triggerSend();
    }

    function onError() {
    	DebugManager.log("CommListener onError");
    	self.ctx.state.deliveryInProgress = false;
    	self.ctx.state.deliveryErrorCount++;
    }
}
