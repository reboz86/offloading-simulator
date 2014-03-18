/**
 * @author filippo
 */


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
import ditl.sim.World;
import ditl.transfers.Message;
import ditl.transfers.MessageEvent;
import ditl.transfers.MessageTrace;
import ditl.transfers.Transfer;
import ditl.transfers.TransferEvent;
import ditl.transfers.TransferTrace;

public class TransferReport extends Report 
implements PresenceTrace.Handler, TransferTrace.Handler, MessageTrace.Handler, Listener<Object>, Generator {
	
	private static String infraRouterOption = "infra-router-id";
	
	private static int infra_router_id;
	private long infra_transferred_up = 0;
	private long infra_transferred_down = 0;
	private long adhoc_transferred = 0;
	private long hypotetical_transferred = 0;
	
	List<Integer> present_node_list= new ArrayList<Integer>();
	
	private Map<Integer,Long> pending_messages= new HashMap<Integer,Long>();
	private Map<Integer,List<Integer>>message_map = new HashMap<Integer,List<Integer>>();
	
	private Map<Integer,ArrayList<Long>> delay_map= new HashMap<Integer,ArrayList<Long>>();
	private Map<Integer,HashMap<Integer,Long>> creation_map= new HashMap<Integer,HashMap<Integer,Long>>();

	protected long n_transfers = 0;

	protected long tot_delay = 0;

	private long totSize =0;

	public TransferReport(OutputStream out) throws IOException {
		super(out);
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		App app = new ExportApp(){

			GraphOptions graph_options = new GraphOptions(GraphOptions.PRESENCE);
			Long min_time, max_time;

			@Override
			protected void initOptions() {
				super.initOptions();
				graph_options.setOptions(options);
				options.addOption(null, minTimeOption, true, "Start analysis at time <arg>");
				options.addOption(null, maxTimeOption, true, "Stop analysis at time <arg>");
				options.addOption(null, infraRouterOption, true, "Infrastructure router id (default: -42)");

			}

			@Override
			protected void run() throws IOException, NoSuchTraceException,
			AlreadyExistsException, LoadTraceException {
				
				// adding presence and transfers traces
				PresenceTrace presence_trace = (PresenceTrace)_store.getTrace(graph_options.get(GraphOptions.PRESENCE));
				TransferTrace infra_trace=(TransferTrace)_store.getTrace("infra_transfers");
				TransferTrace adhoc_trace=(TransferTrace)_store.getTrace("adhoc_transfers");
				MessageTrace message=(MessageTrace)_store.getTrace("messages");
				
				if ( min_time == null )
					min_time = message.minTime();
				else
					min_time *= 1000;
				if ( max_time == null )
					max_time = message.maxTime();
				else
					max_time *= 1000;
				
				TransferReport r = new TransferReport(System.out);
				
				
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
				Reader<MessageEvent> messageReader = message.getReader();
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
				return "[OPTIONS] STORE";
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
		append("Total bytes transferred up: "+infra_transferred_up );
		append("Total bytes transferred in ad hoc: "+adhoc_transferred );
		append("Hypotetical total bytes transferred in infra: "+hypotetical_transferred );
		append("Total bytes infrastructure savings: " + (hypotetical_transferred -(infra_transferred_down+infra_transferred_up)));
		
		append("Mean message delay : "+  tot_delay/n_transfers );
		
		
		append("Fraction of users that receives messages before deadline : "+  (((Long)n_transfers).doubleValue() / totSize ));
		
		super.finish();
	}

	@Override
	public Listener<TransferEvent> transferEventListener() {
		return new Listener<TransferEvent>(){
			@Override
			public void handle(long time, Collection<TransferEvent> events)
					throws IOException {
				for ( TransferEvent tev : events ){
					// We are not interested in  START EVENTS
					if(tev.type()==TransferEvent.ABORT || tev.type()==TransferEvent.COMPLETE){
						// if it is an INFRA TRANSFER
						if (tev.from() == infra_router_id){
							infra_transferred_down+=tev.bytesTransferred();

						}else if (tev.to() == infra_router_id ){
							infra_transferred_up+=tev.bytesTransferred();

						}
						// else is an ADHOC transfer
						else{
							adhoc_transferred+=tev.bytesTransferred();
						}
					}


					if (tev.type() == ditl.sim.TransferEvent.COMPLETE && creation_map.containsKey(tev.msgId())){
						Map<Integer,Long>created_map=creation_map.get(tev.msgId());
						if(created_map.containsKey(tev.to())){
							ArrayList<Long> list= delay_map.get(tev.msgId());
							list.add(time-created_map.get(tev.to()));
							delay_map.put(tev.msgId(), list);
							
							n_transfers++;
							tot_delay+=time-created_map.get(tev.to());
						
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

			List<Integer> node_list;

			@Override
			public void handle(long time, Collection<PresenceEvent> events) throws IOException {
				for ( PresenceEvent pev : events ){
					if ( pev.isIn() ){
						present_node_list.add(pev.id());

						for (Integer msgId:pending_messages.keySet()){
							node_list= message_map.get(msgId);
							if(!node_list.contains(pev.id())){
								node_list.add(pev.id());
								message_map.put(msgId,node_list);
							}
							
							HashMap<Integer,Long>creation=creation_map.get(msgId);
							if(creation!=null){
								if (!creation.containsKey(pev.id())){
									creation.put(pev.id(), time);
									totSize++;
									
								}
								creation_map.put(msgId, creation);
							}
						}
						
						
					}

					else{
						present_node_list.remove(pev.id());
					}

				}
			}

		};
	}

	@Override
	public Listener<MessageEvent> messageEventListener() {
		return new Listener<MessageEvent>(){

			List<Integer> node_list;

			@Override
			public void handle(long time, Collection<MessageEvent> events)
					throws IOException {
				for ( MessageEvent mev : events ){
					
					if ( mev.isNew() ){
						//System.out.println("totSize: "+totSize);
						pending_messages.put(mev.msgId(),mev.size());
						
						node_list=new ArrayList<Integer>();
						for (Integer nodeId :present_node_list ){
							node_list.add(nodeId);
						}
						
						message_map.put(mev.msgId(), node_list);
						
						//delay_map.put(mev.msgId(), new ArrayList<Long>());
						Map<Integer,Long> present_creation_time= new HashMap<Integer,Long>();
						
						for (Integer node_id:present_node_list){
							
							present_creation_time.put(node_id, time);
							totSize++;

						}
						
						creation_map.put(mev.msgId(), (HashMap<Integer, Long>) present_creation_time);
						delay_map.put(mev.msgId(), new ArrayList<Long>());

					}else{
						// count the number of bytes that I should send if only in INFRA
						if (pending_messages != null){
							for (Integer msgId:pending_messages.keySet()){
								long msg_size= pending_messages.get(msgId);
	
								node_list= message_map.get(msgId);
								hypotetical_transferred+=node_list.size()*msg_size;
							}
						}
						
						
						pending_messages.remove(mev.msgId());
						message_map.remove(mev.msgId());

					}
				}

			}
		};
	}

	@Override
	public Listener<Message> messageListener() {
		
		return null;
	}

}
