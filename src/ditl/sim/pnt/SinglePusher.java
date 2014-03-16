package ditl.sim.pnt;

import java.util.*;

import ditl.sim.Message;

public class SinglePusher extends DefaultNumToPush {

	private Set<Integer> _sent = new HashSet<Integer>();
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
			//if( curTime>=4200000 && curTime<4500000) System.out.println("numToPush:"+curTime+" 1");
			return _initial;
		}
		//if( curTime>=4200000 && curTime<4500000) System.out.println("numToPush:"+curTime+" 0");
		return 0;
	}

	@Override
	public void expireMessage(Message msg) {
		_sent.remove(msg.msgId());
	}
	
	public int initCopies(){
		return _initial;
	}
	
}
