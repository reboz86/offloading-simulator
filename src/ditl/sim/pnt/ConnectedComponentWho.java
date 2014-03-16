package ditl.sim.pnt;

import java.util.*;

import ditl.Listener;
import ditl.graphs.*;
import ditl.sim.*;

public class ConnectedComponentWho extends DefaultWhoToPush 
	implements Listener<TransferEvent>, PresenceTrace.Handler {

	private Map<Integer,Set<Integer>> _neighbors = new HashMap<Integer,Set<Integer>>();;
	private List<Set<Integer>> _ccs;
	private Set<Integer> present = new HashSet<Integer>();
	
	
	/**
	 * Decide the who to push strategy. Every time it calculates first the size of the best not infected connected components (CC)
	 * If all CC have at least one infected node, it choose a random node within the one with the most uninfected nodes.
	 * 
	 * @param 
	 * @return Integer
	 */
	@Override
	public Integer whoToPush(Message msg, Set<Integer> infected, Set<Integer> sane){
		recalc();
		int sizeSane = 0;
		int sizeInf = 0;
		Set<Integer> bestSane, bestInf, cc;
		bestSane = null;
		bestInf = null;
		for ( Set<Integer> set : _ccs ){
			cc = new HashSet<Integer>(set);
			int s = cc.size();
			cc.removeAll(infected);
			if ( cc.size() < s ){ // infected
				if ( cc.size() > sizeInf ){
					bestInf = cc;
					sizeInf = cc.size();
				}
			} else {
				if (cc.size() > sizeSane){
					bestSane = cc;
					sizeSane = cc.size();
				}
			}
		}
		if ( bestSane != null )
			return RNG.randomFromSet(bestSane);
		return RNG.randomFromSet(bestInf);
	}
	
	@Override
	public void handle(long time, Collection<TransferEvent> events){
		for ( TransferEvent tev : events ){
			Transfer transfer = tev.transfer();
			Message msg = transfer.message();
			if ( msg instanceof NeighborsMessage ){
				Integer from_id = transfer.from().id();
				Set<Integer> neighbs = ((NeighborsMessage)msg).neighbors();
				if ( neighbs != null )
					updateNode(from_id, neighbs);
				else
					updateNode(from_id, Collections.<Integer>emptySet());
			}
		}
	}

	private void addNode(Integer from) {
		if ( ! from.equals(PntScenario.root_id))
			present.add(from);
	}

	private void removeNode(Integer from) {
		present.remove(from);
	}
	
	private void updateNode(Integer from, Set<Integer> neighbors){
		_neighbors.put(from, neighbors);
	}
	
	
	/**
	 *  Updates the  connected components (_ccs) that exist in the network. Divides and groups nodes using
	 * the _neighbor Map.
	 */
	
	private void recalc(){
		Set<Integer> toVisit = new HashSet<Integer>(present);
		_ccs = new LinkedList<Set<Integer>>();
		while( ! toVisit.isEmpty() ){
			LinkedList<Integer> inCC = new LinkedList<Integer>();
			Set<Integer> curCC = new HashSet<Integer>();
			Integer i = null;
			for ( Integer k : toVisit ){
				i = k;
				toVisit.remove(k);
				break;
			}
			inCC.add(i);
			while( ! inCC.isEmpty() ){
				i = inCC.pop();
				curCC.add(i);
				Set<Integer> neighbs = _neighbors.get(i);
				if ( neighbs != null ){
					for ( Integer j : neighbs ){
						if ( toVisit.contains(j) ){
							toVisit.remove(j);
							inCC.add(j);
						}
					}
				}
			}
			_ccs.add(curCC);
		}
	}

	@Override
	public Listener<PresenceEvent> presenceEventListener() {
		return new Listener<PresenceEvent>(){
			@Override
			public void handle(long time, Collection<PresenceEvent> events) {
				for ( PresenceEvent pev : events ){
					if ( pev.isIn() ){
						addNode(pev.id());
					} else {
						removeNode(pev.id());
					}
				}
			}
		};
	}

	@Override
	public Listener<Presence> presenceListener() {
		return new Listener<Presence>(){
			@Override
			public void handle(long time, Collection<Presence> events) {
				for ( Presence p : events )
					addNode(p.id());
			}
		};
	}
}
