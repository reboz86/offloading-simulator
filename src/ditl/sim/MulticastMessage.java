package ditl.sim;

import java.util.Set;

public class MulticastMessage extends Message {

	private Set<Router> _group;
	
	public MulticastMessage(Router from, Set<Router> group, long bytes, long creationTime, long expirationTime) {
		super(from, bytes, creationTime, expirationTime);
		_group = group;
	}

	@Override
	public boolean isDestinedTo(Router to) {
		return _group.contains(to);
	}
	
	public static final class Factory extends MessageFactory<MulticastMessage> {
		private World _world;
		private double _p;
		public Factory(World world, long minBytes, long maxBytes, double p){
			super(minBytes, maxBytes);
			_world = world;
			_p = p;
		}
		@Override
		public MulticastMessage getNew(long creationTime, long expirationTime) {
			Router from =_world.getRandomRouter();
			Set<Router> group = _world.getRandomRouters(_p);
			group.remove(from);
			return new MulticastMessage(from, group, nextBytes(), creationTime, expirationTime);
		}
	}

}
