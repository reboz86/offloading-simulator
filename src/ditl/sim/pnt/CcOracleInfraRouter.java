package ditl.sim.pnt;

import java.io.IOException;
import java.util.Collection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import ditl.Bus;
import ditl.Generator;
import ditl.Listener;
import ditl.Matcher;
import ditl.Trace;

import ditl.graphs.Link;
import ditl.graphs.LinkEvent;
import ditl.graphs.Presence;
import ditl.graphs.PresenceEvent;
import ditl.graphs.PresenceTrace;
import ditl.graphs.LinkTrace;
import ditl.sim.BufferEvent;
import ditl.sim.Message;
import ditl.sim.PervasiveInfraRadio;
import ditl.sim.RNG;
import ditl.sim.Radio;
import ditl.sim.Router;
import ditl.sim.Transfer;
import ditl.sim.TransferOpportunity;

public class CcOracleInfraRouter extends Router implements Generator, PresenceTrace.Handler,LinkTrace.Handler{
	
	protected static final boolean D = false;
	protected final PervasiveInfraRadio infra_radio;
	protected Map<Integer, Set<Integer>> msg_infected = new HashMap<Integer, Set<Integer>>();
	protected Map<Integer, Set<Integer>> msg_sane = new HashMap<Integer, Set<Integer>>();
	protected Bus<Message> msg_update_bus = new Bus<Message>();
	private Set<Message> active_messages = new HashSet<Message>();
	protected PriorityQueue<TransferOpportunity> down_buffer = new PriorityQueue<TransferOpportunity>();
	private long panic_interval;
	
	private Map<Integer,Set<Integer>> _neighbors = new HashMap<Integer,Set<Integer>>();;
	private List<Set<Integer>> _ccs;
	private Set<Integer> present = new HashSet<Integer>();

	public CcOracleInfraRouter(PervasiveInfraRadio infraRadio, long panicInterval, Integer id, int bufferSize, Bus<BufferEvent> bus) {
		super(id, bufferSize, bus);
		infra_radio = infraRadio;
		msg_update_bus.addListener(new MessageUpdateTrigger());
		panic_interval = panicInterval;
	}


	@Override
	public void newMessage(long time, Message msg) throws IOException {
		super.newMessage(time, msg);
		msg_infected.put(msg.msgId(), new HashSet<Integer>());
		active_messages.add(msg);
		//msg_sane.put(msg.msgId(), new HashSet<Integer>(present_ids));
		
		
		Set<Integer> actual_recipients=msg.getRecipientsId();
		//if (true)System.out.println(time+ ": RECIPIENTS: "+actual_recipients.size());
		if(actual_recipients!=(null)){ 
			msg_sane.put(msg.msgId(), actual_recipients);
		}
		
		ccRecalc();
		//Send a message to a random node in each CC
		Iterator<Set<Integer>> iter=_ccs.iterator();
		if(true)System.out.println(time+": Initial Injection"+_ccs.size());
		while (  iter.hasNext() ){
				Set<Integer> component = iter.next();
				Integer cc_node = RNG.randomFromSet(component);
				if (component.size()>1){
					tryPush(time, msg, cc_node);
					msg_sane.get(msg.msgId()).remove(cc_node);
					msg_infected.get(msg.msgId()).add(cc_node);
					if(D)System.out.println(time+":"+cc_node.intValue());
				}
			}
		
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
			if (msg_sane.containsKey(acked_msg_id))
				msg_sane.get(acked_msg_id).remove(from);
			if (msg_infected.containsKey(acked_msg_id))
				msg_infected.get(acked_msg_id).add(from);
		}
	}


	@Override
	public Listener<PresenceEvent> presenceEventListener() {
		return new Listener<PresenceEvent>(){
			@Override
			public void handle(long time, Collection<PresenceEvent> events) {
				for ( PresenceEvent pev : events ){
					if ( pev.isIn() ){
						present.add(pev.id());

						for (Message msg : active_messages){ 	

							Set<Integer> sane=msg_sane.get(msg.msgId());

							if (sane == null)
								sane= new HashSet<Integer>();
							sane.add(pev.id());
							msg_sane.put(msg.msgId(), sane);
						}

					} else {
						present.remove(pev.id());

						Iterator<TransferOpportunity> i = down_buffer.iterator();
						while ( i.hasNext() ){
							TransferOpportunity opp = i.next();
							if ( opp.to().id().equals(pev.id()) )
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
				for ( Presence p : events )
					present.add(p.id());
			}
		};
	}

	@Override
	public void expireMessage(long time, Message msg) throws IOException {
		super.expireMessage(time, msg);
		
		active_messages.remove(msg);
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
				
//				Set<Integer> infected = new HashSet<Integer>(msg_infected.get(msg.msgId())); // here infected is the sum of actually infected and receiving nodes 
//				Set<Integer> sane = new HashSet<Integer>(msg_sane.get(msg.msgId())); 
//				
//				infected.retainAll(msg.getRecipientsId()); // intersection of total number of infected with the recipient's list
//				Set<Integer> pres= new HashSet<Integer>(present_ids);
//				infected.retainAll(pres); // intersection of infected AND recipients with the present nodes 
//				// this is done because I push only to a node that is present and that is a recipient 
//				
//				if (D)System.out.println("MessageUpdatetriggerMessage. Sane:"+msg_sane.get(msg.msgId()));
				// same treatment done to sane nodes
				//sane.retainAll(pres);
				Set<Integer> pres= new HashSet<Integer>(present);
				Set<Integer> sane = new HashSet<Integer>(msg_sane.get(msg.msgId())); 
				//sane.retainAll(pres);
				
				if(true)System.out.println(time+": Panic Injection"+sane.size());
				
				if ( panic(time,msg) ){
					
					Iterator<Integer> i = sane.iterator();
					while ( i.hasNext() ){
						Integer to = i.next();
						msg_infected.get(msg.msgId()).add(to);
						msg_sane.get(msg.msgId()).remove(to);
						tryPush(time, msg, to); // push to everyone
						//if(true)System.out.println(time+":"+to);
						i.remove();
					}
				}
			}
		}
	}


	@Override
	public Listener<Link> linkListener() {
		return new Listener<Link>(){
			@Override
			public void handle(long time, Collection<Link> events) {
				for ( Link p : events ){
						
				}
					
			}
		};
		
	}

	@Override
	public Listener<LinkEvent> linkEventListener() {
		return new Listener<LinkEvent>(){
			@Override
			public void handle(long time, Collection<LinkEvent> events) {
				for ( LinkEvent p : events ){
					if(p.isUp()){

						//create entry if does not exist
						if(_neighbors.get(p.id1())== null)
							_neighbors.put(p.id1(),new HashSet<Integer>());

						Set<Integer> neigh = _neighbors.get(p.id1());
						neigh.add(p.id2());
						_neighbors.put(p.id1(), neigh);

						//create entry if does not exist
						if(_neighbors.get(p.id2())== null)
							_neighbors.put(p.id2(),new HashSet<Integer>());

						//if(present.contains(p.id2()))
						neigh = _neighbors.get(p.id2());
						neigh.add(p.id1());
						_neighbors.put(p.id2(), neigh);
					}
					else
						//if(present.contains(p.id1()))
						if(_neighbors.get(p.id1())!= null)
							_neighbors.get(p.id1()).remove(p.id2());
						//if(present.contains(p.id2()))
					if(_neighbors.get(p.id2())!= null)
						_neighbors.get(p.id2()).remove(p.id1());
						
				}
					
			}
		};
	}
	
	private void ccRecalc(){
		Set<Integer> toVisit = new HashSet<Integer>(present);
		_ccs = new LinkedList<Set<Integer>>();
		while( ! toVisit.isEmpty() ){
			LinkedList<Integer> inCC = new LinkedList<Integer>();
			Set<Integer> curCC = new HashSet<Integer>();
			Integer i = null;
			for ( Integer k : toVisit ){
				i = k;
				toVisit.remove(k);
				break;
			}
			inCC.add(i);
			while( ! inCC.isEmpty() ){
				i = inCC.pop();
				curCC.add(i);
				Set<Integer> neighbs = _neighbors.get(i);
				if ( neighbs != null ){
					for ( Integer j : neighbs ){
						if ( toVisit.contains(j) ){
							toVisit.remove(j);
							inCC.add(j);
						}
					}
				}
			}
			_ccs.add(curCC);
		}
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

}