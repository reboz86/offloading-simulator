package ditl.sim;

import java.util.HashSet;
import java.util.Set;

public class MulticastMessage extends Message {

	
	public MulticastMessage(Router from, Set<Router> group, long bytes, long creationTime, long expirationTime) {
		super(from, bytes, creationTime, expirationTime);
		setRecipients(group);
	}

	@Override
	public boolean isDestinedTo(Router to) {
		Set<Router> group=getRecipients();
		//System.out.println(this.msgId()+" is destined to "+to.id()+"? "+group.contains(to));
		return group.contains(to);
	}
	
	public static class Factory extends MessageFactory<MulticastMessage> {
		private World _world;
		private double _p;
		private int nCategory = 10;
		Set<Double> category= new HashSet<Double>(); // category to rank contents
		
		public Factory(World world, long minBytes, long maxBytes, double p){
			super(minBytes, maxBytes);
			_world = world;
			_p = p;
			for (int i=1; i<=nCategory; i++)
				category.add((double)1/Math.pow(i, 1.1)); // Zipf popularity distribution qk = 1/(k^alpha) with alpha = 2--- K classes of popularity; contents items of class k are requested with probability  qk ;
		}
		// Multicast Message is always started by Infra 
		@Override
		public MulticastMessage getNew(long creationTime, long expirationTime) {
			Router from =_world.getRouterById(-42);
			//			_p=(double)RNG.randomFromSet(category)/10 ;// exp (-2)
			//			System.out.println("Probability:"+_p);
			//			Set<Router> group = _world.getRandomPresentRouters(_p);
			//			group.remove(from);
			return new MulticastMessage(from, null, nextBytes(), creationTime, expirationTime);
		}


		@Override
		public Set<Router> defineMessageRecipients(){
			Router from =_world.getRouterById(-42);
			_p=(double)RNG.randomFromSet(category);// /10 ;// exp (-2)
			
			Set<Router> group = _world.getRandomPresentRouters(_p);
			System.out.println("Probability:"+_p);
			group.remove(from);
			return group;
		}
		
	}

}
