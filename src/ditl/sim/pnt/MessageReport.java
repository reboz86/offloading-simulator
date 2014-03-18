package ditl.sim.pnt;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ditl.Listener;
import ditl.Report;

import ditl.graphs.Presence;
import ditl.graphs.PresenceEvent;
import ditl.graphs.PresenceTrace;
import ditl.sim.Message;
import ditl.sim.MessageEvent;
import ditl.transfers.TransferEvent;


public class MessageReport extends Report implements  ditl.transfers.TransferTrace.Handler, Listener<MessageEvent>, PresenceTrace.Handler{

	private static int infra_router_id = -42;
	
	private Map<Integer,MsgStat> messages= new HashMap<Integer,MsgStat>();
	int ackSize;
	
	private Set<Integer> presenceList= new HashSet<Integer>();

	private long panic;
	

	public MessageReport(String out_file_name, int ackSize, boolean append, long panic_time) throws IOException {

		super((out_file_name != null)? new FileOutputStream(out_file_name,append) : System.out);
		if(!append)
			append("#Content\tn-adhoc\tbytes-adhoc\tn-infra-up\n-infra-down\tbytesinfra-up\tbytes-infradown\tn-recipients\tbytes-infra-only");
		this.ackSize= ackSize;
		this.panic=panic_time;
	}

	@Override
	public void handle(long time, Collection<MessageEvent> events) throws IOException {
		for (MessageEvent mev: events){
			
			if (mev.isNew()){
				//System.out.println("New Message");
				// create a new void Map entry
				MsgStat msg=new MsgStat(mev.message(),time,(mev.message().expirationTime()-mev.message().creationTime()));
				messages.put(mev.message().msgId(), msg);
			}

			if(!mev.isNew()){
				System.out.println("Expire Message:"+ mev.message().msgId()+":"+mev.message().getRecipients().size());
//				MsgStat message=messages.get(mev.message().msgId());
				//System.out.println("A");
//				for(int id:message.presenceList){
//					//System.out.println(id);
//					long elapsed=time-message.entryTime.get(id)+message.elapsedTime.get(id);
//					message.elapsedTime.put(id, elapsed);
//				}

				// Extract and Print the stats
				messages.get(mev.message().msgId()).writeDown();
				messages.remove(mev.message().msgId());
				//System.out.println(mev.message().msgId()+" REMOVED!!");
			}
		}
	}

	@Override
	public Listener<ditl.sim.TransferEvent> transferEventListener() {
		return new Listener<ditl.sim.TransferEvent>(){
			@Override
			public void handle(long time, Collection<ditl.sim.TransferEvent> events)
					throws IOException {
				
				
				for ( ditl.sim.TransferEvent tev : events ){
					
					// We are interested only in MSGID messages
					MsgStat stat=messages.get(tev.transfer().message().msgId());
					if (stat!=null){
						
						// We are not interested in  START EVENTS
						if(tev.type()==TransferEvent.ABORT || tev.type()==TransferEvent.COMPLETE){
	
							// if it is an INFRA TRANSFER
							if (tev.transfer().from().id() == infra_router_id){
								stat.bytes_tx_infra_down+=tev.bytesTransferred();
								//System.out.println(time+":"+(stat.msg.expirationTime()-panic));
								if(time>=(stat.msg.expirationTime()-panic))
										stat.bytes_panic+=tev.bytesTransferred();
							}
							
							// else is an ADHOC transfer
							else{
								stat.bytes_tx_adhoc+=tev.bytesTransferred();
							}
						}

						if(tev.type()== TransferEvent.COMPLETE && tev.transfer().from().id() == infra_router_id){
							stat.n_infra_transferred_down++;
							stat.receptionList.add(tev.transfer().to().id());
							if(time>=(stat.msg.expirationTime()-panic))
									stat.n_panic++;
						}

						else if(tev.type()== TransferEvent.COMPLETE && tev.transfer().to().id() != infra_router_id && tev.transfer().from().id() != infra_router_id){
							stat.n_adhoc_transferred++;
							stat.receptionList.add(tev.transfer().to().id());
						}
					}
				}
			}
		};
	}
	
	
	@Override
	public Listener<Presence> presenceListener() {

		return null;
	}

	@Override
	public Listener<PresenceEvent> presenceEventListener() {
		return new Listener<PresenceEvent>(){

			@Override
			public void handle(long time, Collection<PresenceEvent> events)
					throws IOException {
				for(PresenceEvent pres : events){
					for(MsgStat message : messages.values()){
						//System.out.println("B");
						if(pres.isIn()){
							presenceList.add(pres.id());
//							message.entryTime.put(pres.id(), time);
//							if(message.elapsedTime.get(pres.id())==null)
//								message.elapsedTime.put(pres.id(), (long)0);
						}else{
							//							System.out.println("time:"+time);
							//							System.out.println("message:"+message.msgId);
							//							System.out.println("out:"+pres.id());
							//							System.out.println("lastEntry:"+message.lastEntryTime.get(pres.id()));
							//							System.out.println("elapsed:"+message.elapsedTime.get(pres.id()));
//							if (message.entryTime.get(pres.id())!=null){
//								long elapsed=(time-message.entryTime.get(pres.id()))+message.elapsedTime.get(pres.id());// in the case that node enters and exit some times
//								message.entryTime.remove(pres.id());
								//System.out.println(time+":"+pres.id()+ " message:"+message.msgId+" elapsed:"+elapsed);
//								message.elapsedTime.put(pres.id(), elapsed);
							presenceList.remove(pres.id());
//							}
						}
					}

				}
			}
		};
	}

	public void close() throws IOException{
		finish();
	}
	
	@Override
	public Listener<ditl.sim.Transfer> transferListener() {
		// TODO Auto-generated method stub
		return null;
	}

	private class MsgStat{
		
		
		private int n_infra_transferred_down;
		private int n_adhoc_transferred;
		private int n_panic;

		
		private long bytes_tx_infra_down;
		private long bytes_tx_adhoc;
		private long bytes_panic;
		
		private int n_infra_only;
		private long bytes_infra_only;
		
		private int msgId;
		private long delay;
		
		private Message msg;
		private Set<Integer> receptionList;

//		private Map<Integer,Long> entryTime= new HashMap<Integer,Long>();
//		private Map<Integer,Long> elapsedTime = new HashMap<Integer,Long>();
		

		MsgStat(Message msg,long time, long delay){

			
			n_infra_transferred_down=0;
			n_adhoc_transferred=0;
			n_panic=0;
			
			bytes_tx_infra_down=0;
			bytes_tx_adhoc=0;
			bytes_panic=0;
			
			n_infra_only=msg.getRecipients().size();
			
			receptionList=new HashSet<Integer>();
		//	presenceList=new HashSet<Integer>(msg.getRecipientsId());
			this.msg=msg;
			this.delay=delay/1000;
			
			
//			for(int id : msg.getRecipientsId()){
//				//System.out.println(id);
//				elapsedTime.put(id, (long)0);
//				entryTime.put(id, time);
//			}
			
			this.msgId=msg.msgId();
		}
		
		public void writeDown() throws IOException{
			Set<Integer> presentNodes= new HashSet<Integer>(presenceList);
			if(msg.getRecipientsId()!=null){
				presentNodes.retainAll(msg.getRecipientsId());
				
				n_infra_only=msg.getRecipients().size();
				
				bytes_infra_only=n_infra_only*msg.bytes();
				
				
//			Set<Entry<Integer,Long>> elapsed= elapsedTime.entrySet();
//			Iterator<Entry<Integer, Long>> iter = elapsed.iterator();
		
//			int elapsed75=0;
//			int received75=0;
//			int elapsed50=0;
//			int received50=0;
//			int elapsed25=0;
//			int received25=0;
//			int elapsed10=0;
//			int received10=0;
//			int elapsed0=0;
//			int received0=0;
//
//			while (iter.hasNext()){
//				Entry<Integer, Long> entry= iter.next();
//
//				if (!msg.getRecipientsId().contains(entry.getKey())){
//
//					// not recipients
//					//75%<D<100%			
//					if (entry.getValue()>=delay*(3/4) && entry.getValue()<delay ){
//						
//						System.out.println(entry.toString());
//						elapsed75++;
//						if (receptionList.contains(entry.getKey()))
//							received75++;
//					}				//50%<D<75%
//					else if (entry.getValue()>=delay/2 && entry.getValue()<delay*(3/4)  ){
//						elapsed50++;
//						if (receptionList.contains(entry.getKey()))
//							received50++;
//					}				//25%<D<50%
//					else if (entry.getValue()>=delay/4 && entry.getValue()<delay/2 ){
//						elapsed25++;
//						if (receptionList.contains(entry.getKey()))
//							received25++;
//					}				//10%<D<25%
//					else if (entry.getValue()>=delay/10&& entry.getValue()<delay/4 ){
//						elapsed10++;
//						if (receptionList.contains(entry.getKey()))
//							received10++;
//					}				//0%<D<10%
//					else if (entry.getValue()>0 && entry.getValue()<delay/10){
//						elapsed0++;
//						if (receptionList.contains(entry.getKey()))
//							received0++;
//					}
//				}
//			}
				System.out.println(msgId+"\t"+n_adhoc_transferred+"\t"+bytes_tx_adhoc+"\t"+n_adhoc_transferred+"\t"+n_infra_transferred_down+"\t"+
					(n_adhoc_transferred*ackSize)+"\t"+bytes_tx_infra_down+"\t"+n_infra_only+"\t"+bytes_infra_only+"\t"+n_panic+"\t"+bytes_panic+"\t"+delay);

				append(msgId+"\t"+n_adhoc_transferred+"\t"+bytes_tx_adhoc+"\t"+n_adhoc_transferred+"\t"+n_infra_transferred_down+"\t"+
					(n_adhoc_transferred*ackSize)+"\t"+bytes_tx_infra_down+"\t"+n_infra_only+"\t"+bytes_infra_only+"\t"+n_panic+"\t"+bytes_panic+"\t"+delay);
			}

		}
	}
}
