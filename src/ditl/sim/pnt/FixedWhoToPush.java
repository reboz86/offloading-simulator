package ditl.sim.pnt;

import java.util.Set;
import ditl.sim.Message;

public class FixedWhoToPush extends DefaultWhoToPush {
	int node;
	
	public FixedWhoToPush(int node_id){
		node=node_id;
	}
	// Choose the fixed node only if it exist
	@Override
	public Integer whoToPush(Message msg, Set<Integer> infected, Set<Integer> sane){
		if (sane.contains(node))
			return node;
		return null;
	
	}
}


