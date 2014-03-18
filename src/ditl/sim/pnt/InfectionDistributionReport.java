package ditl.sim.pnt;

import java.io.*;
import java.util.*;

import org.apache.commons.cli.*;

import ditl.*;
import ditl.Store.LoadTraceException;
import ditl.Store.NoSuchTraceException;
import ditl.WritableStore.AlreadyExistsException;
import ditl.Reader;
import ditl.cli.*;
import ditl.graphs.*;
import ditl.graphs.cli.GraphOptions;
import ditl.transfers.*;

public class InfectionDistributionReport extends MultipleReports
	implements PresenceTrace.Handler, BufferTrace.Handler, MessageTrace.Handler, Listener<Object>, Generator {

	private int infected = 0;
	private int present = 0;
	private Set<Integer> infected_ids = new HashSet<Integer>();
	private Bus<Object> update_bus = new Bus<Object>();
	private long start_time;
	private long end_time;
	private static String report_prefix;
	protected int message_counter=0;
	GraphOptions graph_options = new GraphOptions(GraphOptions.PRESENCE);
	MessageTrace message;
	BufferTrace buffer;
	PresenceTrace presence;
	protected long message_creation_time;
	
	
	public InfectionDistributionReport(String prefix,  long startTime, long endTime, MessageTrace message, BufferTrace buffer, PresenceTrace presence ) throws IOException {
		super();
		start_time = startTime;
		end_time= endTime;
		update_bus.addListener(this);
		this.message=message;
		this.buffer=buffer;
		this.presence=presence;
		report_prefix=prefix;
				
		
	}
	
	public InfectionDistributionReport(OutputStream out,  long startTime, long endTime ) throws IOException {
		super();
		start_time = startTime;
		end_time= endTime;
		update_bus.addListener(this);
		
	}
	
	private void queueUpdate(long time){
		update_bus.queue(time, Collections.emptySet());
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
							infected += 1;
							infected_ids.add(event.id());
						}
					}
					if ( event.type() == BufferEvent.REMOVE ){
						// MSG EXPIRED
						if (! event.id().equals(PntScenario.root_id)){
							infected -= 1;
							infected_ids.remove(event.id());
						}
					}

				}
				if(time <end_time && time>= start_time){
					queueUpdate(time);
				}
			}
		};
	}

	@Override
	public Listener<Buffer> bufferListener() {
		return null;
	}

	@Override
	public Listener<PresenceEvent> presenceEventListener() {
		return new Listener<PresenceEvent>(){
			@Override
			public void handle(long time, Collection<PresenceEvent> events) throws IOException {
				for ( PresenceEvent pev : events ){
					if ( pev.isIn() && pev.id()!= PntScenario.root_id){
						present += 1;
						if ( infected_ids.contains(pev.id()))
							infected +=1;
					} else if ( !pev.isIn() && pev.id()!= PntScenario.root_id){
						present -= 1;
						if ( infected_ids.contains(pev.id()))
							infected -=1;
					}
					if(time <end_time && time>= start_time){
						queueUpdate(time);
					}
				}
			}
		};
	}

	@Override
	public Listener<Presence> presenceListener() {
		return new Listener<Presence>(){
			@Override
			public void handle(long time, Collection<Presence> events) throws IOException {
				present = events.size();
				if(time <end_time && time>= start_time){
					queueUpdate(time);
				}
			}
		};
	}
	
	
	@Override
	public Listener<MessageEvent> messageEventListener() {
		return new Listener<MessageEvent>(){
			@Override
			public void handle(long time, Collection<MessageEvent> events) throws FileNotFoundException, IOException {
				if(time <end_time && time>= start_time){
					for ( MessageEvent mev : events ){
						if ( mev.isNew() ){
							// new message
							message_creation_time=time;
							message_counter++;
							newReport(new FileOutputStream(report_prefix+message_counter+".txt"));

						} else {
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
	
	
	public void exec() throws NoSuchTraceException, IOException{
	
		if (start_time == -1) start_time=presence.minTime();
		if (end_time == -1) end_time = presence.maxTime();
		
		//Presence
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
		
		Runner runner = new Runner(buffer.maxUpdateInterval(), presence.minTime(), presence.maxTime());
		runner.addGenerator(bufferReader);
		runner.addGenerator(presenceReader);
		runner.addGenerator(messageReader);
		runner.addGenerator(this);
		
		runner.run();
		
		//System.out.println("InfectionDistribution.finish");
		this.finish();
		
	}
	
	
	public static void main(String args[]) throws IOException{
		App app = new ExportApp(){

			GraphOptions graph_options = new GraphOptions(GraphOptions.PRESENCE);
			Long min_time, max_time;
			private final String startOption= "start-time";
			private final String finishOption= "stop-time";
			private  String reportprefixOption= "report-prefix";
			
			@Override
			protected void initOptions() {
				super.initOptions();
				graph_options.setOptions(options);
				//options.addOption(null, shiftOption, true, "Shift starting time by <arg>");
				
				options.addOption(null, startOption, true, "Start analysis at time <arg>");
				options.addOption(null, finishOption, true, "Stop analysis at time <arg>");
				options.addOption(null, reportprefixOption, true, "Name of the reports <arg> (Default: report-msg-)");
			}
			
			@Override
			protected void run() throws IOException, NoSuchTraceException,
					AlreadyExistsException, LoadTraceException {
				PresenceTrace presence = (PresenceTrace)_store.getTrace(graph_options.get(GraphOptions.PRESENCE));
				BufferTrace buffers = (BufferTrace)_store.getTrace("buffers");
				MessageTrace messages = (MessageTrace)_store.getTrace("messages");
				
				if (min_time == -1) min_time=presence.minTime();
				if (max_time == -1) max_time = presence.maxTime();
				
				InfectionDistributionReport r = new InfectionDistributionReport(_out,min_time, max_time);
				
				//Presence
				Bus<Presence> presenceBus = new Bus<Presence>();
				presenceBus.addListener(r.presenceListener());
				Bus<PresenceEvent> presenceEventBus = new Bus<PresenceEvent>();
				presenceEventBus.addListener(r.presenceEventListener());
				StatefulReader<PresenceEvent,Presence> presenceReader = presence.getReader();
				presenceReader.setBus(presenceEventBus);
				presenceReader.setStateBus(presenceBus);
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
				
				Runner runner = new Runner(buffers.maxUpdateInterval(), presence.minTime(), presence.maxTime());
				runner.addGenerator(bufferReader);
				runner.addGenerator(presenceReader);
				runner.addGenerator(messageReader);
				runner.addGenerator(r);
				
				runner.run();
				
				r.finish();
				presenceReader.close();
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
				report_prefix=cli.getOptionValue(reportprefixOption,"report-msg-");
				
				
			}
			
		};
		app.ready("", args);
		app.exec();
	}


	
	@Override
	public void handle(long time, Collection<Object> arg1) throws IOException {
			double r = (double)(infected)/present;
			append((time)+" "+r);
	}

	@Override
	public void incr(long arg0) throws IOException {}

	@Override
	public void seek(long arg0) throws IOException {}

	@Override
	public Bus<?>[] busses() {
		return new Bus<?>[]{update_bus};
	}

	@Override
	public int priority() {
		return Integer.MAX_VALUE;
	}
}
