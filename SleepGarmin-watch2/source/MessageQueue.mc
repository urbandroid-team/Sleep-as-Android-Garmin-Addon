using Toybox.System as Sys;

class MessageQueue {

	var queue = [];

	function initialize() {
	}

	public function getFirst() {
		if (queue.size() == 0) {return null;}
		return queue[0];
	}
	
	function enqueueAsFirst(msg) {
		var newQ = [msg];
		newQ.addAll(queue);
		queue = newQ;
		newQ = null;
	}
	
	function enqueue(msg) {
		if (contains(msg)) { return; }

        var freeMemRatio = Sys.getSystemStats().freeMemory*100/Sys.getSystemStats().totalMemory;

        DebugManager.log("free: " + Sys.getSystemStats().freeMemory + " ratio:" + freeMemRatio + " q size:" + queue.size());

        if (((freeMemRatio <= 65) && (queue.size() > 0)) || (queue.size() > 30)) {
            DebugManager.log("Rem from q, freeRatio:" + freeMemRatio + ",q:" + queue.size());
            queue.remove(queue[0]);
        }
		
		queue.add(msg);
	}
	
	function isEmpty() {
		return queue.size == 0;
	}
	
	function contains(msg) {
		return (queue.indexOf(msg) != -1);
	}
	
	function removeFirst() {
		queue.remove(self.getFirst());
	}

}