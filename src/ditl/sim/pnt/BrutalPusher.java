package ditl.sim.pnt;

import ditl.sim.Message;

public class BrutalPusher extends DefaultNumToPush {

	// Always push to all nodes
	@Override
	public int numToPush(Message msg, double curTime, int nInfected, int nTotal) {
		return nTotal - nInfected;
	}
	
}
