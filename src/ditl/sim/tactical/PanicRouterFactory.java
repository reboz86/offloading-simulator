package ditl.sim.tactical;

import java.io.IOException;
import java.util.*;

import ditl.Bus;
import ditl.graphs.*;
import ditl.sim.*;

public class PanicRouterFactory extends TacticalRouterFactory {
	
	protected PanicBus panic_bus;
	protected long panic_time;

	public PanicRouterFactory() {};
	
	public PanicRouterFactory(Set<Group> groups,
			Bus<BufferEvent> bufferBus, AdHocRadio uhf, AdHocRadio vhfData,
			AdHocRadio vhfControl, int bufferSize, long guardTime,
			PanicBus panicBus, long panicTime) throws IOException {
		
		panic_bus = panicBus;
		panic_time = panicTime;
		init(groups, bufferBus, uhf, vhfData, vhfControl, bufferSize, guardTime);
	}
	
	@Override
	protected Leader newLeader(Integer id, Integer gid){
		return new PanicLeader(_uhf, vhf_data, vhf_control, id, gid, 
				buffer_size, guard_time, buffer_bus, Collections.unmodifiableMap(leaders), 
				panic_bus, panic_time);
	}

}
