using Toybox.Application as App;
using Toybox.WatchUi as Ui;
using Toybox.Sensor as Sensor;
using Toybox.Timer as Timer;
using Toybox.Time as Time;
using Toybox.Communications as Comm;
using Toybox.Attention as Attention;
using Toybox.System as Sys;
using Toybox.Time.Gregorian as Calendar;
using Toybox.Math as Math;

// Globals

    var debug = true; var fakeTransmit = false; var beta = false; var debugAlarm = false;
    var notice = "";

    var dataTimer;
    // var secondTimer;
    // var messageTime = 0;
    // const messageLoop = 2000;
    var current_heartrate = 0;

    var now;
    var timecurrent = 0;
    var startTimestamp = 0;
    var logtimestamp;

    var messageQueue = [];
    var deliveryInProgress = false;
    var deliveryErrorCount = 0;
    var deliveryPauseCount = 0;
    const MAX_DELIVERY_ERROR = 3;
    const MAX_DELIVERY_PAUSE = 20;
    var lastMessageReceived = 0;

    var alarm_currently_active = false; // Is alarm currently ringing on phone?
    var exitTapped = false;
    var alarmViewActive = false;

    var trackingBool = false;
    var stoppingBool = false;
    var targetExitTime = 0;
    var shouldExit = false;
    

    function exitTimer(afterCycles) {
		targetExitTime = afterCycles;
    	shouldExit = true;
    }

    // Logs into the /GARMIN/APPS/LOGS/appname.TXT
    // The file has to be created manually first. It is not possible to gather debug logs in production (after distribution in the ConnectIQ store)
    function log(a) {
        if (debug == true) {
            logtimestamp = Time.now().value() - startTimestamp;
            Sys.println(logtimestamp + ": " + a);
        }
    }

    function betalog(a) {
        if (beta == true) {
            logtimestamp = Time.now().value() - startTimestamp;
            Sys.println(logtimestamp + ": " + a);
        }
    }

    // Puts message in the messageQueue, also attempts to do some memory checks so as not to overload the underlying watch's queue
    function enqueue(message) {
        var freeMemRatio = Sys.getSystemStats().freeMemory*100/Sys.getSystemStats().totalMemory;
        log("free: " + Sys.getSystemStats().freeMemory + " freeRatio:" + freeMemRatio);

        // if (messageQueue.size() > 50) {
        //    log("MsgQ > 50!!!");
        // }

        if (((freeMemRatio <= 7) && (messageQueue.size() > 0)) || (messageQueue.size() > 50)) {
            log("Rem from q, freeRatio:" + freeMemRatio + ",q:" + messageQueue.size());
            messageQueue.remove(messageQueue[0]);
        }


        if (messageQueue.indexOf(message) == -1) {
            messageQueue.add(message);
            // log("Adding to q:" + message + " outQ: " + messageQueue);
        }
    }

    // Convenience global functions to enqueue specific messages to be sent to phone
    function sendResumeTracking() {
        enqueue("RESUME");
    }

    function sendPauseTracking() {
        enqueue("PAUSE");
    }

    function sendStopTracking() {
    	stoppingBool = true;
        enqueue("STOPPING");
    }

    function sendDismissAlarm() {
        enqueue("DISMISS");
    }

    function sendSnoozeAlarm() {
        enqueue("SNOOZE");
        stopAlarm();
    }

    function sendConfirmConnection() {
        enqueue("CONFIRMCHECK");
    }

    function stopAlarm() {
        alarm_currently_active = false;
        Ui.switchToView(new SleepMainView(), new SleepMainDelegate(), Ui.SLIDE_IMMEDIATE);
        alarmViewActive = false;
    }

    function normalExit(){
    	log("Normal Exit");
    	stoppingBool = true;
    	if (Sys.getDeviceSettings().phoneConnected && !fakeTransmit) {
                Comm.transmit("STOPPING", null, new SleepNowListener("STOPPING"));
		} else if (!Sys.getDeviceSettings().phoneConnected) {
				exitTimer(20);
		} else if ( lastMessageReceived == 0 || ((lastMessageReceived - Time.now().value()) > 30000)) {
				log("Possible failure_during_transfer");
				exitTimer(20);
    }

    function forceExit(){
    	log("Force Exit");
    	stoppingBool = true;
		if (Sys.getDeviceSettings().phoneConnected && !fakeTransmit) {
        	Comm.transmit("STOPPING", null, new SleepNowListener("STOPPING"));
		}
		exitTimer(20);
    }




class SleepApp extends App.AppBase {

    const SAMPLE_PERIOD = 100; //ms
    const AGG_PERIOD = 10000; //ms
    const MAX_AGG_COUNT = AGG_PERIOD/SAMPLE_PERIOD;

    var info;
    var hrInfo;
    var listener;
    var connectIQVersion;

    var scheduled_alarm_ts = 0;
    var delay = 0; // For delayed alarm vibration

    // Heart rates
    const HR_ON_COUNT = 120;//120;
    const HR_MAX_COUNT = 360;//360;
    var hrCurrentlyReading = false;
    var hrTracking = false;
    var hrValue = 0;
    var hrCount = 0;

    var timerCount = 0;
    var stopAppDelay = 0;

    // Alarms
    var alarm_gap_duration = 300; // Time between alarm pulses
    var alarm_gap_duration_current = alarm_gap_duration;
    var alarmCount = alarm_gap_duration;
    // Vibration patterns
    var vibrateOnAlarm = true;
    const shortPulse = [new Attention.VibeProfile(50, 200)];

    // Actigraphy
    var last = false;
    var lastValues = new [3];
    var max_sum = 0;
    var currentMax;
    var aggCount = 0;
    var batchSize = 12;
    var batch = [];
    // New actigraphy
    var batch_new = [];
    var max_sum_new = 0;


    function initialize() {
        AppBase.initialize();
        startTimestamp = Time.now().value();

        if(Comm has :registerForPhoneAppMessages) {
        	log("registerForPhoneAppMessages");
            Comm.registerForPhoneAppMessages( method(:onMsg) );
        } else {
            notice = notice + "Err: Old CIQ version\n";
        }

        connectIQVersion = Sys.getDeviceSettings().monkeyVersion[0];
    }

    function onMsg(msg) {
    	//log("onMsg Called");
        handleIncomingMessage(msg.data.toString());
    }

    //! onStart() is called on application start up
    function onStart(state) {
        log("--onStart--");
        dataTimer = new Timer.Timer();
		dataTimer.start( method(:timerCallback), SAMPLE_PERIOD, true);

        sendStartingTracking();
        now = Sys.getClockTime();
        timecurrent = now.hour + ":" + now.min.format("%02d");
        // current_heartrate = (Sensor.getInfo()).heartRate;
        Ui.requestUpdate();

        // Just for emulator
        if (fakeTransmit == true) { notice = notice + "fakeTransmit";}

    }

    // Main timer loop - here we gather data from sensors, check for alarms and ring them, and send messages to phone
    function timerCallback() {
    	//log("TimerCallback");
        now = Sys.getClockTime();
        timecurrent = now.hour + ":" + now.min.format("%02d");
        if (now.sec == 0) {
            Ui.requestUpdate();
        }

        if (shouldExit) {
        	if (targetExitTime == 0) {
       		 	Sys.exit();
    		}
        	targetExitTime = targetExitTime - 1;
    	}

        info = Sensor.getInfo();

        timerCount++;

        if (stopAppDelay < 5) {
            stopAppDelay++;
        }
        if (timerCount % 10 == 0) {
            // log("timerCallback");
            gatherHR(info);
            timerCount = 0;
        }

        gatherData(info);

        if (alarm_currently_active) {
            alarmCount++;
            if (delay <= 0) {
                if (alarmViewActive != true) {
                    Ui.switchToView(new SleepAlarmView(), new SleepAlarmDelegate(), Ui.SLIDE_IMMEDIATE);
                }
                if (alarmCount >= alarm_gap_duration_current) {
                    ringAlarm();
                    alarm_gap_duration = Math.floor(alarm_gap_duration/2);
                    if (alarm_gap_duration < 10) {
                        alarm_gap_duration = 10;
                    }
                    alarm_gap_duration_current = alarm_gap_duration;
                    alarmCount = 0;
                }
            } else {
                delay = delay - SAMPLE_PERIOD;
            }
        } else {
            checkIfAlarmScheduledForNow();
        }
        sendNextMessage();
    }

    // MESSAGES TO PHONE
    // These messages are not needed globally
    function sendStartingTracking() {
        enqueue("STARTING");
    }
    function sendCurrentDataAndResetBatch() {
        var toSend = ["DATA", batch.toString()];
        var toSend_new = ["DATA_NEW", batch_new.toString()];
        if (batch.size() > 0) {
            enqueue(toSend);
            enqueue(toSend_new);
            //batch = null;
            //batch_new = null;
            batch = [];
            batch_new = [];
        }
        // log("transmitting: " + batch.toString());
    }
    function sendHRData(hrAvg) {
        var HRtoSend = ["HR", hrAvg];
        enqueue(HRtoSend);
    }

    // Handling messages coming from the phone
    function handleIncomingMessage(mail) {
        var data;
        log("In: " + mail);
        lastMessageReceived = Time.now().value();

        if ( mail.equals("StopApp") && stopAppDelay == 5) {
        	stopAlarm();
			exitTimer(22);
        } else if ( mail.equals("Check") ) {
            sendConfirmConnection();
        } else if ( mail.find("Pause;") == 0 ) {
            // Currently doing nothing when pause received from phone
            data = extractDataFromIncomingMessage(mail).toNumber();  // time
            // enqueue(data);
            // TODO extract value and pause tracking (start sending -0.01s) and show pause time
        } else if ( mail.find("BatchSize;") == 0 ) {
            data = extractDataFromIncomingMessage(mail).toNumber(); // size
            setBatchSize(data);
        } else if ( mail.find("SetAlarm;") == 0 ) {
            data = extractDataFromIncomingMessage(mail).toNumber(); // timestamp
            setAlarm(data);
        } else if ( mail.find("StartAlarm;") == 0 ) {
            delay = extractDataFromIncomingMessage(mail).toNumber(); // delay
            if (delay == "-1") {
                vibrateOnAlarm = false;
                } else {
                    vibrateOnAlarm = true;
                }
            startAlarm();
        } else if ( mail.find("Hint;") == 0 ) {
            data = extractDataFromIncomingMessage(mail).toNumber();  // repeat
            doHint(data);
        } else if ( mail.find("StopAlarm;") == 0 ) {
            stopAlarm();
        } else if ( mail.equals("StartHRTracking")) {
    		Sensor.setEnabledSensors([Sensor.SENSOR_HEARTRATE]);
    		hrTracking = true;
        	hrCurrentlyReading = true;
        	if (!trackingBool) {
        		trackingBool = true;
        		Ui.requestUpdate();
        		log("tracking true");
    		}
        } else if ( mail.equals("StartTracking")) {
        	if (!trackingBool) {
        		trackingBool = true;
        		Ui.requestUpdate();
        		log("tracking true");
    		}
        } else {
            // mail = "Message not handled: " + mail;
            log("Msg fail" + mail.toString());
        }
    }

    function extractDataFromIncomingMessage(mail) {
        return mail.substring((mail.find(";"))+1,mail.length());
    }

    function gatherData(info) {
        if ( info has :accel && info.accel != null ) {
            store_max(info.accel); // saves to both max_sum and max_sum_new

            if ( aggCount >= MAX_AGG_COUNT ) {
                batch.add(max_sum);
                batch_new.add(max_sum_new);
                max_sum_new = 0;
                max_sum = 0;
                aggCount = 0;
            }

            if ( batch.size() >= batchSize ) {
                sendCurrentDataAndResetBatch();
            }

            aggCount++;
        }

    }

    function gatherHR(hrInfo) {
        if (hrTracking == true) {
        // log("has nonnull heartrate: " + hrInfo.heartRate);
            if ( hrInfo has :heartRate && hrInfo.heartRate != null ) {

                hrCount = hrCount + 1;
                // log(hrCount);

                if (hrCurrentlyReading == true) {
                    // log("hrinfo, heartrate" + hrInfo + " " +hrInfo.heartRate);
    	            hrValue = hrValue + hrInfo.heartRate;
                }

                if ( (hrCount >= HR_ON_COUNT) && (hrCurrentlyReading == true) ) {
 					// log("hrinfo, heartrate" + hrInfo + " " +hrInfo.heartRate);
    	            // log("switching off HR read");
                    hrCurrentlyReading = false;
                    // Sensor.setEnabledSensors([]); // disables heart rate sensor
                    sendHRData(hrValue/hrCount);
                    hrValue = 0;
                }

                if ( hrCount >= HR_MAX_COUNT) {
                    // log("hrloop restart");
                    hrCount = 0;
                    hrCurrentlyReading = true;
                }
            }
        }
    }

    // Batch can be any number but usually we set it either to 1 when the phone user is currently viewing the phone so he has data from watch sent to phone immediately for viewing, or to 12 when the phone is idle, to conserve battery (we don't have to send via bluetooth as often)
    function setBatchSize(newBatchSize) {
        log("Batch " + newBatchSize.toString());
        batchSize = newBatchSize;
        sendCurrentDataAndResetBatch();
    }

    function checkIfAlarmScheduledForNow() {
        if (now == scheduled_alarm_ts) {
            startAlarm();
        }
    }

    function setAlarm(timestamp) {
        log("Alarm " + timestamp.toString());
        scheduled_alarm_ts = timestamp;
    }

    function startAlarm() {
        alarm_currently_active = true;
        alarm_gap_duration = 300;
        alarm_gap_duration_current = 300;
        alarmCount = alarm_gap_duration;
    }

    function ringAlarm() {
        if ( vibrateOnAlarm == true ) {
            if (Attention has :vibrate ) {
                Attention.vibrate(shortPulse);
            } else {
                if (Attention has :playTone) {
                    Attention.playTone(8);
                }
            }
        }
    }

    // Hint is lucid dreaming or anti-snoring vibration
    function doHint(repeat) {
        // Garmin only supports vibrating up to 8 VibeProfiles, so we have to cap repeating on 4
        if (repeat > 4) {
            repeat = 4;
        }
        log("Doing HINT " + repeat.toString() + " times.");

        if (Attention has :vibrate) {
            var vibrateData = [];
            for ( var i = 0; i < repeat; i += 1) {
                vibrateData.add(new Attention.VibeProfile(  50, 1000));
                vibrateData.add(new Attention.VibeProfile(  0, 1000));
            }
            Attention.vibrate(vibrateData);
        } else if (Attention has :playTone) {
            for ( var i = 0; i < repeat; i += 1) {
                Attention.playTone(0); // playing TONE_KEY
            }
        }
    }



    function sendNextMessage() {
        if (deliveryErrorCount > MAX_DELIVERY_ERROR) {
            deliveryPauseCount++;
            if (deliveryPauseCount > MAX_DELIVERY_PAUSE) {
                deliveryPauseCount = 0;
                deliveryErrorCount = 0;
            }
        } else if (messageQueue.size() > 0 && !deliveryInProgress && Sys.getDeviceSettings().phoneConnected) {
                var message = messageQueue[0];
                deliveryInProgress = true;
                if (fakeTransmit == true) {
                	log("FakeTransmit: " + message);
                	new SleepListener(message).onComplete();
                } else {
              		log("Send " + message.toString().substring(0,5) + "...");
	                Comm.transmit(message, null, new SleepListener(message));
                }
        }
    }

    function store_max(currentValues) {

        if (last) {
        	//log("x" + currentValues[0] + "y" + currentValues[1] + "z" + currentValues[2]);
            var sum = ((lastValues[0] - currentValues[0]).abs() + (lastValues[1] - currentValues[1]).abs() + (lastValues[2] - currentValues[2]).abs());
            var sum_new = Math.floor(Math.sqrt((currentValues[0] * currentValues[0]) + (currentValues[1] * currentValues[1]) + (currentValues[2] * currentValues[2]))).toNumber();

            if (sum > max_sum) {
                max_sum = sum;
            }
            if (sum_new > max_sum_new) {
                max_sum_new = sum_new;
            }

        }

        last = true;
        lastValues[0] = currentValues[0];
        lastValues[1] = currentValues[1];
        lastValues[2] = currentValues[2];
    }

    //! onStop() is called when your application is exiting
    function onStop(state) {
		log("onStop");
        dataTimer.stop();
        // messageQueue = null;
        betalog("usedMem" + Sys.getSystemStats().usedMemory + "freeMem" + Sys.getSystemStats().freeMemory + "totalMem" + Sys.getSystemStats().totalMemory);
		// messageQueue = null;

    }

    //! Return the initial view of your application here
    function getInitialView() {
        log("getInitialView");
        if (debugAlarm) {
        	return [ new SleepAlarmView(), new SleepAlarmDelegate() ];
        }
        return [ new SleepMainView(), new SleepMainDelegate() ];
    }
}