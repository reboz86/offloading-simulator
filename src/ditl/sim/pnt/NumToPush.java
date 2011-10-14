package ditl.sim.pnt;

import ditl.sim.Message;

public interface NumToPush  {
	public int numToPush(Message msg, double curTime, int nInfected, int nTotal );
	public void expireMessage(Message msg);
}
