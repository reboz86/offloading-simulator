package ditl.sim;

import java.io.IOException;
import java.util.*;

import ditl.*;
import ditl.graphs.*;

public class PervasiveInfraRadio extends Radio implements PresenceTrace.Handler {
	
	private Set<Integer> present_node_ids = new HashSet<Integer>();
	private Map<Integer,Transfer> active_uploads = new HashMap<Integer,Transfer>();
	private Map<Integer,Transfer> active_downloads = new HashMap<Integer,Transfer>();
	private BitrateGenerator down_bitrate_generator;
	private BitrateGenerator up_bitrate_generator;
	private World _world;
	private Integer root_id;
	
	public PervasiveInfraRadio(World world, BitrateGenerator downBitrateGenerator,
			BitrateGenerator upBitrateGenerator, Integer rootId){
		_world = world;
		up_bitrate_generator = upBitrateGenerator;
		down_bitrate_generator = downBitrateGenerator;
		root_id = rootId;
		addListener(this);
	}
	
	public boolean canPushUp(Integer id){
		return ! active_uploads.containsKey(id);
	}
	
	public boolean canPushDown(Integer id){
		return ! active_downloads.containsKey(id);
	}

	@Override
	public void startNewTransfer(long time, TransferOpportunity opp) {
		Transfer transfer = null;
		if ( opp.from().id().equals(root_id) ){ // down
			transfer = opp.toTransfer(this, time, down_bitrate_generator.getNext());
			active_downloads.put(opp.to().id(), transfer);
		} else { // up
			transfer = opp.toTransfer(this, time, up_bitrate_generator.getNext());
			active_uploads.put(opp.to().id(), transfer);
		}
		transfer.start(time);
		queue(time, transfer.getStartEvent());
		queue(transfer.getCompleteTime(), transfer.getCompleteEvent());
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
				stopTransfer(time, transfer);
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
	
	private void stopTransfer(long time, Transfer transfer){
		Integer from = transfer.from().id();
		Integer to = transfer.to().id();
		if ( from.equals(root_id) ){ // this was a downlink transfer
			active_downloads.remove(to);
			tryStartNewPushDown(time, to);
		} else { // uplink transfer 
			active_uploads.remove(from);
			if ( present_node_ids.contains(from) ) // node is still present
				tryStartNewPushUp(time, from);
		}
	}

	@Override
	public Listener<PresenceEvent> presenceEventListener() {
		return new Listener<PresenceEvent>(){
			@Override
			public void handle(long time, Collection<PresenceEvent> events) {
				for ( PresenceEvent pev : events ){
					Integer id = pev.id();
					if ( pev.isIn() ){
						present_node_ids.add(id);
						tryStartNewPushUp(time, id);
						tryStartNewPushDown(time, id);
					} else {
						present_node_ids.remove(id);
						Transfer act_trans;
						act_trans = active_downloads.get(id);
						if ( act_trans != null )
							queue(time, act_trans.getAbortEvent(time));
						act_trans = active_uploads.get(id);
						if ( act_trans != null )
							queue(time, act_trans.getAbortEvent(time));
					}
				}
			}
		};
	}

	@Override
	public Listener<Presence> presenceListener() {
		return new Listener<Presence>(){
			@Override
			public void handle(long time, Collection<Presence> events){
				for ( Presence p : events ) {
					present_node_ids.add(p.id());
					tryStartNewPushUp(time, p.id());
					tryStartNewPushDown(time, p.id());
				}
			}
		};
	}
	
	protected void tryStartNewPushUp(long time, Integer id){
		Router r = _world.getRouterById(id);
		Router root = _world.getRouterById(root_id);
		TransferOpportunity opp = r.getBestTransferTo(time, this, root);
		if ( opp != null )
			startNewTransfer(time, opp);
	}
	
	protected void tryStartNewPushDown(long time, Integer id){
		Router root = _world.getRouterById(root_id);
		Router r = _world.getRouterById(id);
		if ( r != null ){
			TransferOpportunity opp = root.getBestTransferTo(time, this, r);
			if ( opp != null )
				startNewTransfer(time, opp);
		}
	}
	
	public Router root(){
		return _world.getRouterById(root_id);
	}
	
	public Router getClient(Integer id){
		return _world.getRouterById(id);
	}
}
