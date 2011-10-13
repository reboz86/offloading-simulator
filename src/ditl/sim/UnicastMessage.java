package ditl.sim;

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
			Router from = _world.getRandomRouter();
			Router to = from;
			while ( to.equals(from) )
				to = _world.getRandomRouter();
			return new UnicastMessage(from, to, nextBytes(), creationTime, expirationTime);
		}
	}

}
