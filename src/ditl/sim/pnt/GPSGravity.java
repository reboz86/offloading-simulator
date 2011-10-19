package ditl.sim.pnt;

import java.util.*;

import ditl.sim.*;

public class GPSGravity extends GPSWho {
		
	@Override
	public Integer whoToPush(Message msg, Set<Integer> infected, Set<Integer> sane){
		if ( _mainZone == null ){
			return RNG.randomFromSet(sane);
		}
		
		// first get barycenters for whole BH Tree
		setBarycenter(_mainZone,infected);
		
		Set<Zone> seenZones = new HashSet<Zone>();
		PriorityQueue<ScoreCard> scores = new PriorityQueue<ScoreCard>();
		double totalScore = 0.0;
		
		// then get gravity repulsion over all sane nodes
		for ( Integer i : sane ){
			Zone zone = _zoneMap.get(i);
			ScoreCard sc = new ScoreCard();
			sc.node = i;
			if ( zone == null ){
				sc.score = 0.0; // add the node with the lowest priority
				scores.add(sc);
			} else 	if ( ! seenZones.contains(zone) ){
				seenZones.add(zone);
				sc.score = getInvGravityPotential(zone.center());
				scores.add(sc);
				totalScore += sc.score;
			}
		}
		if ( totalScore == 0.0 ){
			return RNG.randomFromSet(sane);
		}
		scores = prune(scores, 5 ); // get top five zones
		return getRandDest(scores);
	}
}
