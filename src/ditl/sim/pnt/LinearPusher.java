package ditl.sim.pnt;

import ditl.sim.Message;

public class LinearPusher extends DefaultNumToPush {

	@Override
	public int numToPush(Message msg, double curTime, int nInfected, int nTotal) {
		if ( ! Double.isNaN(msg.expirationTime()) ){
			double p = (curTime-msg.creationTime())/(msg.expirationTime()-msg.creationTime());
			int target = (int)(nTotal * p)+1;
			if ( nInfected < target ){
				return (target - nInfected);
			}
		}
		return 0;
	}
}
