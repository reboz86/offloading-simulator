package ditl.sim.pnt;

import java.util.Set;

import ditl.sim.Message;

public interface WhoToPush {
	public Integer whoToPush(Message msg, Set<Integer> infected, Set<Integer> sane);
}
