using Toybox.Math;
using Toybox.Lang;

function less_than(a, b) {
	return a < b;
}

class DataUtil {

	static function hrFromBeatIntervals(beatIntervalArray) {
		DebugManager.log("hrFromBeatIntervals");

		// check if array is empty or null
		if (beatIntervalArray == null || beatIntervalArray.size() == 0) { return null; }

		// check if array contains valid numbers
		for (var i = 0; i < beatIntervalArray.size(); i++) {
			var value = beatIntervalArray[i];
			if (value == null || value <= 0 || !Lang.isNumber(value)) {
				DebugManager.log("Invalid value: " + value);
				return null;
			}
		}

		var med = median(beatIntervalArray);
		if (med == null || med == 0) { return null; }
		
		return 60000 / med; // median beat interval in one minute
	}
	
	static function computeMaxRawFromArray(xArr, yArr, zArr) {
		var axisAggregates = [];
		
		for( var i = 0; i < xArr.size(); i += 1 ) {
			axisAggregates.add(Math.sqrt(Math.pow(xArr[i],2) + Math.pow(yArr[i],2) + Math.pow(zArr[i],2)));
		}
		
		return DataUtil.max(axisAggregates); 
	}
	
	static function max(arr) {
		if (arr.size() == 0) { return null; }
		
		var max = arr[0];
		for( var i = 1; i < arr.size(); i += 1 ) {
			if (arr[i] > arr[i-1]) { max = arr[i]; }
		}
		return max;
	}
	
	static function median(arr) {
	
		if (arr.size() == 0) { return null; }
		if (arr.size() == 1) { return arr[0]; }
		DataUtil.sort_asc(arr);
		
//		DebugManager.log("Median: arr.size " + arr.size() + " arr: " + arr.toString()); 
		
		return arr[Math.round(arr.size() / 2) - 1];
	}
	
	static function bubble_sort_aux(array, lo, hi, cmp) {
		var n = hi;
		
		do {
			var newn = 0;


			for (var i = lo + 1; i < n; i++) {

				if (cmp.invoke(array[i], array[i-1])) {
					var tmp = array[i-1];
					array[i-1] = array[i];
					array[i] = tmp;
		
					newn = i;
				}
			}
		
			n = newn;
		
		} while (n != 0);
	}

	static function bubble_sort(array, cmp) {
//    	DebugManager.log("bubble_sort");
		DataUtil.bubble_sort_aux(array, 0, array.size(), cmp);
	}
	
	static function sort_asc(array) {
//    	DebugManager.log("sort_asc");	
		DataUtil.bubble_sort(array, new Lang.Method($, :less_than));
	}
}