using Toybox.Time;
using Toybox.Math;
using Toybox.Lang;

class DateUtil {
	static function convertMsTsToMoment(timestampMs) {
//		DebugManager.log("convertMsTsToMoment: " + timestampMs);
			var mom = new Time.Moment(Math.floor(timestampMs/1000));
			return mom;
	}
	
	static function momentToHHMM(moment) {
//		DebugManager.log("momentToHHMM");
		var date = Time.Gregorian.info(moment, Time.FORMAT_SHORT);
//		DebugManager.log("momentToHHMM date.hour: " + date.hour);
		return date.hour + ":" + date.min.format("%02d");
	}
	
	static function msTimestampToHHMM(timestampMs) {
		if (timestampMs instanceof Lang.Long) {
			DebugManager.log("msTimestampToHHMM: " + timestampMs);
			var mom = DateUtil.convertMsTsToMoment(timestampMs);
			return DateUtil.momentToHHMM(mom);
		} else {		
			DebugManager.log("msTimestampToHHMM NOT: " + timestampMs);
			return "??";
		}
	}
}