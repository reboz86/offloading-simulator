package ditl.sim;

import java.io.IOException;
import java.util.*;

import ditl.*;
import ditl.graphs.*;
import ditl.transfers.Buffer;
import ditl.transfers.BufferEvent;
import ditl.transfers.BufferTrace;

public class BufferEventLogger implements Logger<ditl.sim.BufferEvent>, PresenceTrace.Handler {
	
	private static final boolean D=false;
	
	private StatefulWriter<BufferEvent,Buffer> buffer_writer;
	
	public BufferEventLogger(StatefulWriter<BufferEvent,Buffer> bufferWriter){
		buffer_writer = bufferWriter;
	}

	@Override
	public void handle(long time, Collection<ditl.sim.BufferEvent> events) throws IOException {
		if(D) System.out.println("BufferEventlogger.handle:"+time);
		for ( ditl.sim.BufferEvent event : events ){
			BufferEvent bev;
			if ( event.isAdd() )
				bev = new BufferEvent(event.router().id(), event.message().msgId(), BufferEvent.ADD);
			else
				bev = new BufferEvent(event.router().id(), event.message().msgId(), BufferEvent.REMOVE);
			if(D) System.out.println("event:"+bev.toString());
			
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
				if(D) System.out.println("BufferEventLogger.presenceEventlistener.handle:");
				for ( PresenceEvent event : events ){
					if(D) System.out.println("BufferEventLogger.Listener.handle:"+event.toString());
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
				if(D) System.out.println("BufferEventLogger.presencelistener.handle:");
				Set<Buffer> initBuffers = new HashSet<Buffer>();
				
				//TODO Filippo questo l'ho modificato io per aggiungere L'invio infra
				initBuffers.add(new Buffer(-42));
				//TODO
				
				for ( Presence p : events ){
					if(D) System.out.println(p.id());
					initBuffers.add(new Buffer(p.id()));
				}
				buffer_writer.setInitState(time, initBuffers);
			}
		};
	}
}
