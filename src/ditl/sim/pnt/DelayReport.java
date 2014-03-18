package ditl.sim.pnt;


import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import ditl.Bus;
import ditl.Generator;
import ditl.Listener;
import ditl.Reader;
import ditl.Report;
import ditl.Runner;
import ditl.StatefulReader;
import ditl.Store.LoadTraceException;
import ditl.Store.NoSuchTraceException;
import ditl.WritableStore.AlreadyExistsException;
import ditl.cli.App;
import ditl.cli.ExportApp;
import ditl.graphs.Presence;
import ditl.graphs.PresenceEvent;
import ditl.graphs.PresenceTrace;
import ditl.graphs.cli.GraphOptions;
import ditl.transfers.Buffer;
import ditl.transfers.BufferEvent;
import ditl.transfers.BufferTrace;
import ditl.transfers.Message;
import ditl.transfers.MessageEvent;
import ditl.transfers.MessageTrace;

public class DelayReport extends Report 
implements  BufferTrace.Handler,MessageTrace.Handler, Listener<Object>, Generator {
	
	private static String infraRouterOption = "infra-router-id";
	
//	private static int infra_router_id;
//	private long infra_transferred_up = 0;
//	private long infra_transferred_down = 0;
//	private long adhoc_transferred = 0;
//	private long hypotetical_transferred = 0;
//	
//	private int infra_message_sent =0;
	private long actual_msg_start_time=0;
	
	List<Integer> present_node_list= new ArrayList<Integer>();
	
	private Map<Integer,Long> entry_times = new HashMap<Integer,Long>();
	protected long n_transfers = 0;
	protected int completed=0;
	protected long tot_delay = 0;
	private long totSize =0;
	private int msg_id;
	private long start_time;
	private long end_time;
	private int lifetime, nSlot;
	private Bus<Object> update_bus= new Bus<Object>();
	private MessageTrace message;
	private BufferTrace buffer;
	private PresenceTrace presence;
	protected int infected;
	private Set<Integer> infected_ids = new HashSet<Integer>();
	private int[] data;
	
	
	public DelayReport(OutputStream out,  long startTime, long endTime, MessageTrace message, BufferTrace buffer, PresenceTrace presence ) throws IOException {
		
		super(out);
		start_time = startTime;
		end_time= endTime;
		update_bus.addListener(this);
		this.message=message;
		this.buffer=buffer;
		this.presence=presence;
				
		
	}

	public DelayReport(OutputStream out,  long startTime, long endTime, int lifetime, int nSlot) throws IOException {
		super(out);
		start_time = startTime;
		end_time= endTime;
		this.lifetime=lifetime;
		this.nSlot=nSlot;
		data = new int[nSlot];
		update_bus.addListener(this);
	}
	
	
	private void queueUpdate(long time){
		update_bus.queue(time, Collections.emptySet());
	}

	
	public void exec() throws NoSuchTraceException, IOException{
		
		if (start_time == -1) start_time=presence.minTime();
		if (end_time == -1) end_time = presence.maxTime();
		
		//Presence
//		Bus<Presence> presenceBus = new Bus<Presence>();
//		presenceBus.addListener(this.presenceListener());
//		Bus<PresenceEvent> presenceEventBus = new Bus<PresenceEvent>();
//		presenceEventBus.addListener(this.presenceEventListener());
//		StatefulReader<PresenceEvent,Presence> presenceReader = presence.getReader();
//		presenceReader.setBus(presenceEventBus);
//		presenceReader.setStateBus(presenceBus);
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
		
		Runner runner = new Runner(buffer.maxUpdateInterval(), presence.minTime(), presence.maxTime());
		runner.addGenerator(bufferReader);
//		runner.addGenerator(presenceReader);
		runner.addGenerator(messageReader);
		runner.addGenerator(this);
		
		runner.run();
		
		//System.out.println("InfectionDistribution.finish");
		this.finish();
		
	}
	
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		App app = new ExportApp(){

			GraphOptions graph_options = new GraphOptions(GraphOptions.PRESENCE);
			Long min_time, max_time;
			int lifetime, nSlot;
			private final String startOption= "start-time";
			private final String finishOption= "stop-time";
			private final String lifetimeOption= "lifetime";
			private final String slotOption= "slot";
			
			@Override
			protected void initOptions() {
				super.initOptions();
				graph_options.setOptions(options);
				//options.addOption(null, shiftOption, true, "Shift starting time by <arg>");
				options.addOption(null, lifetimeOption, true, "Lifetime of content");
				options.addOption(null, startOption, true, "Start analysis at time <arg>");
				options.addOption(null, finishOption, true, "Stop analysis at time <arg>");
				options.addOption(null, slotOption, true, "Number of slot to divide the lifetime");
			}
			
			@Override
			protected void run() throws IOException, NoSuchTraceException,
					AlreadyExistsException, LoadTraceException {
//				PresenceTrace presence = (PresenceTrace)_store.getTrace(graph_options.get(GraphOptions.PRESENCE));
				BufferTrace buffers = (BufferTrace)_store.getTrace("buffers");
				MessageTrace messages = (MessageTrace)_store.getTrace("messages");
				
				if (min_time == -1) min_time=buffers.minTime();
				if (max_time == -1) max_time = buffers.maxTime();
				
				DelayReport r = new DelayReport(_out,min_time, max_time, lifetime,nSlot);
				
//				//Presence
//				Bus<Presence> presenceBus = new Bus<Presence>();
//				presenceBus.addListener(r.presenceListener());
//				Bus<PresenceEvent> presenceEventBus = new Bus<PresenceEvent>();
//				presenceEventBus.addListener(r.presenceEventListener());
//				StatefulReader<PresenceEvent,Presence> presenceReader = presence.getReader();
//				presenceReader.setBus(presenceEventBus);
//				presenceReader.setStateBus(presenceBus);
				//buffer
				Bus<BufferEvent> bufferEventBus = new Bus<BufferEvent>();
				bufferEventBus.addListener(r.bufferEventListener());
				Reader<BufferEvent> bufferReader = buffers.getReader();
				bufferReader.setBus(bufferEventBus);
				//messages
				Bus<MessageEvent> messageEventBus = new Bus<MessageEvent>();
				messageEventBus.addListener(r.messageEventListener());
				Reader<MessageEvent> messageReader = messages.getReader();
				messageReader.setBus(messageEventBus);
				
				Runner runner = new Runner(buffers.maxUpdateInterval(), buffers.minTime(), buffers.maxTime());
				runner.addGenerator(bufferReader);
//				runner.addGenerator(presenceReader);
				runner.addGenerator(messageReader);
				runner.addGenerator(r);
				
				runner.run();
				
				r.finish();
//				presenceReader.close();
				bufferReader.close();
				messageReader.close();
			}
			
			@Override
			protected String getUsageString(){
				return "[OPTIONS] STORE ";
			}
			
			@Override
			protected void parseArgs(CommandLine cli, String[] args)
					throws ParseException, ArrayIndexOutOfBoundsException,
					HelpException {
				super.parseArgs(cli, args);
				graph_options.parse(cli);
				min_time=Long.parseLong(cli.getOptionValue(startOption,"-1"));
				max_time=Long.parseLong(cli.getOptionValue(finishOption,"-1"));
				lifetime=Integer.parseInt(cli.getOptionValue(lifetimeOption,"60"));
				nSlot=Integer.parseInt(cli.getOptionValue(slotOption,"6"));
				
			}
			
		};
		app.ready("", args);
		app.exec();

	}

	@Override
	public void incr(long dt) throws IOException {
		
	}

	@Override
	public void seek(long time) throws IOException {
		
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
		
		//System.out.println("delay= "+tot_delay+" num transfers= "+n_transfers);
		
		
	}
	
	
	@Override
	public void finish() throws IOException {

		for (int j=0;j<nSlot; ++j){
			append((double)data[j]/n_transfers, "\t");
		}
		append("\n");
		
		super.finish();
	}


//	@Override
//	public Listener<Presence> presenceListener() {
//		return new Listener<Presence>(){
//			@Override
//			public void handle(long time, Collection<Presence> events) throws IOException {}
//		};
//	}
//
//	
//	@Override
//	public Listener<PresenceEvent> presenceEventListener() {
//		return new Listener<PresenceEvent>(){
//
//
//			@Override
//			public void handle(long time, Collection<PresenceEvent> events) throws IOException {
//				for ( PresenceEvent pev : events ){
//						if ( pev.isIn() ){
//							present_node_list.add(pev.id());
//							if (!entry_times.containsKey(pev.id()))
//								entry_times.put(pev.id(), time);
//
//						}
//
//						else{
//							present_node_list.remove(pev.id());
//							
//						}
//					}
//
//				}
//
//		};
//	}

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
							
							
						}else{
							//queueUpdate(time);
							
							
						}
					}
				}
			}
		};
	}

	@Override
	public Listener<Message> messageListener() {
		// TODO Auto-generated method stub
		return null;
	}

	public Listener<BufferEvent> bufferEventListener() {
		return new Listener<BufferEvent>(){
			@Override
			public void handle(long time, Collection<BufferEvent> events)
					throws IOException {
				for ( BufferEvent event : events ){
					if ( event.type() == BufferEvent.ADD ){
						// MSG Received
						if (! event.id().equals(PntScenario.root_id)){
							
							long reception_time = (time-Math.max(actual_msg_start_time,entry_times.get(event.id())))/1000;
							System.out.println(reception_time+":"+lifetime+":"+nSlot);
							int i=(int)reception_time/(lifetime/nSlot);
							++data[i];
							n_transfers++;
							
						}
					}
					if ( event.type() == BufferEvent.REMOVE ){
						// MSG EXPIRED
						if ( event.id().equals(PntScenario.root_id)){
							
						}
					}
					
					if ( event.type() == BufferEvent.IN ){
						present_node_list.add(event.id());
						if (!entry_times.containsKey(event.id()))
							entry_times.put(event.id(), time);
					}
					
					if ( event.type() == BufferEvent.OUT ){
						present_node_list.remove(event.id());
					}

				}
			}
		};
	}

	@Override
	public Listener<Buffer> bufferListener() {
		return null;
	}


}

