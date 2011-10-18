package ditl.sim.pnt;

import java.util.*;

import ditl.Listener;
import ditl.graphs.*;
import ditl.sim.Message;

public class DominatingSetPusher extends DefaultNumToPush implements WhoToPush, GroupTrace.Handler {

	protected Integer cur_msg_id = -1;
	protected GroupTrace.Updater group_updater = new GroupTrace.Updater();
	protected Set<Integer> ds_nodes = null;
	
	@Override
	public Integer whoToPush(Message msg, Set<Integer> infected, Set<Integer> sane) {
		if ( ds_nodes == null )
			return null;
		Integer who = null;
		for ( Integer i : ds_nodes ){
			if ( sane.contains(i) ){
				who = i;
				break;
			}
		}
		if ( who == null )
			return null;
		ds_nodes.remove(who);
		return who;
	}

	@Override
	public int numToPush(Message msg, double curTime, int nInfected, int nTotal) {
		if ( msg.msgId() != cur_msg_id ){
			cur_msg_id = msg.msgId();
			ds_nodes = dominatingSet();
		}
		if ( ds_nodes == null )
			return 0;
		return ds_nodes.size();
	}

	@Override
	public Listener<GroupEvent> groupEventListener() {
		return new Listener<GroupEvent>(){
			@Override
			public void handle(long time, Collection<GroupEvent> events) {
				for ( GroupEvent gev : events )
					group_updater.handleEvent(time, gev);
			}
		};
	}

	@Override
	public Listener<Group> groupListener() {
		return new Listener<Group>(){
			@Override
			public void handle(long time, Collection<Group> events) {
				group_updater.setState(events);
			}
		};
	}
	
	public Set<Integer> dominatingSet(){
		for ( Group g : group_updater.states() ){
			return new HashSet<Integer>(g.members());
		}
		return null;
	}

}
