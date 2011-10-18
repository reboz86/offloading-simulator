package ditl.sim.pnt;

import java.util.*;

import ditl.sim.*;

public class PrioAverage extends ArrivalWho {

	@Override
	public Integer whoToPush(Message msg, Set<Integer> infected, Set<Integer> sane){
		double avg = getAverageArrival();
		double bestInt = Double.MAX_VALUE;
		double closestTime = Double.NaN;
		for( Integer i : sane ){
			if ( Math.abs(_arrivalTimes.get(i)-avg) < bestInt ){
				closestTime = _arrivalTimes.get(i);
				bestInt = Math.abs(_arrivalTimes.get(i)-avg);
			}
		}
		Set<Integer> closest = new HashSet<Integer>();
		for( Integer i : sane ){
			if ( _arrivalTimes.get(i) == closestTime ){
				closest.add(i);
			}
		}
		return RNG.randomFromSet(closest);
	}
	
	private double getAverageArrival(){
		long num = 0;
		for ( Long arr : _arrivalTimes.values() )
			num += arr;
		return (double)num/((double)_arrivalTimes.size());
	}
}
