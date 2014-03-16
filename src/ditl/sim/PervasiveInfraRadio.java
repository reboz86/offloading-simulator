package ditl.sim;

import java.io.IOException;
import java.util.*;

import ditl.*;
import ditl.graphs.*;

public class PervasiveInfraRadio extends Radio implements PresenceTrace.Handler {
	
	private final static boolean D=false;
	
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

	//called by InfraRouter.tryPush()
	@Override
	public void startNewTransfer(long time, TransferOpportunity opp) {
		if (D && opp.message().msgId()<3) System.out.println("PervasiveInfraRadio.startNewTransfer:"+time+";"+opp.message().msgId()+";"+opp.from().id()+";"+opp.to().id());
		Transfer transfer = null;
		if ( opp.from().id().equals(root_id) ){ // down
			if (D && opp.message().msgId()<3) System.out.println("	Down :"+opp.from().id()+";"+opp.to().id());
			transfer = opp.toTransfer(this, time, down_bitrate_generator.getNext());
			active_downloads.put(opp.to().id(), transfer);
		} else { // up
			//if (D) System.out.println("	Up :"+opp.from().id()+";"+opp.to().id());
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
			//if (D && event.transfer().message().msgId()<1) System.out.println("	event:"+event.toString());
			final Transfer transfer = event.transfer();
			if ( event.type() == TransferEvent.ABORT ){
				//if (D ) System.out.println("PervasiveInfraRadio.handle: ABORT "+time+";"+event.transfer().message().msgId()+";"+event.transfer().to().id());
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
				if (D && event.transfer().message().msgId()<3) System.out.println("PervasiveInfraRadio.handle: COMPLETE "+time+";"+event.transfer().message().msgId()+";"+event.transfer().from().id()+";"+event.transfer().to().id());
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
					
					if (D && present_node_ids.isEmpty()) System.out.println("PervasiveInfraRadio.PresenceEventListener.handle:"+time+":"+pev.id()+":"+pev.isIn());
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
				if(D) System.out.println("PervasiveInfraRadio.PresenceListener.handle:"+time);
				for ( Presence p : events ) {
					
					if(D)System.out.println("event:"+p.toString());
					present_node_ids.add(p.id());
					tryStartNewPushUp(time, p.id());
					tryStartNewPushDown(time, p.id());
				}
			}
		};
	}
	
	protected void tryStartNewPushUp(long time, Integer id){
		//if (D) System.out.println("PervasiveInfraRadio.TryStartnewPushUp:"+time+";"+id);
		Router r = _world.getRouterById(id);
		Router root = _world.getRouterById(root_id);
		TransferOpportunity opp = r.getBestTransferTo(time, this, root);
		if ( opp != null )
			startNewTransfer(time, opp);
	}

	protected void tryStartNewPushDown(long time, Integer id){
		if (D && present_node_ids.size()<3) System.out.println("PervasiveInfraRadio.tryStartNewPushDown:"+time+";"+id);
		Router root = _world.getRouterById(root_id);
		Router r = _world.getRouterById(id);
		

		if ( r != null ){
			TransferOpportunity opp = root.getBestTransferTo(time, this, r);
			if ( opp != null ){
				if (D && present_node_ids.size()<2) System.out.println("	startNewTransfer:"+time+";"+opp.toString());
				startNewTransfer(time, opp);
			}
		}
	}
	
	public Router root(){
		return _world.getRouterById(root_id);
	}
	
	public Router getClient(Integer id){
		return _world.getRouterById(id);
	}
}
