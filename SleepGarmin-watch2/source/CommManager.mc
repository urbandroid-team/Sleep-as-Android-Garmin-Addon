using Toybox.Communications;
using Toybox.System;
using Toybox.Lang;
using Toybox.Application;

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
	static const MSG_STOP_ALARM = "StopAlarm";

	// To phone via TRANSMIT
	static const MSG_START_TRACKING = "STARTING";
	static const MSG_CONFIRMCHECK = "CONFIRMCHECK";

	// To phone via HTTP
	static const MSG_SNOOZE_ALARM = "SNOOZE";
	static const MSG_DISMISS_ALARM = "DISMISS";
	static const MSG_STOP_TRACKING = "STOPPING";
	static const MSG_PAUSE_TRACKING = "PAUSE";
	static const MSG_RESUME_TRACKING = "RESUME";
	static const MSG_DATA = "DATA_NEW";
	static const MSG_HR = "HR";
	static const MSG_OXY = "SPO2";
	static const MSG_RR = "RR";
	static const MSG_ERROR = "ERROR";
	
    const MAX_DELIVERY_ERROR = 3;
    const MAX_DELIVERY_PAUSE = 3;
    const MAX_WAITING_TIME_IN_TRANSMIT_MS = 60000;

	const MINIMAL_POLL_INTERVAL_MS = 30000;
	const AROUND_ALARM_POLL_INTERVAL_MS = 1000;
	var lastSendTriggerTs = 0;

	const WEB_URL = "http://127.0.0.1:1765";

    function initialize(ctx) {
        DebugManager.log("CommManager initialized");
        
        self.ctx = ctx;
		self.queue = new MessageQueueWithStorage();
    }
    
    public function start() {
		Communications.registerForPhoneAppMessages(method(:onPhoneMsgReceive));

		// if (Toybox.Application has :Storage) {
		// } else {
			// self.queue = new MessageQueue();
		// }
        self.commListener = new CommListener(self.queue, self.ctx);
        
        enqueue(CommManager.MSG_START_TRACKING);
        
        if (DebugManager.commDebug) {
	        var msg = new Communications.PhoneAppMessage();
			msg.data = CommManager.MSG_START;
	        onPhoneMsgReceive(msg);
	        
	        msg.data = CommManager.MSG_BATCH_SIZE + "12";
	        onPhoneMsgReceive(msg);
	        
	        // var msg2 = new Communications.PhoneAppMessage();
			// msg2.data = CommManager.MSG_START_ALARM + "0";
	        // onPhoneMsgReceive(msg2);
        }
    }
        
    public function enqueue(msg) {
    	DebugManager.log("CommManager enqueue " + msg);
    	self.queue.enqueue(msg);
    	DebugManager.log("CommManager enqueue, current queue: " + self.queue.showCurrentQueue());
    }
    
    public function enqueueAsFirst(msg) {
    	DebugManager.log("CommManager enqueueAsFirst " + msg);
    	self.queue.enqueueAsFirst(msg);
    	DebugManager.log("CommManager enqueueAsFirst, current queue: " + self.queue.showCurrentQueue());    
    }

    public function triggerSend() {
		doTriggerSend();
		lastSendTriggerTs = System.getTimer();
	}
	    
    public function doTriggerSend() {
    	DebugManager.log("CommManager#doTriggerSend, inprogress: " + self.ctx.state.deliveryInProgress);

    	if (self.ctx.state.deliveryInProgress && !isDeliveryTakingTooLong()) {
			return; 
    	}
    	
    	if (self.ctx.state.deliveryErrorCount > MAX_DELIVERY_ERROR) {
    		DebugManager.log("Max delivery error");
    		self.ctx.state.deliveryPauseCount++;
    		
    		if (self.ctx.state.deliveryPauseCount > MAX_DELIVERY_PAUSE) {
    			self.ctx.state.deliveryPauseCount = 0;
    			self.ctx.state.deliveryErrorCount = 0;
    		}

    		return;
    	}
    	
    	var msg = self.queue.getFirst();
    	// DebugManager.log("First msg: " + msg);
    	if (msg != null) {
	    	self.ctx.state.deliveryInProgress = true;
    		DebugManager.log("CommManager#doTriggerSend msg: " + msg);
    		self.ctx.state.lastDeliveryTs = System.getTimer();
    		
    		if (DebugManager.commDebug) {
    			DebugManager.log("NOT Transmitted");
	   			// self.commListener.onComplete();
				self.commListener.onError();
				return;
			}

			if (msg.equals(CommManager.MSG_START_TRACKING) || msg.equals(CommManager.MSG_CONFIRMCHECK)) {
				Communications.transmit(msg, {}, self.commListener);		
			} else {
				var messageToPhone = new MessageToPhone(msg);
				var req = messageToPhone.toRequest();
    			DebugManager.log("CommManager#doTriggerSend webRequest:" + req);
				pollWebserver(req);
			}

			return;
    	} 

		if (msg == null) {
			if (self.ctx.businessManager.isAroundAlarm() && (System.getTimer() - lastSendTriggerTs) > AROUND_ALARM_POLL_INTERVAL_MS) {
				pollWebserver(new MessageToPhone("quickPollBeforeAlarm").toRequest());
				return;
			}

			if ((System.getTimer() - lastSendTriggerTs) > MINIMAL_POLL_INTERVAL_MS) {
				pollWebserver(new MessageToPhone("poll").toRequest());
				return;
			}
		}
    }

	function isDeliveryTakingTooLong() {
		return (System.getTimer() - self.ctx.state.lastDeliveryTs) < MAX_WAITING_TIME_IN_TRANSMIT_MS;
	}

	function pollWebserver(req) {
		if (req == null || WEB_URL == null || method(:onWebMsgReceive) == null) {
			return;
		}
		Communications.makeWebRequest(
			WEB_URL, 
			req,
			{:method => Communications.HTTP_REQUEST_METHOD_GET}, 
			method(:onWebMsgReceive)
		);
	}

	function onPhoneMsgReceive(phoneAppMessage) {
		if (phoneAppMessage instanceof Communications.PhoneAppMessage) {
			try {
				handleMessageReceived(phoneAppMessage.data);
			} catch (ex) {
				DebugManager.log("Error in handleMessageReceived: " + ex);
			}
		} else {
			DebugManager.log("Invalid phoneAppMessage: " + phoneAppMessage);
		}
	}

	function onWebMsgReceive(responseCode, data) {
       if (responseCode == 200) {
        	DebugManager.log("onWebMsgReceive Request Successful: " + data);
			self.commListener.onComplete();

			try {
				// Expecting to receive a JSON string
				var msgArray = parseJsonDataToArray(data);

				for ( var i = 0; i < msgArray.size(); i += 1 ) {
					handleMessageReceived(msgArray[i]);
				}
			} catch (e instanceof Toybox.Lang.UnexpectedTypeException) {
				enqueueAsFirst([CommManager.MSG_ERROR, "UnexpectedTypeException"]);
			}

       }
       else {
           DebugManager.log("onWebMsgReceive Response: " + responseCode);
		   self.commListener.onError();
       }
    }

	function parseJsonDataToArray(json) {
		DebugManager.log("parseJsonDataToArray" + json);
		// contract is be c:command, d:data (d is optional)
		var ar = [];
		if (json == null) {
			return ar;
		}

		for (var i = 0; i < json.size(); ++i) {
			var entry = json[i];
			var command = entry["c"];
			var param = entry["d"];

			if (param == null) {
				ar.add(command);
			} else {
				ar.add(command + ";" + param);
			}
		}

		return ar;
	}

    function handleMessageReceived(msg) {
        DebugManager.log("handleMessageReceived: " + msg);
		self.ctx.businessManager.logTransmit("CommManager#handleMessageReceived: " + msg);

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
			self.ctx.businessManager.logTransmit("CommManager#received MSG_STOP_ALARM");

			self.ctx.businessManager.stopAlarm();
			return;
		}
		
    }
    
    function extractDataFromIncomingMessage(msg) {
    	if (msg.find(";") == null) { return null; }
    	
    	return msg.substring((msg.find(";"))+1, msg.length());
    }
}