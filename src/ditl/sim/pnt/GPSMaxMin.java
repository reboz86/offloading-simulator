package ditl.sim.pnt;

import java.util.*;

import ditl.graphs.Point;
import ditl.sim.*;

public class GPSMaxMin extends GPSWho {

	protected Map<Integer,PriorityQueue<Integer>> _closestNodes = new HashMap<Integer,PriorityQueue<Integer>>();
	
	@Override
	public Integer whoToPush(Message msg, Set<Integer> infected, Set<Integer> sane){
		if( infected.isEmpty() ){
			return RNG.randomFromSet(sane);
		}
		double maxD2 = Double.MIN_VALUE;
		Integer best = null;
		for( Integer i : sane ){
			Point ci = _lastPositions.get(i);
			if ( ci != null ){
				for ( Integer j : _closestNodes.get(i) ){
					if ( infected.contains(j) ){ // closest infected node to i
						Point cj = _lastPositions.get(j);
						if ( cj != null ){
							double d2 = dist2(ci,cj);
							if ( d2 > maxD2 ){
								maxD2 = d2;
								best = i;
								break;
							}
						}
					}
				}
			}
		}
		return best;
	}
	
	@Override
	protected void removeNode(Integer i){
		_closestNodes.remove(i);
		for ( PriorityQueue<Integer> closest : _closestNodes.values() ){
			closest.remove(i);
		}
		_lastPositions.remove(i);
	}
	
	@Override
	protected void updateNode(Integer i, Point c){
		// first remove ourself completely
		removeNode(i);
		// then update our information
		_lastPositions.put(i, c);
		PriorityQueue<Integer> closest = new PriorityQueue<Integer>(11,new DistanceComparator(c));		
		for ( Integer j : _lastPositions.keySet() ){ // update our list
			closest.add(j);
		}
		
		// finally update other lists
		for ( PriorityQueue<Integer> otherClosest : _closestNodes.values() ){
			otherClosest.add(i);
		}
		_closestNodes.put(i, closest);
	}
	
	private class DistanceComparator implements Comparator<Integer> {
		
		private Point _ref;
		
		public DistanceComparator(Point ref){
			_ref = ref;
		}
		
		@Override
		public int compare(Integer i1, Integer i2) {
			if ( _ref == null ) // no known reference positions => no way to evaluate distances
				return 0;
			Point c1 = _lastPositions.get(i1);
			if ( c1 == null ) // no last position for i1, i2 has priority
				return 1;
			Point c2 = _lastPositions.get(i2);
			if ( c2 == null ) // no last positions for i2, i1 has priority
				return -1;
			double d1 = dist2(_ref,c1);
			double d2 = dist2(_ref,c2);
			if ( d1 < d2 )
				return -1;
			if ( d1 > d2 )
				return 1;
			return 0;
		}
	
	}
}
