package ditl.sim.pnt;

import java.util.*;

import ditl.Listener;
import ditl.sim.*;

public class AdaptativeSinglePusher extends DefaultNumToPush implements Listener<MessageEvent> {

	private Set<Message> _sent;
	private int _initial = 1;
	private int _initial_nTotal = 0;
	private double _beta = Double.NaN;
	private double _beta_est = Double.NaN;
	private boolean _beta_lock = false;
	private int _beta_weight = 1;
	private int _max_beta_weight = 2;
	
	public AdaptativeSinglePusher() {}
	
	public AdaptativeSinglePusher(int initialNumCopies) {
		_initial = initialNumCopies;
	}
	
	@Override
	public int numToPush(Message msg, double curTime, int nInfected, int nTotal) {
		if ( nTotal - nInfected < 1)
			return 0;
		if ( ! _sent.contains(msg) ){ // not yet sent this message
			_sent.add(msg);
			if ( ! Double.isNaN(_beta) ){ // beta has been set
				double D = msg.expirationTime() - curTime;
				 _initial = (int)Math.ceil( (double)nTotal / ( 1.0 + Math.exp( _beta * D / 2.0) ) );
				 System.out.println(curTime+": beta is "+_beta+", injecting "+_initial+" copies.");
				 _beta_lock = false; // unlock beta
			}
			_initial_nTotal = nTotal;
			return _initial;
		} else if ( ! _beta_lock ) { // update our current estimate of beta
			double p = (curTime-msg.creationTime())/(msg.expirationTime()-msg.creationTime());
			double ir1 = (double)_initial / (double)_initial_nTotal;
			double ir2 = (double)nInfected / (double)nTotal;
			if ( ir2 > 0.5 || p > 0.75 ){ // passing half-way infection is a good time to estimate beta
				double dt = curTime - msg.creationTime();
				double beta = 1/dt * Math.log( (ir2 * ( 1 - ir1 )) / (ir1 * (1-ir2)) );
				if ( Double.isNaN(_beta_est) ){
					_beta_est = beta;
					_beta_weight = 1;
				} else {
					_beta_est = ( _beta_weight * _beta_est + beta ) / (_beta_weight + 1);
					_beta_weight += 1; // just do a simple average with previous beta
					_beta_weight = Math.min(_beta_weight, _max_beta_weight);
				}
				_beta = _beta_est;
				_beta_lock = true; // lock beta
			//} else if ( p > 0.75 ) { // set beta but do not update estimate
			//	double dt = curTime - msg.creationTime();
			//	double beta = 1/dt * Math.log( (ir2 * ( 1 - ir1 )) / (ir1 * (1-ir2)) );
			//	_beta = beta;
			//	_beta_lock = true; // lock beta
			}
		}
		return 0;
	}

	@Override
	public void handle(long time, Collection<MessageEvent> events) {
		for ( MessageEvent mev : events )
			if ( mev.isNew() )
				_sent.remove(mev.message());
	}
	
}
