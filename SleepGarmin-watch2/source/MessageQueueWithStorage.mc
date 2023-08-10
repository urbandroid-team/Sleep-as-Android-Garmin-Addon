using Toybox.System as Sys; 
using Toybox.Application as App;
using Toybox.Application.Storage as Storage;

class MessageQueueWithStorage {
    // Max storage per app is 128 kB
    // Max storage per key is 8 kB
    // The queue would be scattered among multiple keys to maximize available space

    var keyPrefix = "queue_";
    var keyCount = 0;

    function initialize() {
        DebugManager.log("MessageQueueWithStorage");
        Storage.clearValues();
        keyCount = 0;
    }

    public function getFirst() {
        if (keyCount == 0) {return null;}
        return Storage.getValue(keyPrefix + "0");
    }

    public function enqueueAsFirst(msg) {
        var newKey = keyPrefix + keyCount;
        Storage.setValue(newKey, msg);
        keyCount++;

        for (var i = 0; i < keyCount - 1; i++) {
            var oldKey = keyPrefix + i;
            var replacementKey = keyPrefix + (i + 1);
            var value = Storage.getValue(oldKey);
    
            // TODO: Trycatch the possible full storage exception
            Storage.setValue(replacementKey, value);
        }
    
        // TODO: Trycatch the possible full storage exception
        Storage.setValue(keyPrefix + "0", msg);
    }

    public function enqueue(msg) {
        if (contains(msg)) { return; }


        var newKey = keyPrefix + keyCount;
        // TODO: Trycatch the possible full storage exception
        Storage.setValue(newKey, msg);
        keyCount++;
    }

    public function contains(msg) {
        for (var i = 0; i < keyCount; i++) {
            var key = keyPrefix + i;
            var value = Storage.getValue(key);
            if (value == msg) { return true; }
        }
        return false;
    }

    public function removeFirst() {
        if (keyCount == 0) {return;}
        Storage.deleteValue(keyPrefix + "0");
        for (var i = 1; i < keyCount; i++) {
            var oldKey = keyPrefix + i;
            var newKey = keyPrefix + (i - 1);
            var value = Storage.getValue(oldKey);
            // TODO: Trycatch the possible full storage exception
            Storage.setValue(newKey, value);
        }
        Storage.deleteValue(keyPrefix + (keyCount - 1));
        keyCount--;
    }

    public function showCurrentQueue() {
		return "to be implemented";
	}


}