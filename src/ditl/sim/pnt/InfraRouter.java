package ditl.sim.pnt;

import java.io.IOException;
import java.util.*;

import ditl.*;
import ditl.graphs.*;
import ditl.sim.*;

public class InfraRouter extends Router implements Generator, Listener<Message>, PresenceTrace.Handler {

	protected final PervasiveInfraRadio infra_radio;
	protected Map<Integer, Set<Integer>> msg_infected = new HashMap<Integer, Set<Integer>>();
	protected Map<Integer, Set<Integer>> msg_sane = new HashMap<Integer, Set<Integer>>();
	protected Bus<Message> msg_update_bus = new Bus<Message>();
	protected Set<Integer> present_ids = new HashSet<Integer>();
	
	protected final NumToPush num_to_push;
	protected final WhoToPush who_to_push;
	protected final long send_incr;
	protected final long panic_interval;
	
	public InfraRouter(PervasiveInfraRadio infraRadio, WhoToPush whoToPush, NumToPush numToPush,
			long sendIncr, long panicInterval, Integer id, int bufferSize, Bus<BufferEvent> bus) {
		super(id, bufferSize, bus);
		infra_radio = infraRadio;
		who_to_push = whoToPush;
		num_to_push = numToPush;
		msg_update_bus.addListener(this);
		send_incr = sendIncr;
		panic_interval = panicInterval;
	}
	
	@Override
	public void newMessage(long time, Message msg) throws IOException {
		super.newMessage(time, msg);
		msg_infected.put(msg.msgId(), new HashSet<Integer>());
		msg_sane.put(msg.msgId(), new HashSet<Integer>(present_ids));
		queueMsgUpdate(time, msg);
		queueMsgUpdate(msg.expirationTime()-panic_interval, msg); // always trigger panic at right time
	}
	
	private void queueMsgUpdate(long time, Message msg){
		msg_update_bus.queue(time, msg);
	}
	
	@Override
	protected void receiveMessage(long time, Message msg, Radio radio) throws IOException {
		if ( msg instanceof AckMessage ){
			Integer from = msg.from().id();
			Integer acked_msg_id = ((AckMessage)msg).ackMsgId();
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
		num_to_push.expireMessage(msg);
	}

	@Override
	public Bus<?>[] busses() {
		return new Bus<?>[]{ msg_update_bus };
	}

	@Override
	public int priority() {
		return Trace.defaultPriority;
	}

	@Override
	public void incr(long time) throws IOException {}

	@Override
	public void seek(long time) throws IOException {}

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
	
	private boolean panic(long time, Message msg){
		return ( msg.expirationTime() - time <= panic_interval );
	}
	
	private void tryPush(long time, Message msg, Integer dest_id){
		if ( infra_radio.canPushDown(dest_id) ){
			TransferOpportunity opp = new TransferOpportunity(this, infra_radio.getClient(dest_id), msg);
			infra_radio.startNewTransfer(time, opp);
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
					} else {
						present_ids.remove(id);
						for ( Set<Integer> sane : msg_sane.values() )
							sane.remove(id);
						for ( Set<Integer> infected : msg_infected.values() )
							infected.remove(id);
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
				for ( Presence p : events )
					present_ids.add(p.id());
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

}
