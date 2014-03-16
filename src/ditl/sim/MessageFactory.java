package ditl.sim;

import java.util.Random;
import java.util.Set;

public abstract class MessageFactory<M extends Message> {
	
	protected long min_bytes;
	protected long max_bytes;
	
	public MessageFactory(long minBytes, long maxBytes){
		min_bytes = minBytes;
		max_bytes = maxBytes;
	}
	
	public abstract M getNew(long creationTime, long expirationTime);
	
	public abstract Set<Router> defineMessageRecipients();
	
	protected long nextBytes(){
		Random rng = RNG.getInstance();
		return min_bytes + (long)((max_bytes-min_bytes)*rng.nextDouble());
	}
}
