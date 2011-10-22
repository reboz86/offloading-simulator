package ditl.sim.pnt;

import java.io.IOException;
import java.util.*;

import ditl.*;
import ditl.graphs.*;
import ditl.sim.*;

public class GPSMessageQueuer 
	implements MovementTrace.Handler, Generator, Listener<Object> {
	
	private World _world;
	private long send_incr;
	private Map<Integer, Movement> movements = new HashMap<Integer,Movement>();
	private Bus<Object> next_update_bus = new Bus<Object>();
	private Integer root_id;
	
	public GPSMessageQueuer(World world, long sendIncr, Integer rootId){
		_world = world;
		send_incr = sendIncr;
		next_update_bus.addListener(this);
		root_id = rootId;
	}
	
	
	@Override
	public Listener<MovementEvent> movementEventListener() {
		return new Listener<MovementEvent>(){
			@Override
			public void handle(long time, Collection<MovementEvent> events) {
				for ( MovementEvent mev : events){
					Movement m;
					if ( ! mev.id().equals(root_id) ){
						switch(mev.type()){
						case MovementEvent.IN: 
							m = mev.origMovement();
							movements.put(m.id(), m);
							break;
							
						case MovementEvent.OUT:
							m = movements.get(mev.id());
							movements.remove(m.id());
							break;
							
						default:
							m = movements.get(mev.id());
							m.handleEvent(time, mev);
						}
					}
				}
			}
		};
	}
	@Override
	public Listener<Movement> movementListener() {
		return new Listener<Movement>(){
			@Override
			public void handle(long time, Collection<Movement> events) {
				for ( Movement m : events ){
					if ( ! m.id().equals(root_id) )
						movements.put(m.id(), m);
				}
			}
		};
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
		for ( Movement m : movements.values() ){
			Integer id = m.id();
			Point cur_pos = m.positionAtTime(time);
			PntRouter router = (PntRouter)_world.getRouterById(id);
			GPSMessage msg = new GPSMessage(router, root, time, cur_pos);
			router.queue(time, msg);
			router.trySendControlImmediately(time);
		}
		next_update_bus.queue(time+send_incr, Collections.emptySet());
	}

}
