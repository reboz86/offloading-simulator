package ditl.sim.tactical;

import java.io.IOException;
import java.util.*;

import ditl.*;
import ditl.graphs.*;
import ditl.sim.*;

public class CCRouterFactory extends PanicRouterFactory implements GroupTrace.Handler {
	
	protected GroupTrace.Updater cc_updater;

	public CCRouterFactory(Set<Group> groups,
			Bus<BufferEvent> bufferBus, AdHocRadio uhf, AdHocRadio vhfData,
			AdHocRadio vhfControl, int bufferSize, long guardTime,
			PanicBus panicBus, long panicTime) throws IOException {
		
		cc_updater = new GroupTrace.Updater();
		panic_bus = panicBus;
		panic_time = panicTime;
		init(groups, bufferBus, uhf, vhfData, vhfControl, bufferSize, guardTime);
	}
	
	@Override
	protected Leader newLeader(Integer id, Integer gid){
		return new CCLeader(_uhf, vhf_data, vhf_control, id, gid, 
				buffer_size, guard_time, buffer_bus, Collections.unmodifiableMap(leaders), 
				panic_bus, panic_time, cc_updater);
	}

	@Override
	public Listener<GroupEvent> groupEventListener() {
		return new Listener<GroupEvent>(){
			@Override
			public void handle(long time, Collection<GroupEvent> events) {
				for ( GroupEvent event : events )
					cc_updater.handleEvent(time, event);
			}
		};
	}

	@Override
	public Listener<Group> groupListener() {
		return new Listener<Group>(){
			@Override
			public void handle(long time, Collection<Group> groups) {
				cc_updater.setState(groups);
			}
		};
	}

}
