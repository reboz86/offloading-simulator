package ditl.sim.pnt;

import java.util.*;

import ditl.sim.*;

public class DefaultWhoToPush implements WhoToPush {
	
		
	public Integer whoToPush(Message msg, Set<Integer> infected, Set<Integer> sane){
		return null;
	}
		
	protected Integer getRandDest(PriorityQueue<ScoreCard> scores){
		Random rng = RNG.getInstance();
		double total = 0;
		for ( ScoreCard sc : scores ){
			total += sc.score;
		}
		double rand = rng.nextDouble();
		double cum = 0;
		for ( ScoreCard sc : scores ){
			cum += sc.score;
			if ( rand <= cum/total ){
				return sc.node;
			}
		}
		return null;
	}
	
	protected PriorityQueue<ScoreCard> prune(PriorityQueue<ScoreCard> scores, int maxElems ){
		PriorityQueue<ScoreCard> newScores = new PriorityQueue<ScoreCard>();
		int c = 0;
		for ( ScoreCard sc : scores ){
			newScores.add(sc);
			++c;
			if ( c >= maxElems ){
				break;
			}
		}
		return newScores;
	}
	
	protected class ScoreCard implements Comparable<ScoreCard>{
		public Integer node;
		public Double score;
		
		@Override
		public int compareTo(ScoreCard o) { // bigger is better (and hence smaller for priority queues)
			if ( score > o.score )
				return -1;
			if ( score < o.score )
				return 1;
			return 0;
		}
	}
}
