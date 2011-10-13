package ditl.sim.tactical;

import java.io.IOException;
import java.util.*;

import ditl.*;
import ditl.sim.*;

public class PanicLeader extends Leader implements Listener<PanicEvent> {

	protected PanicBus panic_bus;
	protected long panic_time;
	protected Set<Message> panic_messages = new HashSet<Message>();
	

	public PanicLeader(AdHocRadio adhocRadio, AdHocRadio vhfData,
			AdHocRadio vhfControl, Integer id, Integer gid, int bufferSize, long guardTime,
			Bus<BufferEvent> bus, Map<Integer, Leader> leaders, 
			PanicBus panicBus, long panicTime ) {
		super(adhocRadio, vhfData, vhfControl, id, gid, bufferSize, guardTime, bus, leaders);
		panic_bus = panicBus;
		panicBus.addListener(this);
		panic_time = panicTime;
	}

	@Override
	public void handle(long time, Collection<PanicEvent> events){
		for ( PanicEvent pev : events ){
			Message msg = pev.message();
			if ( not_yet_acknowledged.containsKey(msg) ){ // this panic event concerns us
				panic_messages.add(msg);
				trySendImmediately(time,vhf_data);
			}
		}
	}
	
	@Override
	protected boolean allowVHFTransfer(long time, Message msg, Router dest){
		return super.allowVHFTransfer(time, msg, dest) && panic_messages.contains(msg);
	}
	
	@Override
	public void expireMessage(long time, Message msg) throws IOException {
		super.expireMessage(time, msg);
		panic_messages.remove(msg);
	}

	@Override
	protected void takeResponsability(long time, Message msg){
		super.takeResponsability(time, msg);
		long panic_t = msg.expirationTime() - panic_time;
		if ( panic_t <= time ){
			panic_messages.add(msg);
			trySendImmediately(time,vhf_data);
		} else {
			panic_bus.queue(panic_t, new PanicEvent(msg));
		}
	}
}
