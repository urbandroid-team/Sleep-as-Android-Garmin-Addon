using Toybox.System as Sys;
using Toybox.Application as App;

class MessageQueue {

	var queue = [];

	function initialize() {
    }

    public function getFirst() {
        if (queue.size() == 0) { return null; }
        return queue[0];
    }

    public function enqueueAsFirst(msg) {
        pruneIfNeeded();
		var newQ = [msg];
		newQ.addAll(queue);
		queue = newQ;
    }

    public function enqueue(msg) {
        if (contains(msg)) { return; }
        pruneIfNeeded();
        queue.add(msg);
    }

    private function pruneIfNeeded() {
        var stats = Sys.getSystemStats();
        var freeMemRatio = (stats.totalMemory > 0) ? (stats.freeMemory * 100 / stats.totalMemory) : 100;

        DebugManager.log("free: " + stats.freeMemory + " ratio:" + freeMemRatio + " q size:" + queue.size());

        while (queue.size() > 0 && ((freeMemRatio <= 50) || (queue.size() >= 15))) {
            DebugManager.log("Rem from q, freeRatio:" + freeMemRatio + ", q:" + queue.size());
            queue.remove(queue[0]);
        }
    }
    
    public function contains(msg) {
        return (queue.indexOf(msg) != -1);
    }
    
    public function removeFirst() {
		queue.remove(self.getFirst());
    }

    public function showCurrentQueue() {
        return queue;
    }
}