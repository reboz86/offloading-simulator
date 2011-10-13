package ditl.sim;

import java.io.IOException;
import java.util.*;

import ditl.*;
import ditl.graphs.*;

public class World implements PresenceTrace.Handler, Listener<MessageEvent> {

	private Map<Integer,Router> routers = new HashMap<Integer,Router>();
	private RouterFactory router_factory;
	
	public void setRouterFactory(RouterFactory routerFactory){
		router_factory = routerFactory;
	}
	
	@Override
	public Listener<PresenceEvent> presenceEventListener() {
		return new Listener<PresenceEvent>(){
			@Override
			public void handle(long time, Collection<PresenceEvent> events){
				for ( PresenceEvent pev : events ){
					if ( pev.isIn() )
						routers.put(pev.id(),router_factory.getNew(pev.id()));
					else
						routers.remove(pev.id());
				}
			}
		};
	}

	@Override
	public Listener<Presence> presenceListener() {
		return new StatefulListener<Presence>(){
			@Override
			public void reset() {
				routers.clear();
			}

			@Override
			public void handle(long time, Collection<Presence> events){
				for ( Presence p : events )
					routers.put(p.id(), router_factory.getNew(p.id()));
			}
			
		};
	}
	
	public Router getRandomRouter(){
		Random rng = RNG.getInstance();
		int k = rng.nextInt(routers.size());
		int j=0;
		for ( Router r : routers.values() ){
			if ( j >= k )
				return r;
			j++;
		}
		return null;
	}
	
	public Set<Router> getRandomRouters(double p){
		Random rng = RNG.getInstance();
		Set<Router> group = new HashSet<Router>();
		for ( Router router : routers.values() ){
			if ( rng.nextDouble() < p ) 
				group.add(router);
		}
		return group;
	}
	
	public Router getRouterById(Integer id){
		return routers.get(id);
	}

	@Override
	public void handle(long time, Collection<MessageEvent> events) throws IOException {
		for ( MessageEvent event : events ){
			Message msg = event.message();
			if ( event.isNew() ){
				Router from = msg.from();
				from.newMessage(time, msg);
			} else {
				for ( Router node : routers.values() ){
					node.expireMessage(time, msg);
				}
			}
		}
	}

}
