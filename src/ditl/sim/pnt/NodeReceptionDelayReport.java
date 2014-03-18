package ditl.sim.pnt;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ditl.Bus;
import ditl.Generator;
import ditl.Listener;
import ditl.MultipleReports;
import ditl.Reader;
import ditl.Runner;
import ditl.StatefulReader;
import ditl.Store.NoSuchTraceException;
import ditl.graphs.Presence;
import ditl.graphs.PresenceEvent;
import ditl.graphs.PresenceTrace;
import ditl.transfers.Buffer;
import ditl.transfers.BufferEvent;
import ditl.transfers.BufferTrace;
import ditl.transfers.Message;
import ditl.transfers.MessageEvent;
import ditl.transfers.MessageTrace;

public class NodeReceptionDelayReport extends MultipleReports 	implements  BufferTrace.Handler,MessageTrace.Handler,PresenceTrace.Handler,  Listener<Object>, Generator {

	private long start_time;
	private long end_time;
	private BufferTrace buffer;
	private Bus<Object> update_bus = new Bus<Object>();
	private MessageTrace message;
	protected int message_counter=0;
	List<Integer> present_node_list= new ArrayList<Integer>();
	private Map<Integer,Long> entry_times = new HashMap<Integer,Long>();
	protected long actual_msg_start_time;
	private PresenceTrace presence;
	private static String report_prefix;



	public NodeReceptionDelayReport(String prefix,  long startTime, long endTime, MessageTrace message, BufferTrace buffer,PresenceTrace presence ) throws IOException {
		super();
		start_time = startTime;
		end_time= endTime;
		update_bus.addListener(this);
		this.buffer=buffer;
		this.message=message;
		this.presence=presence;
		report_prefix=prefix;

	}

	public NodeReceptionDelayReport(OutputStream out,  long startTime, long endTime) throws IOException {
		super();
		start_time = startTime;
		end_time= endTime;
		update_bus.addListener(this);
	}
	
	private void queueUpdate(long time){
		update_bus.queue(time, Collections.emptySet());
	}
	
	
public void exec() throws NoSuchTraceException, IOException{
		
		if (start_time == -1) start_time = presence.minTime();
		if (end_time == -1) end_time = presence.maxTime();
		
		
		Bus<Presence> presenceBus = new Bus<Presence>();
		presenceBus.addListener(this.presenceListener());
		Bus<PresenceEvent> presenceEventBus = new Bus<PresenceEvent>();
		presenceEventBus.addListener(this.presenceEventListener());
		StatefulReader<PresenceEvent,Presence> presenceReader = presence.getReader();
		presenceReader.setBus(presenceEventBus);
		presenceReader.setStateBus(presenceBus);
		
		
		//buffer
		Bus<BufferEvent> bufferEventBus = new Bus<BufferEvent>();
		bufferEventBus.addListener(this.bufferEventListener());
		Reader<BufferEvent> bufferReader = buffer.getReader();
		bufferReader.setBus(bufferEventBus);
		//messages
		Bus<MessageEvent> messageEventBus = new Bus<MessageEvent>();
		messageEventBus.addListener(this.messageEventListener());
		Reader<MessageEvent> messageReader = message.getReader();
		messageReader.setBus(messageEventBus);
		
		Runner runner = new Runner(buffer.maxUpdateInterval(),presence.minTime(), presence.maxTime());
		runner.addGenerator(bufferReader);
		runner.addGenerator(messageReader);
		runner.addGenerator(presenceReader);
		runner.addGenerator(this);
		
		runner.run();
		
		this.finish();
		
	}


	@Override
	public void incr(long dt) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void seek(long time) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public Bus<?>[] busses() {
		return new Bus<?>[]{update_bus};
	}

	@Override
	public int priority() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void handle(long time, Collection<Object> events) throws IOException {
		// TODO Auto-generated method stub

	}


	public Listener<BufferEvent> bufferEventListener() {
		return new Listener<BufferEvent>(){
			@Override
			public void handle(long time, Collection<BufferEvent> events)
					throws IOException {
				if(time <end_time && time>= start_time){
					for ( BufferEvent event : events ){
						if ( event.type() == BufferEvent.ADD ){
							// MSG Received
							if (! event.id().equals(PntScenario.root_id)){
								long reception_time = time-Math.max(actual_msg_start_time,entry_times.get(event.id()));
								append(event.id()+"\t"+ reception_time);

							}
						}
						if ( event.type() == BufferEvent.REMOVE ){


						}
					}

				}
			}
	};
}

	@Override
	public Listener<Buffer> bufferListener() {
		return null;
	}


	@Override
	public Listener<MessageEvent> messageEventListener() {
		return new Listener<MessageEvent>(){
			@Override
			public void handle(long time, Collection<MessageEvent> events) throws IOException {
				if(time <=end_time && time>= start_time){
					for (MessageEvent mev: events){
						if ( mev.isNew()){
							// new message
							actual_msg_start_time= time;
							message_counter++;
							//newReport(new FileOutputStream(report_prefix+message_counter+".txt"));
							
							
						}else{
							//expired message

						}
					}
				}
			}
		};
	}

	@Override
	public Listener<Message> messageListener() {
		return null;
	}
	
	
	@Override
	public Listener<PresenceEvent> presenceEventListener() {
		return new Listener<PresenceEvent>(){

			@Override
			public void handle(long time, Collection<PresenceEvent> events) throws IOException {
				for ( PresenceEvent pev : events ){
						if ( pev.isIn() ){
							present_node_list.add(pev.id());
							if (!entry_times.containsKey(pev.id()))
								entry_times.put(pev.id(), time);

						}

						else{
							present_node_list.remove(pev.id());
							
						}
					}

				}

		};
	}

	@Override
	public Listener<Presence> presenceListener() {
		// TODO Auto-generated method stub
		return null;
	}


}
