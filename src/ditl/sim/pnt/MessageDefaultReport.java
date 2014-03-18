package ditl.sim.pnt;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import ditl.Bus;
import ditl.Generator;
import ditl.Listener;
import ditl.Reader;
import ditl.Report;
import ditl.Runner;
import ditl.Store.LoadTraceException;
import ditl.Store.NoSuchTraceException;
import ditl.WritableStore.AlreadyExistsException;
import ditl.cli.App;
import ditl.cli.ExportApp;
import ditl.graphs.Presence;
import ditl.graphs.PresenceEvent;
import ditl.graphs.PresenceTrace;
import ditl.graphs.cli.GraphOptions;
import ditl.transfers.Message;
import ditl.transfers.MessageEvent;
import ditl.transfers.MessageTrace;
import ditl.transfers.Transfer;
import ditl.transfers.TransferEvent;
import ditl.transfers.TransferTrace;

public class MessageDefaultReport extends Report 
implements PresenceTrace.Handler, TransferTrace.Handler,MessageTrace.Handler, Listener<Object>, Generator {
	
	private static String infraRouterOption = "infra-router-id";
	
	private static int infra_router_id;
	private long infra_transferred_up = 0;
	private long infra_transferred_down = 0;
	private long adhoc_transferred = 0;
	private long hypotetical_transferred = 0;
	
	private int infra_message_sent =0;
	private boolean online=false;
	private long msg_start_time=0;
	
	
	List<Integer> present_node_list= new ArrayList<Integer>();
	
	
	private Map<Integer,Long> entry_times = new HashMap<Integer,Long>();
	private List<Long> times_list= new ArrayList<Long>();

	protected long n_transfers = 0;
	protected int completed=0;

	protected long tot_delay = 0;

	private long totSize =0;

	private int msg_id;

	public MessageDefaultReport(OutputStream out, int msg_id) throws IOException {
		super(out);
		this.msg_id = msg_id;
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		App app = new ExportApp(){

			GraphOptions graph_options = new GraphOptions(GraphOptions.PRESENCE);
			Long min_time, max_time;
			private int msg_id;

			@Override
			protected void initOptions() {
				super.initOptions();
				graph_options.setOptions(options);
				options.addOption(null, minTimeOption, true, "Start analysis at time <arg>");
				options.addOption(null, maxTimeOption, true, "Stop analysis at time <arg>");
				options.addOption(null, infraRouterOption, true, "Infrastructure router id (default: -42)");
//				options.addOption(null, outputOption, true, "Write output file to <arg> (Default: system.out)");
				

			}

			@Override
			protected void run() throws IOException, NoSuchTraceException,
			AlreadyExistsException, LoadTraceException {
				
				// adding presence and transfers traces
				PresenceTrace presence_trace = (PresenceTrace)_store.getTrace(graph_options.get(GraphOptions.PRESENCE));
				TransferTrace infra_trace=(TransferTrace)_store.getTrace("infra_transfers");
				TransferTrace adhoc_trace=(TransferTrace)_store.getTrace("adhoc_transfers");
				MessageTrace message_trace=(MessageTrace)_store.getTrace("messages");
				
				if ( min_time == null )
					min_time = message_trace.minTime();
				else
					min_time *= 1000;
				if ( max_time == null )
					max_time = message_trace.maxTime();
				else
					max_time *= 1000;
				
				MessageDefaultReport r = new MessageDefaultReport(_out, msg_id);
				
				
				Bus<PresenceEvent> presenceEventBus = new Bus<PresenceEvent>();
				presenceEventBus.addListener(r.presenceEventListener());
				Reader<PresenceEvent> presenceReader = presence_trace.getReader();
				presenceReader.setBus(presenceEventBus);
				
				Bus<TransferEvent> infraTransferEventBus = new Bus<TransferEvent>();
				infraTransferEventBus.addListener(r.transferEventListener());
				Reader<TransferEvent> infraTransferReader =infra_trace.getReader();
				infraTransferReader.setBus(infraTransferEventBus);
				
				Bus<TransferEvent> adhocTransferEventBus = new Bus<TransferEvent>();
				adhocTransferEventBus.addListener(r.transferEventListener());
				Reader<TransferEvent> adhocTransferReader =adhoc_trace.getReader();
				adhocTransferReader.setBus(adhocTransferEventBus);
				
				Bus<MessageEvent> messageEventBus = new Bus<MessageEvent>();
				messageEventBus.addListener(r.messageEventListener());
				Reader<MessageEvent> messageReader = message_trace.getReader();
				messageReader.setBus(messageEventBus);
				
				
				Runner runner = new Runner(presence_trace.maxUpdateInterval(), min_time, max_time);
				runner.addGenerator(adhocTransferReader);
				runner.addGenerator(presenceReader);
				runner.addGenerator(infraTransferReader);
				runner.addGenerator(messageReader);
				
				runner.run();
				
				r.finish();
				presenceReader.close();
				adhocTransferReader.close();
				infraTransferReader.close();
				messageReader.close();
				
			}
			
			@Override
			protected String getUsageString(){
				return "[OPTIONS] STORE MSGID";
			}
			
			protected void parseArgs(CommandLine cli, String[] args)
					throws ParseException, ArrayIndexOutOfBoundsException,
					HelpException {
				super.parseArgs(cli, args);
				graph_options.parse(cli);
				if ( cli.hasOption(minTimeOption) )	
					min_time = Long.parseLong(cli.getOptionValue(minTimeOption));
				if ( cli.hasOption(maxTimeOption) )
					max_time = Long.parseLong(cli.getOptionValue(maxTimeOption));
				if ( cli.hasOption(infraRouterOption) )
					infra_router_id = Integer.parseInt(cli.getOptionValue(infraRouterOption));
				else
					infra_router_id = -42;
			
				
				msg_id = Integer.parseInt(args[1]);
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int priority() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void handle(long time, Collection<Object> events) throws IOException {
		
		
	}
	
	
	@Override
	public void finish() throws IOException {
		
		append("Total bytes transferred down: "+infra_transferred_down );
		append("Number of messages sent in INFRA: "+infra_message_sent);
		append("Total bytes transferred in ad hoc: "+adhoc_transferred );
		append("Hypotetical total bytes transferred in infra: "+hypotetical_transferred );
		append("Total bytes infrastructure savings: " + (hypotetical_transferred -(infra_transferred_down+infra_transferred_up)));
		
		append("Mean message delay : "+  tot_delay/n_transfers );
		
		
		//append("Fraction of users that receives messages before deadline : "+  (((Long)n_transfers).doubleValue() / totSize ));
		
		super.finish();
	}

	@Override
	public Listener<TransferEvent> transferEventListener() {
		return new Listener<TransferEvent>(){
			@Override
			public void handle(long time, Collection<TransferEvent> events)
					throws IOException {
				for ( TransferEvent tev : events ){
					// We are interested only in MSGID messages
					if (tev.msgId().equals(msg_id)){
						// We are not interested in  START EVENTS
						if(tev.type()==TransferEvent.ABORT || tev.type()==TransferEvent.COMPLETE){
							// if it is an INFRA TRANSFER
							if (tev.from() == infra_router_id){
								infra_transferred_down+=tev.bytesTransferred();
								
							}
//							}else if (tev.to() == infra_router_id ){
//								infra_transferred_up+=tev.bytesTransferred();
//
//							}
							// else is an ADHOC transfer
							else{
								adhoc_transferred+=tev.bytesTransferred();
							}
						}
						
						if(tev.type()== TransferEvent.START && tev.from() == infra_router_id)
							infra_message_sent++;
						
						// Compute the complete time
						if (tev.type() == ditl.sim.TransferEvent.COMPLETE){
							long  entry_time;
							if (entry_times.containsKey(tev.to()))
								entry_time=entry_times.get(tev.to());
							else
								entry_time=msg_start_time;
							
							tot_delay+=(time-entry_time);
							
							n_transfers++;
							
							hypotetical_transferred+=tev.bytesTransferred();
									
							}
						}
					}
				}
			
		};
	}

	@Override
	public Listener<Transfer> transferListener() {
		return null;
	}



	@Override
	public Listener<Presence> presenceListener() {
		return new Listener<Presence>(){
			@Override
			public void handle(long time, Collection<Presence> events) throws IOException {}
		};
	}

	
	@Override
	public Listener<PresenceEvent> presenceEventListener() {
		return new Listener<PresenceEvent>(){


			@Override
			public void handle(long time, Collection<PresenceEvent> events) throws IOException {
				for ( PresenceEvent pev : events ){
					if(online){
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
			}

		};
	}

	@Override
	public Listener<MessageEvent> messageEventListener() {
		return new Listener<MessageEvent>(){

			@Override
			public void handle(long time, Collection<MessageEvent> events) throws IOException {
				for (MessageEvent mev: events){
					if (mev.msgId()==msg_id && mev.isNew()){
						online=true;
						msg_start_time= time;
					}
					if (mev.msgId()==msg_id && !mev.isNew())
						online=false;
				}
			}
		};
	}

	@Override
	public Listener<Message> messageListener() {
		// TODO Auto-generated method stub
		return null;
	}


}

