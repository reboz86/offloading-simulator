package ditl.sim.tactical;

import ditl.Bus;
import ditl.sim.*;

public class Node extends AdHocRouter {

	protected Integer _gid;
	
	public Node(AdHocRadio adhocRadio, Integer id, Integer gid, int bufferSize,
			Bus<BufferEvent> bus) {
		super(adhocRadio, id, bufferSize, bus);
		_gid = gid;
	}
	
	public Integer gid(){
		return _gid;
	}
}
