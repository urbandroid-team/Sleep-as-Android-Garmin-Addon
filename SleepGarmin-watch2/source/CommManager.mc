using Toybox.Communications;

class CommManager {

	var ctx;
	var queue;
	private var commListener;

	// From phone
	static const MSG_START = "StartTracking";
	static const MSG_START_HR = "StartHRTracking";
	static const MSG_STOP = "StopApp";
	static const MSG_CHECK = "Check";
	static const MSG_BATCH_SIZE = "BatchSize;";
	static const MSG_SET_ALARM = "SetAlarm;";
	static const MSG_START_ALARM = "StartAlarm;";
	static const MSG_HINT = "Hint;";
	static const MSG_STOP_ALARM = "StopAlarm;";

	// To phone
	static const MSG_START_TRACKING = "STARTING";
	static const MSG_CONFIRMCHECK = "CONFIRMCHECK";
	static const MSG_SNOOZE_ALARM = "SNOOZE";
	static const MSG_DISMISS_ALARM = "DISMISS";
	static const MSG_STOP_TRACKING = "STOPPING";
	static const MSG_PAUSE_TRACKING = "PAUSE";
	static const MSG_RESUME_TRACKING = "RESUME";
	static const MSG_DATA = "DATA_NEW";
	static const MSG_HR = "HR";
	
    const MAX_DELIVERY_ERROR = 3;
    const MAX_DELIVERY_PAUSE = 3;

    function initialize(ctx) {
        DebugManager.log("CommManager initialized");
        
        self.ctx = ctx;
    }
    
    public function start() {
        Communications.registerForPhoneAppMessages(method(:handleMessageReceived));
        self.queue = new MessageQueue();
        self.commListener = new CommListener(self.queue, self.ctx);
        
        enqueue(CommManager.MSG_START_TRACKING);
    }
    
    
    public function enqueue(msg) {
    	DebugManager.log("CommManager enqueue " + msg);
    	self.queue.enqueue(msg);
    	DebugManager.log("CommManager enqueue, current queue: " + self.queue.queue);
    }
    
    public function enqueueAsFirst(msg) {
    	DebugManager.log("CommManager enqueueAsFirst " + msg);
    	self.queue.enqueueAsFirst(msg);
    	DebugManager.log("CommManager enqueueAsFirst, current queue: " + self.queue.queue);    
    }
    
    public function triggerSend() {
    	DebugManager.log("Comm TriggerSend, inprogress: " + self.ctx.state.deliveryInProgress);
    	if (self.ctx.state.deliveryInProgress) { return; }
    	
    	if (self.ctx.state.deliveryErrorCount > MAX_DELIVERY_ERROR) {
    		self.ctx.state.deliveryPauseCount++;
    		
    		if (self.ctx.state.deliveryPauseCount > MAX_DELIVERY_PAUSE) {
    			self.ctx.state.deliveryPauseCount = 0;
    			self.ctx.state.deliveryErrorCount = 0;
    		}
    		
    		return;
    	}
    	
    	
    	var msg = self.queue.getFirst();
    	DebugManager.log("First msg: " + msg);
    	if (msg != null) {
	    	self.ctx.state.deliveryInProgress = true;
    		DebugManager.log("CommManager transmit: " + msg);
	    	Communications.transmit(msg, {}, self.commListener);
    	}
    }

    // msg is of type PhoneAppMessage
    function handleMessageReceived(message) {
    	var msg = message.data;
    
        DebugManager.log("Msg received: " + msg);

		if (msg.equals(CommManager.MSG_START)) {
			self.ctx.businessManager.startTracking();
			return;
		}
        
		if (msg.equals(CommManager.MSG_STOP)) {
			self.ctx.businessManager.exit();
			return;
		}
		
		if (msg.equals(CommManager.MSG_CHECK)) {
			self.ctx.businessManager.confirmConnection();
			return;
		}
		
		if (msg.find(CommManager.MSG_BATCH_SIZE) == 0) {
			var size = extractDataFromIncomingMessage(msg).toNumber();
			self.ctx.businessManager.setBatchSize(size);
			return;
		}
		
		if (msg.find(CommManager.MSG_HINT) == 0) {
			var hintRepeat = extractDataFromIncomingMessage(msg).toNumber();
			self.ctx.businessManager.doHint(hintRepeat);
			return;
		}
		
		if (msg.find(CommManager.MSG_SET_ALARM) == 0) {
			var alarmTime = extractDataFromIncomingMessage(msg).toLong();
			self.ctx.businessManager.setAlarmTime(alarmTime, true);
			return;
		}
		
		if (msg.find(CommManager.MSG_START_ALARM) == 0) {
			var delay = extractDataFromIncomingMessage(msg).toNumber();
			self.ctx.businessManager.startAlarm(delay);
			return;
		}
		
		if (msg.find(CommManager.MSG_STOP_ALARM) == 0) {
			self.ctx.businessManager.stopAlarm();
			return;
		}
		
    }
    
    function extractDataFromIncomingMessage(msg) {
    	if (msg.find(";") == null) { return null; }
    	
    	return msg.substring((msg.find(";"))+1, msg.length());
    }
}