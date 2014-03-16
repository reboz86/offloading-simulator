package ditl.sim.pnt;

import java.util.Set;

import ditl.sim.Router;

public class NeighborsMessage extends ControlMessage {

	private Set<Integer> _neighbors;
	
	public NeighborsMessage(Router from, Router to, long creationTime, Set<Integer> neighbors) {
		super(from, to, creationTime,256);
		_neighbors = neighbors;
	}

	public Set<Integer> neighbors(){
		return _neighbors;
	}
}
