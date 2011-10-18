package ditl.sim;

import java.io.IOException;
import java.util.*;

import ditl.*;

public class MessageGenerator extends Bus<MessageEvent> implements Generator, Listener<MessageEvent>{
	
	private long min_time = Long.MIN_VALUE;
	private long max_time = Long.MAX_VALUE;
	private long min_period;
	private long max_period;
	private long min_ttl;
	private long max_ttl;
	private MessageFactory<?> msg_factory;
	
	public MessageGenerator(long minPeriod, long maxPeriod, long minTTL, long maxTTL, MessageFactory<?> msgFactory){
		min_period = minPeriod;
		max_period = maxPeriod;
		min_ttl = minTTL;
		max_ttl = maxTTL;
		msg_factory = msgFactory;
		addListener(this);
	}
	
	public void setTimeLimits(long minTime, long maxTime){
		min_time = minTime;
		max_time = maxTime;
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
				if ( time >= min_time && time <= max_time ) // we are still good timewise
					createNewMessage(nextStartTime(time));
	}
	
	private long nextExpireTime(long time){
		Random rng = RNG.getInstance();
		return time + min_ttl + (long)((max_ttl-min_ttl)*rng.nextDouble()); 
	}
	
	private long nextStartTime(long time){
		Random rng = RNG.getInstance();
		return time + min_period + (long)((max_period-min_period)*rng.nextDouble()); 
	}
	
	@Override
	public Bus<?>[] busses() {
		return new Bus<?>[]{this};
	}

	@Override
	public int priority() {
		// TODO check!
		return Integer.MAX_VALUE; // lowest priority
	}

	@Override
	public void incr(long time) throws IOException {}
	

	@Override
	public void seek(long time) throws IOException {
		reset();
		if ( ! hasNextEvent() )
			createNewMessage(Math.max(time, min_time));
	}
	
	
	
}
