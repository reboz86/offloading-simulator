package ditl.sim.pnt;

import ditl.sim.*;

public abstract class ControlMessage extends UnicastMessage {
	
	public ControlMessage(Router from, Router to, long creationTime, long ackSize) {
		super(from, to, ackSize, creationTime, creationTime);
	}

}
