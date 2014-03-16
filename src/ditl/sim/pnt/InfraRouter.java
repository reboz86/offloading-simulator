package ditl.sim.pnt;

import java.io.IOException;
import java.util.*;

import ditl.*;
import ditl.graphs.*;
import ditl.sim.*;

public class InfraRouter extends Router implements Generator, PresenceTrace.Handler {
	
	private static final boolean D= false;

	protected final PervasiveInfraRadio infra_radio;
	protected Map<Integer, Set<Integer>> msg_infected = new HashMap<Integer, Set<Integer>>();
	protected Map<Integer, Set<Integer>> msg_sane = new HashMap<Integer, Set<Integer>>();
	protected Bus<Message> msg_update_bus = new Bus<Message>();
	protected Bus<TransferOpportunity> opp_bus = new Bus<TransferOpportunity>();
	protected Set<Integer> present_ids = new HashSet<Integer>();
	private Set<Message> active_messages = new HashSet<Message>();
	protected PriorityQueue<TransferOpportunity> down_buffer = new PriorityQueue<TransferOpportunity>();
	
	protected final NumToPush num_to_push;
	protected  WhoToPush who_to_push;
	protected final long send_incr;
	protected final long panic_interval;
	protected final Long float_req_interval;
	protected final long guard_time;
	
	protected int capInfra;
	protected int infraSent;
	
	public InfraRouter(PervasiveInfraRadio infraRadio, WhoToPush whoToPush, NumToPush numToPush,
			long sendIncr, long panicInterval, long guardTime, Long floatReqInterval, Integer id, int bufferSize, Bus<BufferEvent> bus, int capInfra) {
		super(id, bufferSize, bus);
		infra_radio = infraRadio;
		who_to_push = whoToPush;
		num_to_push = numToPush;
		msg_update_bus.addListener(new MessageUpdateTrigger());
		opp_bus.addListener(new QueuedPushTrigger());
		send_incr = sendIncr;
		panic_interval = panicInterval;
		float_req_interval = floatReqInterval;
		guard_time = guardTime;
		this.capInfra=capInfra;
		
	}
	
	public void changeWho(WhoToPush whoToPush){
		who_to_push=whoToPush;
	}
	
	// this is called by world when a new message is created
	@Override
	public void newMessage(long time, Message msg) throws IOException {
		if(D ) System.out.println(this.id()+" InfraRouter.newMessage:"+time+";"+msg.toString());
		super.newMessage(time, msg);

		active_messages.add(msg);
		msg_infected.put(msg.msgId(), new HashSet<Integer>());
		msg_sane.put(msg.msgId(), new HashSet<Integer>(present_ids));

		// actual_recipients  initialized with the present nodes that are recipients of the message
		Set<Integer> actual_recipients=msg.getRecipientsId();								//ONLY for SCENARIO1
		//if (D && msg.msgId()==0)System.out.println(actual_recipients);
		if(actual_recipients!=(null)){
			msg_sane.put(msg.msgId(), actual_recipients);
			//if (D &&msg.msgId()==0)System.out.println("Sane:"+msg_sane.get(msg.msgId()));


			if ( float_req_interval != null ){
				for ( Integer id : present_ids ){
					queuePush(time + float_req_interval, msg, id);
				}
			}
		}
		
		queueMsgUpdate(time, msg);
		long panic_time = Math.max(time, msg.expirationTime()-panic_interval);
		queueMsgUpdate(panic_time, msg); // always trigger panic at right time
		infraSent=0;
	}


	private void queueMsgUpdate(long time, Message msg){
		msg_update_bus.queue(time, msg);
	}
	
	private void queuePush(long time, Message msg, Integer id){
		if ( time < msg.expirationTime() ){ // no point in sending already expired messages
			opp_bus.queue(time, new TransferOpportunity(this, infra_radio.getClient(id), msg));
		}
	}
	
	@Override// should serve to receive ack from peers that receive messages from the ad hoc interface
	protected void receiveMessage(long time, Message msg, Radio radio) throws IOException {
		if ( msg instanceof AckMessage ){
			Integer from = msg.from().id();
			Integer acked_msg_id = ((AckMessage)msg).ackMsgId();
			opp_bus.removeFromQueueAfterTime(time, new TransferOpportunityMatcher(acked_msg_id, from));

			//if(D ) System.out.println(this.id()+" InfraRouter.receiveMessage ACK received: "+time+";"+msg.from().id()+";"+msg.msgId());

			// remove from the list of sane nodes if needed
			if (msg_sane.containsKey(acked_msg_id))
				msg_sane.get(acked_msg_id).remove(from);
			//if (msg.msgId()==0)System.out.println("receive Message. Sane:"+msg_sane.get(acked_msg_id));
			//}
			// infected maintain the list of all nodes infected (also those not interested in the content)
			if (msg_infected.containsKey(acked_msg_id))
				msg_infected.get(acked_msg_id).add(from);
		
	}
	}

	@Override
	public void expireMessage(long time, Message msg) throws IOException {
		super.expireMessage(time, msg);
		
		if(D ) System.out.println(this.id()+" InfraRouter.expireMessage:"+time+";"+msg.toString());
		msg_sane.remove(msg.msgId());
		if (D &&msg.msgId()==0)System.out.println("expire Message. "+msg.msgId());
		msg_infected.remove(msg.msgId());
		active_messages.remove(msg);
		
		msg_update_bus.removeFromQueueAfterTime(time, new MessageMatcher(msg));
		if ( num_to_push != null )
			num_to_push.expireMessage(msg);
		for ( Iterator<Transfer> i = incoming_transfers.iterator(); i.hasNext(); ){
			Transfer transfer = i.next();
			Message m = transfer.message();
			if ( m instanceof AckMessage ){
				if ( ((AckMessage)m).ackMsgId().equals(msg.msgId()) ){
					i.remove();
					transfer.abort(time);
				}
			}
		}
	}

	@Override
	public Bus<?>[] busses() {
		return new Bus<?>[]{ msg_update_bus, opp_bus };
	}

	@Override
	public int priority() {
		return Trace.defaultPriority+1; // more than MessageGenerator changed @filippo perché cosi non invio messaggi inutili quando il messaggio é scaduto
	}

	@Override
	public void incr(long time) throws IOException {}

	@Override
	public void seek(long time) throws IOException {}

	private final class MessageUpdateTrigger implements Listener<Message> {
		@Override
		public void handle(long time, Collection<Message> messages) {
			
			for ( Message msg : messages ){
				//if(D && msg.msgId()<3 ) System.out.println("InfraRouter.MessageUpdateTrigger.handle:"+time+";"+msg.toString()+";"+msg.from().id());
				
				// We need to compute the real number of infected users interested in the content as it is possible that for some strategies 
				// they may have received the content although not interested 
				// infected initially includes all the nodes that are infected, we are interested in nodes that are also recipients
				Set<Integer> infected = new HashSet<Integer>(msg_infected.get(msg.msgId())); 
			//	infected.retainAll(msg.getRecipientsId()); // intersection of total number of infected with the recipient's list
//				Set<Integer> pres= new HashSet<Integer>(present_ids); // @TODO only for SCENARIO 2
			//	infected.retainAll(pres); // intersection of infected AND recipients with the present nodes 
				// this is done because I push only to a node that is present and that is a recipient 
				
				Set<Integer> sane = new HashSet<Integer>(msg_sane.get(msg.msgId())); 
				if (D)System.out.println("MessageUpdatetriggerMessage. Sane:"+msg_sane.get(msg.msgId()));
				// same treatment done to sane nodes
			//	
			
				
				// PANIC ZONE: send to all the uninfected
				if ( panic(time,msg) ){
				//	if (D && msg.msgId()==0)System.out.println("Panic!!! Sane:"+msg_sane.get(msg.msgId()));
					//if (D) System.out.println("PANIC ZONE: "+time+":"+msg.msgId());
					// we infect only sane recipients
					Iterator<Integer> i = sane.iterator();
					while ( i.hasNext() ){
						Integer to = i.next();
						infected.add(to);
						//msg_sane.get(msg.msgId()).remove(to);
						tryPush(time, msg, to); // push to everyone
						
						msg_sane.get(msg.msgId()).remove(to);
						msg_infected.get(msg.msgId()).add(to);
						
						if(D)System.out.println("MessageUpdateTrigger	tryPush:"+time+";"+msg.msgId()+";"+to);
						i.remove();
					}
					// ELSE send to sane using provided 
				} else {
					
					//if( time>=4200000 && time<4250000) System.out.println("SANE");
					long push_time = Math.max(msg.creationTime()+guard_time, time);
					
					 
					// intersection of total number of sane with the present list
					// infected and sane are already of the right dimension due to  previous elaborations
					
					//sane.retainAll(pres);// ONLY FOR SCENARIO2
					//infected.retainAll(pres);
					
					int n = num_to_push.numToPush(msg, push_time, infected.size(),sane.size() );
					//if(D && msg.msgId()<3 ) System.out.println("InfraRouter: numtopush:"+n);
					n = Math.min(n, sane.size());
					 //System.out.println("n= "+n+";"+num_to_push.numToPush(msg, push_time, infected.size(), present_ids.size() ));
					for(int i=0; i<n; ++i){
						Integer next = who_to_push.whoToPush(msg,infected, sane);
						if ( next == null )
							break;
						//if(D  ) System.out.println("InfraRouter.MessageUpdateTrigger.handle:"+time+";"+msg.toString()+";"+msg.from().id()+";"+next);
						if(capInfra>0 && infraSent<capInfra){
							
							//update of the local Set (sane and infected) and overall Map (msg_sane and msg_infected)
							sane.remove(next);
							infected.add(next);
							
							msg_sane.get(msg.msgId()).remove(next);
							msg_infected.get(msg.msgId()).add(next);
							
							tryPush(time, msg, next);
							infraSent++;
						
							if(D && msg.msgId()==0)System.out.println("MessageUpdateTrigger	tryPush:"+time+";"+msg.toString()+";"+msg.from().id()+";"+next);
							if (D && msg.msgId()==0)System.out.println("MessageUpdatetriggerMessage. Not panic Sane:"+msg_sane.get(msg.msgId()));
						}
					}

				}
				//if(D) System.out.println("QueueMsgUpdate:"+msg.msgId()+":"+ (time + send_incr));
				queueMsgUpdate(time + send_incr, msg); 
			}
		}
	}
	
	private boolean panic(long time, Message msg){
		return ( msg.expirationTime() - time <= panic_interval );
	}
	
	private void tryPush(long time, Message msg, Integer dest_id){
		TransferOpportunity opp = new TransferOpportunity(this, infra_radio.getClient(dest_id), msg);
		if (D)System.out.println(infra_radio.getClient(dest_id).id());
		tryPush(time, opp);
	}
	
	private void tryPush(long time, TransferOpportunity opp){
		if ( infra_radio.canPushDown(opp.to().id()) ){
			opp_bus.removeFromQueueAfterTime(time, new TransferOpportunityMatcher(opp.message().msgId(), opp.to().id()));
			if(D ) System.out.println("InfraRouter.tryPush:"+time+";"+opp.message().toString()+";"+opp.message().from().id()+";"+opp.to().id());
			infra_radio.startNewTransfer(time, opp);
			//msg_sane.get(opp.message().msgId()).remove(opp.to().id());
			//msg_infected.get(opp.message().msgId()).add(opp.to().id());
		} else {
			down_buffer.add(opp);
		}
	}
	
	@Override
	public void abortTransfer(long time, Transfer transfer){
		if(D && transfer.message().msgId()<3) System.out.println("InfraRouter.AbortTransfer:"+time+";"+transfer.toString());
		super.abortTransfer(time, transfer);
		if ( transfer.from().equals(this) ){ // aborted downlink push
			if ( transfer.to().acceptMessage(transfer.message()) ){ // dest will still accept the message
				Message msg = transfer.message();
				Integer id = transfer.to().id();
				msg_infected.get(msg.msgId()).remove(id);
				msg_sane.get(msg.msgId()).add(id);
				if (msg.msgId()==0)System.out.println("Abort Transfer. Sane:"+msg_sane.get(msg.msgId()));
			}
		}
	}

	@Override
	public Listener<PresenceEvent> presenceEventListener() {
		return new Listener<PresenceEvent>(){
			@Override
			public void handle(long time, Collection<PresenceEvent> events) {
				//if(D) System.out.println("InfraRouter.presenceEventlistener.handle:"+time);
				for ( PresenceEvent pev : events ){
					Integer id = pev.id();
					if ( pev.isIn() ) {
						present_ids.add(id);
						//NODE IN	// ONLY FOR SCeNARIO1!!!!!!!!!!!!!!
						for (Message msg : active_messages){ 				
							if(msg.getRecipientsId()!=null){
								//in case the node is a recipient for the message and has not received the content yet, then it is added to msg_sane list
								if(msg.getRecipientsId().contains(id) && !msg_infected.get(msg.msgId()).contains(id)){
									Set<Integer> sane=msg_sane.get(msg.msgId());

									if (sane == null)
										sane= new HashSet<Integer>();
									sane.add(id);
									msg_sane.put(msg.msgId(), sane);
								
								} // else  the node is not a recipient or has already received the content then do nothing
							}
						}

						if ( float_req_interval != null ){
							for ( Message msg : messages ){
								queuePush(time+float_req_interval, msg, id);
							}
						}
					} else {
						//NODE OUT
						present_ids.remove(id);


						//						for (Message msg : active_messages){
						//							if(msg.getRecipientsId()!=null){
						//								// the node is a recipient for a message
						//								if(msg.getRecipientsId().contains(id)){
//									
//									if (D&& msg.msgId()==0)System.out.println("PresenceEventListener OUT"+id+". Sane:"+msg_sane.get(msg.msgId()));
//									// the node is infected
//									if(msg_infected.get(msg.msgId()).contains(id)){
//										//DO NOTHING
//									}
//									//the node is sane
//									if(msg_sane.get(msg.msgId()).contains(id)){
//
//										//DO NOTHING
//										// the message has not been received yet
//										//tryPush(time, msg, id);
//										//if(D )System.out.println("	tryPush:"+time+";"+msg.toString()+";"+msg.from().id()+";"+id);
//										//Set<Integer> sane=msg_sane.get(msg.msgId());
//										//sane.remove(id);
//										//msg_sane.put(msg.msgId(),sane);
//									}
//
//								}
//							}
//						}

					}

					// we avoid to eliminate the router from sane and infected
					//						for ( Set<Integer> sane : msg_sane.values() )
					//							sane.remove(id);
					//						for ( Set<Integer> infected : msg_infected.values() )
					//							infected.remove(id);
					opp_bus.removeFromQueueAfterTime(time, new TransferOpportunityMatcher(null, id));
					Iterator<TransferOpportunity> i = down_buffer.iterator();
					while ( i.hasNext() ){
						TransferOpportunity opp = i.next();
						if ( opp.to().id().equals(id) )
							i.remove();
					}
				}
			}

		};
	}

	@Override
	public Listener<Presence> presenceListener() {
		return new Listener<Presence>(){
			@Override
			public void handle(long time, Collection<Presence> events) {
				//if(D) System.out.println("InfraRouter.presenceListener.handle:"+time);
				for ( Presence p : events ){
					if ( ! p.id().equals(_id)) // ignore self
						present_ids.add(p.id());
				}
			}
		};
	}
	
	private final static class MessageMatcher implements Matcher<Message> {
		final Integer msg_id;
		MessageMatcher(Message msg){ msg_id = msg.msgId(); }
		@Override
		public boolean matches(Message msg) {
			return msg_id.equals(msg.msgId());
		}
	}
	
	private final static class TransferOpportunityMatcher implements Matcher<TransferOpportunity> {
		final Integer _id;
		final Integer msg_id;
		TransferOpportunityMatcher(Integer msgId, Integer id){ msg_id = msgId; _id = id; }
		@Override
		public boolean matches(TransferOpportunity opp) {
			if ( msg_id == null ) return _id.equals(opp.to().id());
			if ( _id == null ) return msg_id.equals(opp.message().msgId());
			return msg_id.equals(opp.message().msgId()) && _id.equals(opp.to().id());
		}
	}
	
	private final class QueuedPushTrigger implements Listener<TransferOpportunity> {
		@Override
		public void handle(long time, Collection<TransferOpportunity> opps) {
			
			for ( TransferOpportunity opp : opps ){
				if(D) System.out.println("InfraRouter.QueuedPushTrigger:"+time);
				Integer msgId = opp.message().msgId();
				Integer to = opp.to().id();
				msg_sane.get(msgId).remove(to);
				if (msgId==0)System.out.println("Queued Push Trigger. Sane:"+msg_sane.get(msgId));
				msg_infected.get(msgId).add(to);
				tryPush(time, opp);
			}
		}
	}
	
	@Override
	protected TransferOpportunity getBestTransferTo(long time, Radio radio, Router dest){
		//if (D) System.out.println("InfraRouter.getBestTransferTo");
		Iterator<TransferOpportunity> i = down_buffer.iterator();
		while ( i.hasNext() ){
			TransferOpportunity opp = i.next();
			if ( opp.to().id().equals(dest.id()) ){
				i.remove();
				return opp;
			}
		}
		return null;
	}

}
