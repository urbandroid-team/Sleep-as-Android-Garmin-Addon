using Toybox.System as Sys;
using Toybox.Application as App;

class MessageQueue {

    private var queue;
    private var maxQueueSize;

    function initialize(maxSize) {
        queue = [];
        maxQueueSize = maxSize; // Explicitly control queue size bounds
    }

    public function getFirst() {
        if (queue.size() == 0) { return null; }
        return queue[0];
    }
    
    // Optimized using Monkey C array concatenation syntax
    public function enqueueAsFirst(msg) {
        if (contains(msg)) { return; }
        queue = [msg] + queue; 
        enforceMemoryLimits();
    }

    public function enqueue(msg) {
        if (contains(msg)) { return; }

        queue.add(msg);
        enforceMemoryLimits();
    }
    
    public function contains(msg) {
        return (queue.indexOf(msg) != -1);
    }
    
    public function removeFirst() {
        var size = queue.size();
        if (size > 0) {
            // Corrected slice bounds to safely remove index 0
            queue = queue.slice(1, size);
        }
    }

    // Proactively manage memory based on size and actual available system RAM
    private function enforceMemoryLimits() {
        var size = queue.size();
        
        // 1. Hard cap pruning based on size
        if (size > maxQueueSize) {
            // Keep the newest 'maxQueueSize' elements (the end of the array)
            queue = queue.slice(size - maxQueueSize, size);
            size = queue.size();
        }

        // 2. Emergency pruning based on actual system memory pressure
        var stats = Sys.getSystemStats();
        if (stats.freeMemory < (stats.totalMemory * 0.20)) { // Under 20% memory left
            System.println("Low Memory Warning! Pruning queue.");
            if (size > 5) {
                // Aggressively chop queue down to the last 5 messages
                queue = queue.slice(size - 5, size);
            }
        }
    }

    public function showCurrentQueue() {
        return queue;
    }
}