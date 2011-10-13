package ditl.sim;

import java.io.IOException;
import java.util.*;

import ditl.*;

public class MessageGenerator extends Bus<MessageEvent> implements Generator, Listener<MessageEvent>{
	
	private long min_time;
	private long max_time;
	private long min_ttl;
	private long max_ttl;
	private MessageFactory<?> msg_factory;
	
	public MessageGenerator(long minTime, long maxTime, long minTTL, long maxTTL, MessageFactory<?> msgFactory){
		min_time = minTime;
		max_time = maxTime;
		min_ttl = minTTL;
		max_ttl = maxTTL;
		msg_factory = msgFactory;
		addListener(this);
	}
	
	private void createNewMessage(long time){
		long next_expire_time = nextExpireTime(time);
		Message msg = msg_factory.getNew(time, next_expire_time);
		queue(time, msg.getNewEvent());
		queue(next_expire_time, msg.getExpireEvent());
	}

	@Override
	public void handle(long time, Collection<MessageEvent> events) {
		for ( MessageEvent event : events )
			if ( event.isNew() ) // previous message is just starting
				createNewMessage(nextStartTime(time));
	}
	
	private long nextExpireTime(long time){
		Random rng = RNG.getInstance();
		return time + min_ttl + (long)((max_ttl-min_ttl)*rng.nextDouble()); 
	}
	
	private long nextStartTime(long time){
		Random rng = RNG.getInstance();
		return time + min_time + (long)((max_time-min_time)*rng.nextDouble()); 
	}
	
	@Override
	public Bus<?>[] busses() {
		return new Bus<?>[]{this};
	}

	@Override
	public int priority() {
		// TODO check!
		return Trace.defaultPriority;
	}

	@Override
	public void incr(long time) throws IOException {
		if ( ! hasNextEvent() )
			createNewMessage(time);
	}

	@Override
	public void seek(long time) throws IOException {}
	
	
	
}
