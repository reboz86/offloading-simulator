package ditl.sim.tactical;

import java.io.IOException;
import java.util.*;

import ditl.*;
import ditl.graphs.*;
import ditl.sim.*;

public class ReachableLeader extends PanicLeader {
	
	protected AdjacencySet.Edges _adjacency;
	protected Map<Message,Set<Integer>> msg_dominating_sets = new HashMap<Message,Set<Integer>>();

	public ReachableLeader(AdHocRadio adhocRadio, AdHocRadio vhfData,
			AdHocRadio vhfControl, Integer id, Integer gid, int bufferSize, long guardTime,
			Bus<BufferEvent> bus, Map<Integer, Leader> leaders,
			PanicBus panicBus, long panicTime, AdjacencySet.Edges adjacency) {
		super(adhocRadio, vhfData, vhfControl, id, gid, bufferSize, guardTime, bus, leaders,
				panicBus, panicTime);
		_adjacency = adjacency;
	}
	
	@Override
	protected boolean allowVHFTransfer(long time, Message msg, Router dest){
		return (msg.expirationTime() - time > guard_time) && 
		( panic_messages.contains(msg) || inDominatingSet(msg,dest) );
	}
	
	protected boolean inDominatingSet(Message msg, Router dest){
		Set<Integer> dominating = msg_dominating_sets.get(msg);
		if ( dominating == null )
			return false;
		return dominating.contains(dest.id());
	}
	
	protected Set<Integer> getDominatingSet(){
		Set<Integer> dominating = new HashSet<Integer>(_leaders.keySet());
		Map<Integer,Set<Integer>> sets = new HashMap<Integer,Set<Integer>>();
		// copy over adjacency data
		for ( Integer node : _adjacency.vertices() )
			sets.put(node, new HashSet<Integer>(_adjacency.getNext(node)));
		// remove all nodes that I can directly reach
		Set<Integer> i_can_reach = sets.remove(_id);
		if( i_can_reach != null ){
			dominating.removeAll(i_can_reach);
			for ( Iterator<Integer> i=sets.keySet().iterator(); i.hasNext(); ){
				Integer o = i.next();
				Set<Integer> r = sets.get(o);
				r.removeAll(i_can_reach);
				r.remove(_id);
				if ( r.isEmpty() )
					i.remove();
			}
		}
		
		// complete dominating set using greedy algorithm
		while ( ! sets.isEmpty() ){
			Integer best = null;
			Set<Integer> reached = null;
			int m_size = 0;
			for ( Map.Entry<Integer, Set<Integer>> e : sets.entrySet() ){
				int s = e.getValue().size();
				if ( s > m_size ){
					m_size = s;
					best = e.getKey();
					reached = e.getValue();
				}
			}
			sets.remove(best);
			dominating.removeAll(reached);
			for ( Iterator<Integer> i=sets.keySet().iterator(); i.hasNext(); ){
				Integer o = i.next();
				Set<Integer> r = sets.get(o);
				r.removeAll(reached);
				r.remove(best);
				if ( r.isEmpty() )
					i.remove();
			}
		}
		return dominating;
	}
	
	@Override
	public void expireMessage(long time, Message msg) throws IOException {
		super.expireMessage(time, msg);
		msg_dominating_sets.remove(msg);
	}
	
	@Override
	protected void takeResponsability(long time, Message msg){
		super.takeResponsability(time, msg);
		msg_dominating_sets.put(msg, getDominatingSet());
	}


}
