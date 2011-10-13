package ditl.sim;

import java.io.IOException;
import java.util.*;

import ditl.*;
import ditl.graphs.*;

public class AdHocRadio extends Radio implements LinkTrace.Handler, Listener<TransferEvent> {
	
	private AdjacencyMatrix adjacency = new AdjacencyMatrix();
	private Map<Link,Transfer> active_transfers = new HashMap<Link,Transfer>();
	private Set<Integer> active_node_ids = new HashSet<Integer>();
	private BitrateGenerator bitrate_generator;
	private World _world;
	
	public AdHocRadio(World world, BitrateGenerator bitrateGenerator){
		_world = world;
		addListener(this);
		bitrate_generator = bitrateGenerator;
	}
	
	@Override
	public Bus<?>[] busses() {
		return new Bus<?>[]{this};
	}

	@Override
	public int priority() {
		return Trace.defaultPriority;
	}

	@Override
	public void incr(long time) throws IOException {}

	@Override
	public void seek(long time) throws IOException {}
	
	private boolean canTransmit(Integer id){ // node can transmit if none of its neighbors are transmitting
		if ( ! active_node_ids.contains(id) ){
			Set<Integer> neighbors = adjacency.getNext(id);
			if ( neighbors != null ){
				for ( Integer k : neighbors )
					if ( active_node_ids.contains(k) )
						return false;
				return true;
			}
		}
		return false;
	}

	@Override
	public Listener<LinkEvent> linkEventListener() {
		return new Listener<LinkEvent>(){
			Listener<LinkEvent> adj_listener = adjacency.linkEventListener();
			@Override
			public void handle(long time, Collection<LinkEvent> events)
					throws IOException {
				adj_listener.handle(time, events);
				Set<Integer> new_nodes_can_transmit = new HashSet<Integer>();
				for ( LinkEvent lev : events ){
					Link l = lev.link();
					if ( lev.isUp() ){
						if ( canTransmit(l.id1()) && canTransmit(l.id2()) ){
							new_nodes_can_transmit.add(l.id1());
							new_nodes_can_transmit.add(l.id2());
						}
					} else {
						Transfer act_trans = active_transfers.get(l);
						if ( act_trans != null ){
							queue(time, act_trans.getAbortEvent(time));
						}
						else if ( active_node_ids.contains(l.id1()) )
							new_nodes_can_transmit.add(l.id2());
						else if ( active_node_ids.contains(l.id2()) )
							new_nodes_can_transmit.add(l.id1());
					}
				}
				if ( ! new_nodes_can_transmit.isEmpty() )
					tryStartNewTransfer(time, new_nodes_can_transmit);
			}
			
		};
	}
	
	protected void tryStartNewTransfer(long time, Set<Integer> can_transmit){
		while ( ! can_transmit.isEmpty() ){
			Integer id = popRandomId(can_transmit);
			Router r = _world.getRouterById(id);
			TransferOpportunity opp = r.getBestTransferOpportunity(time, this);
			if ( opp != null ){
				startNewTransfer(time, opp);
				updateCanTransmit(can_transmit);
			}
		}
	}
	
	private void updateCanTransmit(Set<Integer> can_transmit){
		for ( Iterator<Integer> i=can_transmit.iterator(); i.hasNext(); )
			if ( ! canTransmit(i.next()) )
				i.remove();
	}
	
	private Integer popRandomId(Set<Integer> can_transmit){
		Random rng = RNG.getInstance();
		int k = rng.nextInt(can_transmit.size());
		int j=0;
		for ( Integer i : can_transmit ){
			if ( j >= k ){
				can_transmit.remove(i);
				return i;
			}
			j++;
		}
		return null; // should never get here
	}

	@Override
	public Listener<Link> linkListener() {
		final Bus<TransferEvent> thisBus = this;
		return new StatefulListener<Link>(){
			StatefulListener<Link> adj_listener = (StatefulListener<Link>) adjacency.linkListener();
			@Override
			public void reset() {
				adj_listener.reset();
				active_transfers.clear();
				thisBus.reset();
				active_node_ids.clear();
			}

			@Override
			public void handle(long time, Collection<Link> links) throws IOException {
				adj_listener.handle(time, links);
				Set<Integer> ids = new HashSet<Integer>();
				for ( Link l : links ){
					ids.add(l.id1());
					ids.add(l.id2());
				}
				tryStartNewTransfer(time, ids);
			}
		};
	}

	private void stopTransfer(long time, Transfer transfer){
		Integer from = transfer.from().id();
		Integer to = transfer.to().id();
		// stop transfer
		active_transfers.remove(transfer.link());
		active_node_ids.remove(from);
		active_node_ids.remove(to);
		// signal new router ready for transmission
		Set<Integer> router_ids = new HashSet<Integer>();
		Set<Integer> neighbs = adjacency.getNext(to);
		if ( neighbs != null ){
			router_ids.add(from);
			router_ids.addAll(neighbs);
		}
		neighbs = adjacency.getNext(from);
		if ( neighbs != null ){
			router_ids.add(to);
			router_ids.addAll(neighbs);
		}
		
		for ( Iterator<Integer> i = router_ids.iterator(); i.hasNext(); ){
			Integer k = i.next();
			if ( ! canTransmit(k) )
				i.remove();
		}
		tryStartNewTransfer(time, router_ids);
	}
	
	public void startNewTransfer(long time, TransferOpportunity opp){
		Transfer transfer = opp.toTransfer(this, time, bitrate_generator.getNext());
		transfer.start(time);
		queue(time, transfer.getStartEvent());
		queue(transfer.getCompleteTime(), transfer.getCompleteEvent());
		active_node_ids.add(opp.from().id());
		active_node_ids.add(opp.to().id());
		active_transfers.put(transfer.link(), transfer);
	}

	@Override
	public void handle(long time, Collection<TransferEvent> events) throws IOException {
		Set<Transfer> aborted_transfers = new HashSet<Transfer>();
		for ( TransferEvent event : events ){
			final Transfer transfer = event.transfer();
			if ( event.type() == TransferEvent.ABORT ){
				removeFromQueueAfterTime(time, new Matcher<TransferEvent>(){
					@Override
					public boolean matches(TransferEvent event) {
						return event.transfer() == transfer;
					}
				});
				aborted_transfers.add(transfer);
				stopTransfer(time, event.transfer());
			}
		}
		
		for ( TransferEvent event : events ){
			if ( event.type() == TransferEvent.COMPLETE ){
				Transfer transfer2 = event.transfer();
				if ( ! aborted_transfers.contains(transfer2) ){
					transfer2.complete(time);
					stopTransfer(time, transfer2);
				}
			}
		}
	}

	public Set<Router> getPeers(Router router) {
		Set<Router> peers = new HashSet<Router>();
		Integer id = router.id();
		if ( ! active_node_ids.contains(id)){
			Set<Integer> neighbs = adjacency.getNext(id);
			if ( neighbs != null ){
				for ( Integer i : neighbs ){
					if ( canTransmit(i) )
						peers.add(_world.getRouterById(i) );
				}
			}
		}
		return peers;
	}
}
