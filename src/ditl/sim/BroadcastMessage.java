package ditl.sim;


public class BroadcastMessage extends Message {
	
	public BroadcastMessage(Router from, long bytes, long creationTime, long expirationTime) {
		super(from, bytes, creationTime, expirationTime);
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
			return new BroadcastMessage(from, nextBytes(), creationTime, expirationTime);
		}
	}

}
