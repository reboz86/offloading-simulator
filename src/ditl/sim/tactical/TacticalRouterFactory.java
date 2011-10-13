package ditl.sim.tactical;

import java.io.IOException;
import java.util.*;

import ditl.*;
import ditl.graphs.*;
import ditl.sim.*;

public class TacticalRouterFactory implements RouterFactory {

	protected int buffer_size;
	protected Bus<BufferEvent> buffer_bus;
	protected AdHocRadio _uhf;
	protected AdHocRadio vhf_data;
	protected AdHocRadio vhf_control;
	protected Map<Integer,Integer> group_map = new HashMap<Integer,Integer>();
	protected Map<Integer,Leader> leaders = new HashMap<Integer,Leader>();
	protected long guard_time;
	
	protected TacticalRouterFactory() {};
	
	public TacticalRouterFactory(Set<Group> groups, 
			Bus<BufferEvent> bufferBus,
			AdHocRadio uhf, AdHocRadio vhfData, AdHocRadio vhfControl, 
			int bufferSize, long guardTime ) throws IOException {
		
		init(groups,bufferBus,uhf,vhfData,vhfControl,bufferSize, guardTime);
	}
	
	protected void init(Set<Group> groups, 
			Bus<BufferEvent> bufferBus,
			AdHocRadio uhf, AdHocRadio vhfData, AdHocRadio vhfControl, 
			int bufferSize, long guardTime) throws IOException{
		for ( Group g : groups )
			for ( Integer id : g.members() )
				group_map.put(id, g.gid());
		buffer_bus = bufferBus;
		buffer_size = bufferSize;
		_uhf = uhf;
		vhf_data = vhfData;
		vhf_control = vhfControl;
		guard_time = guardTime;
		for ( Group g: groups ){
			Integer id = g.gid();
			leaders.put(id, newLeader(id,id));
		}
	}
	
	@Override
	public Router getNew(Integer id) {
		Integer gid = group_map.get(id);
		if ( id.equals(gid) ) // this is a leader
			return leaders.get(id);
		return new Node(_uhf, id, gid, buffer_size, buffer_bus);
	}
	
	protected Leader newLeader(Integer id, Integer gid){
		return new Leader(_uhf, vhf_data, vhf_control, id, gid, 
				buffer_size, guard_time, buffer_bus, Collections.unmodifiableMap(leaders));
	}
}
