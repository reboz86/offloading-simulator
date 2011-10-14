package ditl.sim.pnt;

import java.util.Set;

import ditl.sim.Message;

public class SinglePusher extends DefaultNumToPush {

	private Set<Integer> _sent;
	private int _initial = 1;
	
	public SinglePusher() {}
	
	public SinglePusher(int initialNumCopies) {
		_initial = initialNumCopies;
	}
	
	@Override
	public int numToPush(Message msg, double curTime, int nInfected, int nTotal) {
		if ( nTotal - nInfected < 1)
			return 0;
		if ( ! _sent.contains(msg.msgId()) ){ // not yet sent this message
			_sent.add(msg.msgId());
			return _initial;
		}
		return 0;
	}

	@Override
	public void expireMessage(Message msg) {
		_sent.remove(msg.msgId());
	}
}
