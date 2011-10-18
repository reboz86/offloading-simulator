package ditl.sim.pnt;

import ditl.sim.Message;


public class LinearSegmentPusher extends DefaultNumToPush {

	double _offset;
	
	public LinearSegmentPusher(double offset){
		_offset = offset;
	}
	
	@Override
	public int numToPush(Message msg, double curTime, int nInfected, int nTotal) {
		if ( ! Double.isNaN(msg.expirationTime()) ){
			double p = (curTime-msg.creationTime())/(msg.expirationTime()-msg.creationTime());
			int target;
			if ( p <= 0.5 ){
				target = (int)( (0.5 + _offset )*nTotal*p*2)+1;
			} else {
				target = (int)( nTotal*(p*(1.0-2*_offset ) + 2*_offset) )+1;
			}
			if ( nInfected < target ){
				return (target - nInfected);
			}
		}
		return 0;
	}
}
