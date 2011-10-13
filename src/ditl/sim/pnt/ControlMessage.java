package ditl.sim.pnt;

import ditl.sim.*;

public abstract class ControlMessage extends UnicastMessage {
	public final static long bytes = 256;
	
	public ControlMessage(Router from, Router to, long creationTime) {
		super(from, to, bytes, creationTime, creationTime);
	}

}
