package ditl.sim.pnt;

import java.util.*;

import ditl.sim.*;

public class PrioOldest extends ArrivalWho {

	@Override
	public Integer whoToPush(Message msg, Set<Integer> infected, Set<Integer> sane){
		long oldestArrival = Long.MAX_VALUE;
		for( Integer i : sane ){
			if ( _arrivalTimes.get(i) < oldestArrival ){
				oldestArrival = _arrivalTimes.get(i);
			}
		}
		Set<Integer> oldest = new HashSet<Integer>();
		for( Integer i : sane ){
			if ( _arrivalTimes.get(i) == oldestArrival ){
				oldest.add(i);
			}
		}
		return RNG.randomFromSet(oldest);
	}
}
