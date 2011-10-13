package ditl.sim.tactical;

import java.util.*;

import ditl.Bus;
import ditl.graphs.*;
import ditl.sim.*;

public class CCLeader extends PanicLeader {

	protected GroupTrace.Updater cc_updater;
	
	public CCLeader(AdHocRadio adhocRadio, AdHocRadio vhfData,
			AdHocRadio vhfControl, Integer id, Integer gid, int bufferSize, long guardTime,
			Bus<BufferEvent> bus, Map<Integer, Leader> leaders,
			PanicBus panicBus, long panicTime, GroupTrace.Updater ccUpdater) {
		super(adhocRadio, vhfData, vhfControl, id, gid, bufferSize, guardTime, bus, leaders,
				panicBus, panicTime);
		cc_updater = ccUpdater;
	}
	
	@Override
	protected boolean allowVHFTransfer(long time, Message msg, Router dest){
		return (msg.expirationTime() - time > guard_time) && 
			( panic_messages.contains(msg) || ! inSameCCAs(dest) );
	}
	
	private boolean inSameCCAs(Router dest){
		for ( Group g : cc_updater.states() ){
			Set<Integer> members = g.members();
			if ( members.contains(_id) ){
				if ( members.contains(dest.id()) )
					return true;
				return false;
			}
		}
		return false;
	}

}
