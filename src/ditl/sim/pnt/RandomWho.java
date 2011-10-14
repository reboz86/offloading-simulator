package ditl.sim.pnt;

import java.util.Set;

import ditl.sim.*;

public class RandomWho extends DefaultWhoToPush {

	@Override
	public Integer whoToPush(Message msg, Set<Integer> infected, Set<Integer> sane){
		return RNG.randomFromSet(sane);
	}
}
