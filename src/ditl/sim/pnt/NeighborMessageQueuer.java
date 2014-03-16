package ditl.sim.pnt;

import java.io.IOException;
import java.util.*;

import ditl.*;
import ditl.graphs.*;
import ditl.sim.*;

public class NeighborMessageQueuer 
	implements LinkTrace.Handler, PresenceTrace.Handler, Generator, Listener<Object> {
	
	private World _world;
	private long send_incr;
	private AdjacencySet.Links adjacency = new AdjacencySet.Links();
	private Set<Integer> present_ids = new HashSet<Integer>();
	private Bus<Object> next_update_bus = new Bus<Object>();
	private Integer root_id;
	
	public NeighborMessageQueuer(World world, long sendIncr, Integer rootId){
		_world = world;
		send_incr = sendIncr;
		next_update_bus.addListener(this);
		root_id = rootId;
	}

	
	@Override
	public Bus<?>[] busses() {
		return new Bus<?>[]{next_update_bus};
	}


	@Override
	public int priority() {
		return Trace.defaultPriority;
	}


	@Override
	public void incr(long time) {}


	@Override
	public void seek(long time) {
		next_update_bus.queue(time, Collections.emptySet());
	}
	
	@Override
	public void handle(long time, Collection<Object> foo) throws IOException {
		Router root = _world.getRouterById(root_id);
		for ( Integer id : present_ids ){
			if ( ! id.equals(root_id) ){
				Set<Integer> neighbs = adjacency.getNext(id);
				PntRouter router = (PntRouter)_world.getRouterById(id);
				NeighborsMessage msg = new NeighborsMessage(router, root, time, neighbs);
				router.queue(time, msg);
				router.trySendControlImmediately(time);
			}
		}
		next_update_bus.queue(time+send_incr, Collections.emptySet());
	}


	@Override
	public Listener<LinkEvent> linkEventListener() {
		return adjacency.linkEventListener();
	}


	@Override
	public Listener<Link> linkListener() {
		return adjacency.linkListener();
	}


	@Override
	public Listener<PresenceEvent> presenceEventListener() {
		return new Listener<PresenceEvent>(){
			@Override
			public void handle(long time, Collection<PresenceEvent> events) {
				for ( PresenceEvent pev : events ){
					if ( pev.isIn() ){
						present_ids.add(pev.id());
					} else {
						present_ids.remove(pev.id());
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
					present_ids.add(p.id());
			}
		};
	}

}
