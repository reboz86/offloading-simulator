package ditl.sim;

import java.util.HashSet;
import java.util.Set;

public class UnicastMessage extends Message {

	private Router _to;
	
	public UnicastMessage(Router from, Router to, long bytes, long creationTime, long expirationTime) {
		super(from, bytes, creationTime, expirationTime);
		_to = to;
	}
	
	public Router to(){
		return _to;
	}

	@Override
	public boolean isDestinedTo(Router to) {
		return _to.equals(to);
	}
	
	public static final class Factory extends MessageFactory<UnicastMessage> {
		private World _world; 
		public Factory(World world, long minBytes, long maxBytes){
			super(minBytes,maxBytes);
			_world = world;
		}
		@Override
		public UnicastMessage getNew(long creationTime, long expirationTime) {
			System.out.println("Unicastmessage.getNew");
			Router from = _world.getRandomRouter();
			Router to = from;
			while ( to.equals(from) )
				to = _world.getRandomRouter();
			return new UnicastMessage(from, to, nextBytes(), creationTime, expirationTime);
		}
		@Override
		public Set<Router> defineMessageRecipients() {
			Set<Router> group=new HashSet<Router>();
			group.add(_world.getRandomRouter());
			return  group;
		}
	}

}
