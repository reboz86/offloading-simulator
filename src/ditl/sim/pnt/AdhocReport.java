package ditl.sim.pnt;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import ditl.Bus;
import ditl.Generator;
import ditl.Listener;
import ditl.Reader;
import ditl.Report;
import ditl.Runner;
import ditl.graphs.Presence;
import ditl.graphs.PresenceEvent;
import ditl.graphs.PresenceTrace;
import ditl.transfers.Message;
import ditl.transfers.MessageEvent;
import ditl.transfers.MessageTrace;
import ditl.transfers.Transfer;
import ditl.transfers.TransferEvent;
import ditl.transfers.TransferTrace;

public class AdhocReport extends Report implements PresenceTrace.Handler, TransferTrace.Handler,MessageTrace.Handler, Listener<Object>, Generator{

	private static int infra_router_id= -42;
	private PresenceTrace presenceTrace;
	private MessageTrace messageTrace;
	private TransferTrace adhocTransferTrace;
	
	private int[] adHocTx;
	private int[] adHocRx;
	protected int contentNumber;
	protected Integer contentId;
	
	private boolean reportType; //report type 0 for Transmission, 1 for Reception
	
	
	
	public AdhocReport(String out_file_name,PresenceTrace presence,MessageTrace message, TransferTrace adhoc_transfer, long numNodes, boolean type) throws IOException {

		super((out_file_name != null)? new FileOutputStream(out_file_name) : System.out);

		this.presenceTrace=presence;
		this.messageTrace= message;
		this.adhocTransferTrace=adhoc_transfer;
		
		adHocTx= new int[(int) numNodes];
		adHocRx= new int[(int) numNodes];
		
		this.reportType=type;

	}
	
	
public void exec() throws IOException{
		
		System.out.println("AdHocReport");
		
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

		Bus<TransferEvent> adhocTransferEventBus = new Bus<TransferEvent>();
		adhocTransferEventBus.addListener(this.transferEventListener());
		Reader<TransferEvent> adhocTransferReader =adhocTransferTrace.getReader();
		adhocTransferReader.setBus(adhocTransferEventBus);
		
		
		
		Runner runner = new Runner(presenceTrace.maxUpdateInterval(), min_time, max_time);
		runner.addGenerator(adhocTransferReader);
		runner.addGenerator(presenceReader);
		runner.addGenerator(messageReader);
		
		
		append("# Content","\t");
		for (int i=1;i<=adHocTx.length;i++){
			append("Node "+i,"\t");
		}
		append("\n");
		
		
		runner.run();
		
		presenceReader.close();
		adhocTransferReader.close();
		messageReader.close();
		finish();
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
		// TODO Auto-generated method stub
		
	}



	@Override
	public Listener<MessageEvent> messageEventListener() {
		return new Listener<MessageEvent>(){

			@Override
			public void handle(long time, Collection<MessageEvent> events) throws IOException {
				for (MessageEvent mev: events){
					if (mev.isNew()){

						contentNumber++;
						contentId=mev.msgId();
						
						for (int i=0; i<adHocTx.length;i++){
							adHocTx[i]=0;
							adHocRx[i]=0;
						}

					}


					if(!mev.isNew()){
						append(contentNumber,"\t");
						for (int i=1;i<=adHocTx.length;i++){
							if (reportType==false)
								append(adHocTx[i-1],"\t");
							else
								append(adHocRx[i-1],"\t");
						}
						append("\n");
						
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
					// We are not interested in  START EVENTS
					if(tev.type()==TransferEvent.COMPLETE && tev.from() != infra_router_id && tev.to() != infra_router_id){
						adHocTx[tev.from()]++;
						adHocRx[tev.to()]++;

					}
				}


			}
		};
	}



	@Override
	public Listener<Transfer> transferListener() {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Listener<Presence> presenceListener() {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Listener<PresenceEvent> presenceEventListener() {
		// TODO Auto-generated method stub
		return null;
	}
}
