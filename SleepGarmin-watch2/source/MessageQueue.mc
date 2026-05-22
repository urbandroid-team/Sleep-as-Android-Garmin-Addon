using Toybox.System as Sys;

class MessageQueue {

	var queue = [];

	var counter = 0;

	var freeMemRatio = 100;

	function initialize() {
	}

	public function getFirst() {
		if (queue.size() == 0) {return null;}
		return queue[0];
	}
	
	public function enqueueAsFirst(msg) {
		var size = queue.size();
		var newQ = new [size + 1];
		newQ[0] = msg;
		for (var i = 0; i < size; i++) {
			newQ[i+1] = queue[i];
		}
		queue = newQ;
	}

	public function enqueue(msg) {
		if (contains(msg)) { return; }

		if (counter > 20) {
		    var stats = Sys.getSystemStats();
    		freeMemRatio = (stats.freeMemory * 100) / stats.totalMemory;
			counter = 0;
		}

		counter++;

		if (freeMemRatio < 25 && queue.size() > 30) {
			DebugManager.log("Pruning queue: Low memory or Max size reached");
			queue = queue.slice(30, null); 
			freeMemRatio = 100;
		}
		
		queue.add(msg);
	}
	
	public function contains(msg) {
		return (queue.indexOf(msg) != -1);
	}
	
	public function removeFirst() {
		if (queue.size() > 0) {
        	queue = queue.slice(1, null);
 	    }
	}

	public function showCurrentQueue() {
		return queue;
	}

}