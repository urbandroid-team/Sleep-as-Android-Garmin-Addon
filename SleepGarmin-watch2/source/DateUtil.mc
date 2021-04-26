using Toybox.Time;
using Toybox.Math;
using Toybox.Lang;

class DateUtil {
	static function convertMsTsToMoment(timestampMs) {
//		DebugManager.log("convertMsTsToMoment: " + timestampMs);
		try {
			var mom = new Time.Moment(Math.floor(timestampMs/1000));
			return mom;
		} catch( ex instanceof Lang.UnexpectedTypeException) {
			return timestampMs;
		}
	}
	
	static function momentToHHMM(moment) {
//		DebugManager.log("momentToHHMM");
		var date = Time.Gregorian.info(moment, Time.FORMAT_SHORT);
//		DebugManager.log("momentToHHMM date.hour: " + date.hour);
		return date.hour + ":" + date.min.format("%02d");
	}
	
	static function msTimestampToHHMM(timestampMs) {
//		DebugManager.log("msTimestampToHHMM: " + timestampMs);
		try {
			var mom = DateUtil.convertMsTsToMoment(timestampMs);
//			DebugManager.log("msTimestampToHHMM, mom ready");
			return DateUtil.momentToHHMM(mom);
		} catch( ex instanceof Lang.UnexpectedTypeException ) {
			return timestampMs;
		}
	}
}