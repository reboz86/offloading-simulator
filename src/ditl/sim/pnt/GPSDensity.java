package ditl.sim.pnt;

import java.util.*;

import ditl.sim.*;


public class GPSDensity extends GPSWho {
	
		
	@Override
	public Integer whoToPush(Message msg, Set<Integer> infected, Set<Integer> sane){
		if ( _mainZone == null ){
			return RNG.randomFromSet(sane);
		}
		Set<Zone> seenZones = new HashSet<Zone>();
		PriorityQueue<ScoreCard> scores = new PriorityQueue<ScoreCard>();
		double totalScore = 0.0;
		for ( Integer i : sane ){
			Zone zone = _zoneMap.get(i);
			ScoreCard sc = new ScoreCard();
			sc.node = i;
			if ( zone == null ){
				sc.score = 0.0; // add the node with the lowest priority
				scores.add(sc);
			} else 	if ( ! seenZones.contains(zone) ){
				seenZones.add(zone);
				if ( zone._coords == null ){ // singleton
					sc.score = 1.0 / zone._area;
				} else {
					Set<Integer> saneNodes = new HashSet<Integer>(zone._coords.keySet());
					saneNodes.removeAll(infected);
					sc.score = (double)saneNodes.size() / zone._area;
				}
				scores.add(sc);
				totalScore += sc.score;
			}
		}
		if ( totalScore == 0.0 ){
			return RNG.randomFromSet(sane);
		}
		scores = prune(scores, 5 ); // get top five densest zones
		return getRandDest(scores);
	}
}
