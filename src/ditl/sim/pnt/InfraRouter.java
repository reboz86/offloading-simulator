package ditl.sim.pnt;

import java.io.IOException;
import java.util.*;

import ditl.*;
import ditl.graphs.*;
import ditl.sim.*;

public class InfraRouter extends Router implements Generator, PresenceTrace.Handler {

	protected final PervasiveInfraRadio infra_radio;
	protected Map<Integer, Set<Integer>> msg_infected = new HashMap<Integer, Set<Integer>>();
	protected Map<Integer, Set<Integer>> msg_sane = new HashMap<Integer, Set<Integer>>();
	protected Bus<Message> msg_update_bus = new Bus<Message>();
	protected Bus<TransferOpportunity> opp_bus = new Bus<TransferOpportunity>();
	protected Set<Integer> present_ids = new HashSet<Integer>();
	protected PriorityQueue<TransferOpportunity> down_buffer = new PriorityQueue<TransferOpportunity>();
	
	protected final NumToPush num_to_push;
	protected final WhoToPush who_to_push;
	protected final long send_incr;
	protected final long panic_interval;
	protected final Long float_req_interval; 
	
	public InfraRouter(PervasiveInfraRadio infraRadio, WhoToPush whoToPush, NumToPush numToPush,
			long sendIncr, long panicInterval, Long floatReqInterval, Integer id, int bufferSize, Bus<BufferEvent> bus) {
		super(id, bufferSize, bus);
		infra_radio = infraRadio;
		who_to_push = whoToPush;
		num_to_push = numToPush;
		msg_update_bus.addListener(new MessageUpdateTrigger());
		opp_bus.addListener(new QueuedPushTrigger());
		send_incr = sendIncr;
		panic_interval = panicInterval;
		float_req_interval = floatReqInterval;
	}
	
	@Override
	public void newMessage(long time, Message msg) throws IOException {
		super.newMessage(time, msg);
		msg_infected.put(msg.msgId(), new HashSet<Integer>());
		msg_sane.put(msg.msgId(), new HashSet<Integer>(present_ids));
		if ( float_req_interval != null ){
			for ( Integer id : present_ids ){
				queuePush(time + float_req_interval, msg, id);
			}
		}
		queueMsgUpdate(time, msg);
		long panic_time = Math.max(time, msg.expirationTime()-panic_interval);
		queueMsgUpdate(panic_time, msg); // always trigger panic at right time
	}
	
	private void queueMsgUpdate(long time, Message msg){
		msg_update_bus.queue(time, msg);
	}
	
	private void queuePush(long time, Message msg, Integer id){
		if ( time < msg.expirationTime() ){ // no point in sending already expired messages
			opp_bus.queue(time, new TransferOpportunity(this, infra_radio.getClient(id), msg));
		}
	}
	
	@Override
	protected void receiveMessage(long time, Message msg, Radio radio) throws IOException {
		if ( msg instanceof AckMessage ){
			Integer from = msg.from().id();
			Integer acked_msg_id = ((AckMessage)msg).ackMsgId();
			opp_bus.removeFromQueueAfterTime(time, new TransferOpportunityMatcher(acked_msg_id, from));
			msg_sane.get(acked_msg_id).remove(from);
			msg_infected.get(acked_msg_id).add(from);
		}
	}
	
	@Override
	public void expireMessage(long time, Message msg) throws IOException {
		super.expireMessage(time, msg);
		msg_sane.remove(msg.msgId());
		msg_infected.remove(msg.msgId());
		msg_update_bus.removeFromQueueAfterTime(time, new MessageMatcher(msg));
		if ( num_to_push != null )
			num_to_push.expireMessage(msg);
	}

	@Override
	public Bus<?>[] busses() {
		return new Bus<?>[]{ msg_update_bus, opp_bus };
	}

	@Override
	public int priority() {
		return Trace.defaultPriority;
	}

	@Override
	public void incr(long time) throws IOException {}

	@Override
	public void seek(long time) throws IOException {}

	private final class MessageUpdateTrigger implements Listener<Message> {
		@Override
		public void handle(long time, Collection<Message> messages) {
			for ( Message msg : messages ){
				Set<Integer> infected = msg_infected.get(msg.msgId()); // here infected is the sum of actually infected and receiving nodes 
				Set<Integer> sane = msg_sane.get(msg.msgId()); 
				
				if ( panic(time,msg) ){
					Iterator<Integer> i = sane.iterator();
					while ( i.hasNext() ){
						Integer to = i.next();
						infected.add(to);
						tryPush(time, msg, to); // push to everyone
						i.remove();
					}
				} else {
					int n = num_to_push.numToPush(msg, time, infected.size(), present_ids.size() );
					n = Math.min(n, sane.size());
					for(int i=0; i<n; ++i){
						Integer next = who_to_push.whoToPush(msg,infected, sane);
						if ( next == null )
							break;
						sane.remove(next);
						infected.add(next);
						tryPush(time, msg, next);
					}
					
				}
				queueMsgUpdate(time + send_incr, msg);
			}
		}
	}
	
	private boolean panic(long time, Message msg){
		return ( msg.expirationTime() - time <= panic_interval );
	}
	
	private void tryPush(long time, Message msg, Integer dest_id){
		TransferOpportunity opp = new TransferOpportunity(this, infra_radio.getClient(dest_id), msg);
		tryPush(time, opp);
	}
	
	private void tryPush(long time, TransferOpportunity opp){
		if ( infra_radio.canPushDown(opp.to().id()) ){
			opp_bus.removeFromQueueAfterTime(time, new TransferOpportunityMatcher(opp.message().msgId(), opp.to().id()));
			infra_radio.startNewTransfer(time, opp);
		} else {
			down_buffer.add(opp);
		}
	}
	
	@Override
	public void abortTransfer(long time, Transfer transfer){
		super.abortTransfer(time, transfer);
		if ( transfer.from().equals(this) ){ // aborted downlink push
			if ( transfer.to().acceptMessage(transfer.message()) ){ // dest will still accept the message
				Message msg = transfer.message();
				Integer id = transfer.to().id();
				msg_infected.get(msg.msgId()).remove(id);
				msg_sane.get(msg.msgId()).add(id);
			}
		}
	}

	@Override
	public Listener<PresenceEvent> presenceEventListener() {
		return new Listener<PresenceEvent>(){
			@Override
			public void handle(long time, Collection<PresenceEvent> events) {
				for ( PresenceEvent pev : events ){
					Integer id = pev.id();
					if ( pev.isIn() ) {
						present_ids.add(id);
						for ( Set<Integer> sane : msg_sane.values() )
							sane.add(id);
						if ( float_req_interval != null ){
							for ( Message msg : messages ){
								queuePush(time+float_req_interval, msg, id);
							}
						}
					} else {
						present_ids.remove(id);
						for ( Set<Integer> sane : msg_sane.values() )
							sane.remove(id);
						for ( Set<Integer> infected : msg_infected.values() )
							infected.remove(id);
						opp_bus.removeFromQueueAfterTime(time, new TransferOpportunityMatcher(null, id));
						Iterator<TransferOpportunity> i = down_buffer.iterator();
						while ( i.hasNext() ){
							TransferOpportunity opp = i.next();
							if ( opp.to().id().equals(id) )
								i.remove();
						}
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
				Integer msgId = opp.message().msgId();
				Integer to = opp.to().id();
				msg_sane.get(msgId).remove(to);
				msg_infected.get(msgId).add(to);
				tryPush(time, opp);
			}
		}
	}
	
	@Override
	protected TransferOpportunity getBestTransferTo(long time, Radio radio, Router dest){
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
