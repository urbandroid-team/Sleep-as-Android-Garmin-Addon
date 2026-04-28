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

		var med = median(beatIntervalArray);
		if (med == null || med == 0) { return null; }
		
		return 60000 / med; // median beat interval in one minute
	}
	
	static function computeMaxRawFromArray(xArr, yArr, zArr) {
		var size = xArr.size();
		if (size == 0) { 
			return null; // Better than 0, so the caller knows there was no data
		}
	
		var maxSquaredMag = 0.0;

		for (var i = 0; i < size; i++) { // Start at 0!
			var x = xArr[i];
			var y = yArr[i];
			var z = zArr[i];

			// Square the values directly (much faster than Math.pow)
			var currentSquaredMag = (x * x) + (y * y) + (z * z);

			if (currentSquaredMag > maxSquaredMag) {
				maxSquaredMag = currentSquaredMag;
			}
		}

	    // Only do the expensive square root ONCE for the final winner
    	return Math.sqrt(maxSquaredMag);
	}
	
	static function max(arr) {
		var size = arr.size();
		
		// 1. Handle empty array
		if (size == 0) { 
			return null; // Better than 0, so the caller knows there was no data
		}
		
		// 2. Initialize with the first element (handles negative numbers)
		var maxVal = arr[0];
		
		// 3. Start loop from the second element
		for (var i = 1; i < size; i++) {
			var current = arr[i];
			if (current > maxVal) { 
				maxVal = current; 
			}
		}
		
		return maxVal;
	}


	static function median(arr) {
	
		if (arr.size() == 0) { return -1; }
		if (arr.size() == 1) { return arr[0]; }
		DataUtil.sorti_asc(arr);
		
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

	static function sorti_asc(array) {
    var n = array.size();
    for (var i = 1; i < n; i++) {
        var key = array[i];
        var j = i - 1;
        while (j >= 0 && array[j] > key) {
            array[j + 1] = array[j];
            j--;
        }
        array[j + 1] = key;
    }
}
}