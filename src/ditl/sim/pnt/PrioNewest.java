package ditl.sim.pnt;

import java.util.*;

import ditl.sim.*;

public class PrioNewest extends ArrivalWho {

	@Override
	public Integer whoToPush(Message msg, Set<Integer> infected, Set<Integer> sane){
		double newestArrival = Double.MIN_VALUE;
		for( Integer i : sane ){
			if ( _arrivalTimes.get(i) > newestArrival ){
				newestArrival = _arrivalTimes.get(i);
			}
		}
		Set<Integer> newest = new HashSet<Integer>();
		for( Integer i : sane ){
			if ( _arrivalTimes.get(i) == newestArrival ){
				newest.add(i);
			}
		}
		return RNG.randomFromSet(newest);
	}
}
