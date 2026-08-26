using Toybox.Time;
using Toybox.Math;
using Toybox.Lang;

class DateUtil {
	static function convertMsTsToMoment(timestampMs) {
		var sec = (timestampMs / 1000).toLong();
		return new Time.Moment(sec);
	}
	
	static function momentToHHMM(moment) {
//		DebugManager.log("momentToHHMM");
		var date = Time.Gregorian.info(moment, Time.FORMAT_SHORT);
//		DebugManager.log("momentToHHMM date.hour: " + date.hour);
		return date.hour + ":" + date.min.format("%02d");
	}
	
	static function momentToDDDHHMM(moment) {
		var info = Time.Gregorian.info(moment, Time.FORMAT_MEDIUM);
		return info.day_of_week + " " + info.hour + ":" + info.min.format("%02d");
	}

	static function msTimestampToHHMM(timestampMs) {
		if (timestampMs instanceof Lang.Long) {
			DebugManager.log("msTimestampToHHMM: " + timestampMs);
			var mom = DateUtil.convertMsTsToMoment(timestampMs);
			return DateUtil.momentToHHMM(mom);
		} else {		
			DebugManager.log("msTimestampToHHMM NOT: " + timestampMs);
			return "---";
		}
	}

	// Converts ms timestamp to "Day HH:MM" (e.g., "Sat 14:27")
    static function msTimestampToDDDHHMM(timestampMs) {
        if (timestampMs instanceof Lang.Long) {
			var mom = DateUtil.convertMsTsToMoment(timestampMs);
			return DateUtil.momentToDDDHHMM(mom);
        } else {
            return "---";
        }
    }
}