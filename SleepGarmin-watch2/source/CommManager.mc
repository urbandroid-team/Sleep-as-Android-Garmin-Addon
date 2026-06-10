using Toybox.Communications;
using Toybox.System;
using Toybox.Lang;
using Toybox.Application;

class CommManager {

    private var ctx;
    private var queue;
    private var commListener;

    // Cache method references as instance variables to prevent OOM runtime leaks
    private var webMsgCallback;
    private var phoneMsgCallback;

	// From phone
	static const MSG_START = "StartTracking";
	static const MSG_START_HR = "StartHRTracking";
	static const MSG_START_OXI = "StartOxiTracking";
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
	
    const MAX_DELIVERY_ERROR = 10;
    const MAX_DELIVERY_PAUSE = 5;
    const MAX_WAITING_TIME_IN_TRANSMIT_MS = 5000;

	const MINIMAL_POLL_INTERVAL_MS = 2500;
	const AROUND_ALARM_POLL_INTERVAL_MS = 1000;
	var lastSendTriggerTs = 0;

	const WEB_URL = "http://127.0.0.1:1765";

    function initialize(ctx) {
        DebugManager.log("CommManager initialized");
        
        self.ctx = ctx;
		self.queue = new MessageQueue(50);

		self.webMsgCallback = method(:onWebMsgReceive);
        self.phoneMsgCallback = method(:onPhoneMsgReceive);
    }
    
    public function start() {
        Communications.registerForPhoneAppMessages(self.phoneMsgCallback);
        self.commListener = new CommListener(self.queue, self.ctx);
        enqueue(CommManager.MSG_START_TRACKING);
    }

    public function stop() {
        Communications.registerForPhoneAppMessages(null);
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
        var currentTimer = System.getTimer();

        if (self.ctx.state.deliveryInProgress && !isDeliveryTakingTooLong(currentTimer)) {
            return;
        }

        if (self.ctx.state.deliveryErrorCount > MAX_DELIVERY_ERROR) {
            self.ctx.state.deliveryPauseCount++;
            if (self.ctx.state.deliveryPauseCount > MAX_DELIVERY_PAUSE) {
                self.ctx.state.deliveryPauseCount = 0;
                self.ctx.state.deliveryErrorCount = 0;
            }
            return;
        }

        var msg = self.queue.getFirst();
        if (msg != null) {
            self.ctx.state.deliveryInProgress = true;
            self.ctx.state.lastDeliveryTs = currentTimer;

            if (msg.equals(CommManager.MSG_START_TRACKING) || msg.equals(CommManager.MSG_CONFIRMCHECK)) {
                Communications.transmit(msg, {}, self.commListener);
            } else {
                var messageToPhone = new MessageToPhone(msg);
                pollWebserver(messageToPhone.toRequest());
            }
            return;
        }

        // Empty processing fallback - Execute polling routines cleanly
        var timeDelta = (currentTimer - lastSendTriggerTs).abs(); // Robust numeric math calculation
        if (self.ctx.businessManager.isAroundAlarm()) {
            if (timeDelta > AROUND_ALARM_POLL_INTERVAL_MS) {
                pollWebserver(new MessageToPhone("quickPollBeforeAlarm").toRequest());
            }
        } else if (timeDelta > MINIMAL_POLL_INTERVAL_MS) {
            pollWebserver(new MessageToPhone("poll").toRequest());
        }
    }

    private function isDeliveryTakingTooLong(currentTimer) {
        return (currentTimer - self.ctx.state.lastDeliveryTs).abs() > MAX_WAITING_TIME_IN_TRANSMIT_MS;
    }

    private function pollWebserver(req) {
        if (req == null) { return; }

        Communications.makeWebRequest(
            WEB_URL,
            req,
            {:method => Communications.HTTP_REQUEST_METHOD_GET},
            self.webMsgCallback // Using cached method pointer safely
        );
    }

    public function onPhoneMsgReceive(phoneAppMessage as Communications.Message) as Void {
        if (phoneAppMessage instanceof Communications.PhoneAppMessage) {
            handleMessageReceived(phoneAppMessage.data);
        }
    }

    public function onWebMsgReceive(responseCode as Lang.Number, data as Lang.Dictionary<Lang.String, Lang.Object?> or Lang.String or Null) as Void {
       if (responseCode == 200) {
            self.commListener.onComplete();
            if (data == null) { return; }

            if (data instanceof Lang.Array) {
                var totalElements = data.size();
                for (var i = 0; i < totalElements; i++) {
                    var entry = data[i];
                    if (entry instanceof Lang.Dictionary) {
                        var command = entry["c"];
                        var param = entry["d"];
                        var compositeMsg = (param == null) ? command : (command + ";" + param);
                        handleMessageReceived(compositeMsg);
                    }
                }
            }
       } else {
           self.commListener.onError();
       }
    }

    private function handleMessageReceived(msg) {
        if (msg == null || !(msg instanceof Lang.String)) { return; }

        self.ctx.businessManager.logTransmit("CommManager: " + msg);
        self.ctx.businessManager.startTracking();

        // Optimized String Parsing sequence using fast early evaluations
        if (msg.equals(CommManager.MSG_START)) { return; }
        if (msg.equals(CommManager.MSG_START_HR)) { self.ctx.sensorManager.startHr(); return; }
        if (msg.equals(CommManager.MSG_START_OXI)) { self.ctx.sensorManager.startOxi(); return; }
        if (msg.equals(CommManager.MSG_STOP)) { self.ctx.businessManager.exit(); return; }
        if (msg.equals(CommManager.MSG_CHECK)) { self.ctx.businessManager.confirmConnection(); return; }
        
        // Parameterized commands block
        var semiColonIndex = msg.find(";");
        if (semiColonIndex != null) {
            var cmdPrefix = msg.substring(0, semiColonIndex + 1);
            var payloadData = msg.substring(semiColonIndex + 1, msg.length());

            if (cmdPrefix.equals(CommManager.MSG_BATCH_SIZE)) {
                var size = payloadData.toNumber();
                if (size > 0 && size < 20) { self.ctx.businessManager.setBatchSize(size); }
            } else if (cmdPrefix.equals(CommManager.MSG_SET_ALARM)) {
                var alarmTime = payloadData.toLong();
                if (alarmTime != 1) { self.ctx.businessManager.setAlarmTime(alarmTime, true); }
            } else if (cmdPrefix.equals(CommManager.MSG_START_ALARM)) {
                self.ctx.businessManager.startAlarm(payloadData.toNumber());
            } else if (cmdPrefix.equals(CommManager.MSG_HINT)) {
                var hintRepeat = payloadData.toNumber();
                if (hintRepeat != -1) { self.ctx.businessManager.doHint(hintRepeat); }
            }
            return;
        }

        if (msg.equals(CommManager.MSG_STOP_ALARM)) {
            self.ctx.businessManager.stopAlarm();
        }
    }
}