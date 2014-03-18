package ditl.sim.pnt;

import java.io.FileOutputStream;
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
import ditl.Listener;
import ditl.Reader;
import ditl.Report;
import ditl.Runner;
import ditl.Store.LoadTraceException;
import ditl.Store.NoSuchTraceException;
import ditl.WritableStore.AlreadyExistsException;
import ditl.cli.App;
import ditl.cli.ExportApp;
import ditl.cli.App.HelpException;
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
import ditl.transfers.Transfer;
import ditl.transfers.TransferEvent;
import ditl.transfers.TransferTrace;

public class MeanDelayReport extends Report 
implements PresenceTrace.Handler,MessageTrace.Handler,TransferTrace.Handler{

	List<Integer> present_node_list= new ArrayList<Integer>();
	
	private static int infra_router_id= -42;

	private Map<Integer,Long> entry_times = new HashMap<Integer,Long>();
	protected Integer msgId;
	protected long msgStartTime;
	protected int infra_message_sent =0;
	protected int n_transfers =0;
	protected int tot_delay =0;
	protected int mean_delay =0;

	private PresenceTrace presenceTrace;
	private MessageTrace messageTrace;
	private TransferTrace infraTransferTrace;

	public MeanDelayReport(String out_file_name,PresenceTrace presence,MessageTrace message, TransferTrace infra_transfer) throws IOException {
		super((out_file_name != null)? new FileOutputStream(out_file_name) : System.out);
		
		this.presenceTrace=presence;
		this.messageTrace= message;
		this.infraTransferTrace= infra_transfer;
		
	 
		
	}

	public void exec() throws IOException{
		
		System.out.println("MeanDelayReport");
		
		long min_time = messageTrace.minTime();
		
		long max_time = messageTrace.maxTime();
		

		Bus<PresenceEvent> presenceEventBus = new Bus<PresenceEvent>();
		presenceEventBus.addListener(this.presenceEventListener());
		Reader<PresenceEvent> presenceReader = presenceTrace.getReader();
		presenceReader.setBus(presenceEventBus);
		
		Bus<MessageEvent> messageEventBus = new Bus<MessageEvent>();
		messageEventBus.addListener(this.messageEventListener());
		Reader<MessageEvent> messageReader = messageTrace.getReader();
		messageReader.setBus(messageEventBus);
		
		Bus<TransferEvent> infraTransferEventBus = new Bus<TransferEvent>();
		infraTransferEventBus.addListener(this.transferEventListener());
		Reader<TransferEvent> infraTransferReader =infraTransferTrace.getReader();
		infraTransferReader.setBus(infraTransferEventBus);
		
		
		
		Runner runner = new Runner(presenceTrace.maxUpdateInterval(), min_time, max_time);
		runner.addGenerator(infraTransferReader);
		runner.addGenerator(presenceReader);
		runner.addGenerator(messageReader);
		
		System.out.println("MeanDelayReport.Run");
		
		runner.run();
		
		System.out.println("out MeanDelayReport.Run");
		
		append("/n");
		
		presenceReader.close();
		infraTransferReader.close();
		messageReader.close();
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
	public Listener<MessageEvent> messageEventListener() {
		return new Listener<MessageEvent>(){

			@Override
			public void handle(long time, Collection<MessageEvent> events) throws IOException {
				for (MessageEvent mev: events){
					if (mev.isNew()){
						msgId=mev.msgId();
						msgStartTime= time;
					}
					
					


					if(!mev.isNew()){
						// compute mean delay and num of infra messages
						if (n_transfers!=0){
							mean_delay=tot_delay/n_transfers;
							append(mean_delay+"/t");
						}
						// reinitialization of variables
						tot_delay=0;
						n_transfers=0;
						infra_message_sent=0;
						
						

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

	@Override
	public Listener<TransferEvent> transferEventListener() {
		return new Listener<TransferEvent>(){
			@Override
			public void handle(long time, Collection<TransferEvent> events)
					throws IOException {
				for ( TransferEvent tev : events ){
					// We are interested only in MSGID messages
					if (tev.msgId().equals(msgId)){
						// We are not interested in  START EVENTS
						
						if(tev.type()== TransferEvent.START && tev.from() == infra_router_id)
							infra_message_sent++;

						// Compute the complete time
						if (tev.type() == ditl.sim.TransferEvent.COMPLETE){
							long entry_time=Math.max(entry_times.get(tev.to()),msgStartTime);

							tot_delay+=(time-entry_time);
							n_transfers++;


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

}
