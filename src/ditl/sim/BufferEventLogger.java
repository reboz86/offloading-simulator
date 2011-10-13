package ditl.sim;

import java.io.IOException;
import java.util.*;

import ditl.*;
import ditl.graphs.*;
import ditl.transfers.Buffer;
import ditl.transfers.BufferEvent;

public class BufferEventLogger implements Logger<ditl.sim.BufferEvent>, PresenceTrace.Handler {
	
	private StatefulWriter<BufferEvent,Buffer> buffer_writer;
	
	public BufferEventLogger(StatefulWriter<BufferEvent,Buffer> bufferWriter){
		buffer_writer = bufferWriter;
	}

	@Override
	public void handle(long time, Collection<ditl.sim.BufferEvent> events) throws IOException {
		for ( ditl.sim.BufferEvent event : events ){
			BufferEvent bev;
			if ( event.isAdd() )
				bev = new BufferEvent(event.router().id(), event.message().msgId(), BufferEvent.ADD);
			else
				bev = new BufferEvent(event.router().id(), event.message().msgId(), BufferEvent.REMOVE);
			buffer_writer.append(time, bev);
		}
	}

	@Override
	public void close() throws IOException {
		buffer_writer.close();
	}

	@Override
	public Listener<PresenceEvent> presenceEventListener() {
		return new Listener<PresenceEvent>(){
			@Override
			public void handle(long time, Collection<PresenceEvent> events)
					throws IOException {
				for ( PresenceEvent event : events ){
					if ( event.isIn() )
						buffer_writer.append(time, new BufferEvent(event.id(), BufferEvent.IN));
					else
						buffer_writer.append(time, new BufferEvent(event.id(), BufferEvent.OUT));
				}
			}
		};
	}

	@Override
	public Listener<Presence> presenceListener() {
		return new Listener<Presence>(){
			@Override
			public void handle(long time, Collection<Presence> events)
					throws IOException {
				Set<Buffer> initBuffers = new HashSet<Buffer>();
				for ( Presence p : events )
					initBuffers.add(new Buffer(p.id()));
				buffer_writer.setInitState(time, initBuffers);
			}
		};
	}
}
