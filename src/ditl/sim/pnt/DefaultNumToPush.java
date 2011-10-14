package ditl.sim.pnt;

import ditl.sim.Message;

public class DefaultNumToPush implements NumToPush {
	
	@Override
	public int numToPush(Message msg, double curTime, int nInfected, int nTotal ){
		return 0;
	}
	
	@Override
	public void expireMessage(Message msg) {};
}
