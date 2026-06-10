using Toybox.Math;
using Toybox.Lang;

function less_than(a, b) {
	return a < b;
}

class DataUtil {

	static function abs(val) {
    	return val < 0 ? -val : val;
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



}