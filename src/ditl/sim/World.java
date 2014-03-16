package ditl.sim;

import java.io.IOException;
import java.util.*;

import ditl.*;
import ditl.graphs.*;

public class World implements PresenceTrace.Handler, Listener<MessageEvent> {

	private static final boolean D= false;

	private static Map<Integer,Router> routers = new HashMap<Integer,Router>();
	private static RouterFactory router_factory;
	private static Set<Router> presence= new HashSet<Router>();


//	private static double probability;
	
	public  void setRouterFactory(RouterFactory routerFactory){
		router_factory = routerFactory;
	}

//	public void setInterestProbability(double prob){
//		probability=prob;
//	}

	

	// Quando un nodo entra nel sistema (presenceEvent IN) viene aggiunto un router appropriato (INFRA o ad hoc)
	@Override
	public Listener<PresenceEvent> presenceEventListener() {
		return new Listener<PresenceEvent>(){
			@Override
			public void handle(long time, Collection<PresenceEvent> events){

				for ( PresenceEvent pev : events ){
					if(D) System.out.println("World.Listener.presenceEventListener.handle:"+time+";"+pev.toString());
					if ( pev.isIn() ){
						routers.put(pev.id(),router_factory.getNew(pev.id()));
						presence.add(routers.get(pev.id()));
						//System.out.println("WorldListener.presenceEventListener.handle::"+routers.toString());

//						//decide if incoming node is interested in the content
//						for (Message msg:active_messages){
//							if (RNG.random(probability))
//								msg.addRecipient(routers.get(pev.id()));
//						}
					}
					else{
						// do not remove the router that exits because we need to push pending messages
						//routers.remove(pev.id());
						presence.remove(routers.get(pev.id()));
						if(D)System.out.println("Router: "+pev.id()+" exiting!!!!");
						
					}
				}
			}
		};
	}

	@Override
	public Listener<Presence> presenceListener() {
		return new StatefulListener<Presence>(){
			@Override
			public void reset() {
				if(D) System.out.println("World.Listener.presenceListener.reset");
				routers.clear();

				//TODO Filippo modifica qui 
				// TODO Rappezzato ma sembra funzionare.....

				// in pratica se non esiste aggiungo l'INFRA perchè se no ho un nullPointerException quando tento 
				// di fare pushUp e PushDown

				if (routers.isEmpty()){
					routers.put(-42, router_factory.getNew(-42));
					presence.add(router_factory.getNew(-42));
					if (D) System.out.println("World: added the INFRA ROUTER");
				}
			}

			@Override
			public void handle(long time, Collection<Presence> events){
				//if(D) System.out.println("World.ListenerpresenceListener.handle:"+time);
				for ( Presence p : events ){
					//System.out.println(p.toString());
					routers.put(p.id(), router_factory.getNew(p.id()));
					presence.add(routers.get(p.id()));
					//System.out.println("World.handle:"+routers.toString());
					//decide if incoming node is interested in the content
//					for (Message msg:active_messages){
//						if (RNG.random(probability))
//							msg.addRecipient(routers.get(p.id()));
//					}
				}
			}
		};
	}

		public Router getRandomRouter(){
			return RNG.randomFromSet(routers.values());
	}
	
	public Set<Router> getRandomRouters(double p){
		if(D)System.out.println(RNG.randomSubSet(routers.values(),p));
		return RNG.randomSubSet(routers.values(),p);
	}
	
	
	public Set<Router> getRandomPresentRouters(double p){
		if(D)System.out.println("getRandomPresentRouters. present:"+presence.size());
		Collection<Router> presentRouters = new HashSet<Router>(routers.values());
		presentRouters.retainAll(presence);
		return RNG.randomSubSet(presentRouters,p);
	}
	
	public  Set<Router> getRandomRoutersExceptInfra(double p){
		
		Collection<Router> all_routers=routers.values();
		all_routers.remove(getRouterById(-42));
		
		return RNG.randomSubSet(all_routers,p);
	}
	
	public  Router  getRouterById(Integer id){
		return routers.get(id);
	}

	//Handler per gestire l'interazione con i messaggi creati da MessageGenerator
	@Override
	public void handle(long time, Collection<MessageEvent> events) throws IOException {
		
		for ( MessageEvent event : events ){
			
			Message msg = event.message();
			if ( event.isNew() ){
				
//				active_messages.add(msg);
				
				Router from = msg.from();
				if(D) System.out.println("World.handle: New Message "+time+";"+event.message().msgId());
				from.newMessage(time, msg);
				
			
			} else {
				if(D) System.out.println("World.handle: Expire Message "+time+";"+event.message().msgId());
				for ( Router node : routers.values() ){
					node.expireMessage(time, msg);
//					active_messages.remove(msg);
					
				}
			}
		}
	}

}
