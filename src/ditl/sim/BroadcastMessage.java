package ditl.sim;

import java.util.Set;


public class BroadcastMessage extends Message {
	
	public BroadcastMessage(Router from, Set<Router> group,long bytes, long creationTime, long expirationTime) {
		super(from, bytes, creationTime, expirationTime);
		setRecipients(group);
	}

	@Override
	public boolean isDestinedTo(Router to) {
		return true;
	}
	
	public static final class Factory extends MessageFactory<BroadcastMessage> {
		private World _world;
		public Factory(World world, long minBytes, long maxBytes){
			super(minBytes, maxBytes);
			_world = world;
		}
		@Override
		public BroadcastMessage getNew(long creationTime, long expirationTime) {
			Router from = _world.getRandomRouter();
			Set<Router> group = _world.getRandomRouters(1);
			group.remove(from);
			return new BroadcastMessage(from, group, nextBytes(), creationTime, expirationTime);
		}
		@Override
		public Set<Router> defineMessageRecipients() {
			return _world.getRandomRouters(1);
		}
	}

}
