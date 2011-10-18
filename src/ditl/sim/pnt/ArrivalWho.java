package ditl.sim.pnt;

import java.util.*;

import ditl.Listener;
import ditl.graphs.*;

public abstract class ArrivalWho extends DefaultWhoToPush implements PresenceTrace.Handler {

	protected Map<Integer,Long> _arrivalTimes = new HashMap<Integer,Long>(); 
	
	@Override
	public Listener<Presence> presenceListener(){
		return new Listener<Presence>(){
			@Override
			public void handle(long time, Collection<Presence> events) {
				for ( Presence p : events )
					_arrivalTimes.put(p.id(), time);
			}
		};
	}
	
	@Override
	public Listener<PresenceEvent> presenceEventListener(){
		return new Listener<PresenceEvent>(){
			@Override
			public void handle(long time, Collection<PresenceEvent> events) {
				for ( PresenceEvent pev : events ){
					if ( pev.isIn() ){
						_arrivalTimes.put(pev.id(), time);
					} else {
						_arrivalTimes.remove(pev.id());
					}
				}
			}
		};
	}
}
