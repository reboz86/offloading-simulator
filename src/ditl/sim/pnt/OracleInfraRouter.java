package ditl.sim.pnt;

import java.io.IOException;
import java.util.*;

import ditl.*;
import ditl.graphs.*;
import ditl.sim.*;

public class OracleInfraRouter extends Router implements Generator, PresenceTrace.Handler, GroupTrace.Handler {

	protected final PervasiveInfraRadio infra_radio;
	protected Map<Integer, Set<Integer>> msg_infected = new HashMap<Integer, Set<Integer>>();
	protected Map<Integer, Set<Integer>> msg_sane = new HashMap<Integer, Set<Integer>>();
	protected Bus<Message> msg_update_bus = new Bus<Message>();
	protected Set<Integer> present_ids = new HashSet<Integer>();
	protected PriorityQueue<TransferOpportunity> down_buffer = new PriorityQueue<TransferOpportunity>();
	protected Group ds;
	
	protected final long panic_interval;
	
	public OracleInfraRouter(PervasiveInfraRadio infraRadio, long panicInterval, Integer id, int bufferSize, Bus<BufferEvent> bus) {
		super(id, bufferSize, bus);
		infra_radio = infraRadio;
		msg_update_bus.addListener(new MessageUpdateTrigger());
		panic_interval = panicInterval;
	}
	
	@Override
	public void newMessage(long time, Message msg) throws IOException {
		super.newMessage(time, msg);
		msg_infected.put(msg.msgId(), new HashSet<Integer>());
		msg_sane.put(msg.msgId(), new HashSet<Integer>(present_ids));
		long panic_time = Math.max(time, msg.expirationTime()-panic_interval);
		queueMsgUpdate(panic_time, msg); // always trigger panic at right time
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
				}
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
					} else {
						present_ids.remove(id);
						for ( Set<Integer> sane : msg_sane.values() )
							sane.remove(id);
						for ( Set<Integer> infected : msg_infected.values() )
							infected.remove(id);
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

	@Override
	public Listener<GroupEvent> groupEventListener() {
		return new Listener<GroupEvent>(){
			@Override
			public void handle(long time, Collection<GroupEvent> events) {
				for ( GroupEvent gev : events ){
					switch ( gev.type() ){
					case GroupEvent.NEW:
						ds = new Group(gev.gid());
						break;
					case GroupEvent.JOIN:
						ds.handleEvent(gev);
						for ( Integer ds_node : gev.members() ){
							for ( Message msg : messages ){ 
								tryPush(time, msg, ds_node);
								msg_sane.get(msg.msgId()).remove(ds_node);
								msg_infected.get(msg.msgId()).add(ds_node);
							}
						}
						break;
					case GroupEvent.LEAVE:
						ds.handleEvent(gev);
						break;
					}
				}
			}
		};
	}

	@Override
	public Listener<Group> groupListener() {
		return new Listener<Group>(){
			@Override
			public void handle(long time, Collection<Group> groups) {
				for ( Group g : groups ){
					ds = g;
					break;
				}
				if ( ds != null ){
					for ( Integer ds_node : ds.members() ){
						for ( Message msg : messages ){ 
							tryPush(time, msg, ds_node);
							msg_sane.get(msg.msgId()).remove(ds_node);
							msg_infected.get(msg.msgId()).add(ds_node);
						}
					}
				}
			}
		};
	}

}
