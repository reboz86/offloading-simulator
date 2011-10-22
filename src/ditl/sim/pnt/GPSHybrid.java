package ditl.sim.pnt;

import java.util.*;

import ditl.sim.*;

public class GPSHybrid extends GPSWho {
		
	@Override
	public Integer whoToPush(Message msg, Set<Integer> infected, Set<Integer> sane){
		if ( _mainZone == null ){
			return RNG.randomFromSet(sane);
		}
		
		// first get barycenters for whole BH Tree
		setBarycenter(_mainZone,infected);
		
		Set<Zone> seenZones = new HashSet<Zone>();
		PriorityQueue<ScoreCard> scores = new PriorityQueue<ScoreCard>();
		
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
			}
		}
		scores = prune(scores, 10 ); // get top ten zones
		
		// then score according to density
		PriorityQueue<ScoreCard> densityScores = new PriorityQueue<ScoreCard>();
		for ( ScoreCard sc : scores ){
			Zone zone = _zoneMap.get(sc.node);
			ScoreCard dsc = new ScoreCard();
			dsc.node = sc.node;
			if ( zone == null ){
				dsc.score = 0.0;
			} else {
				if ( zone._coords != null ){
					Set<Integer> saneNodes = new HashSet<Integer>(zone._coords.keySet());
					saneNodes.removeAll(infected);
					dsc.score = (double)saneNodes.size() / zone._area; 
				} else {
					dsc.score = 1.0 / zone._area;
				}
			}
			densityScores.add(dsc);
		}
		return getRandDest(scores);
	}
}
