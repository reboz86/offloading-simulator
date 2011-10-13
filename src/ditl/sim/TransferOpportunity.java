package ditl.sim;

public class TransferOpportunity implements Comparable<TransferOpportunity> {
	
	protected Message _msg;
	protected Router _from;
	protected Router _to;
	
	public TransferOpportunity(Router from, Router to, Message msg){
		_from = from;
		_to = to;
		_msg = msg;
	}
	
	public Router from(){
		return _from;
	}
	
	public Router to(){
		return _to;
	}
	
	public Transfer toTransfer(Radio radio, long startTime, double bitrate){
		return new Transfer(radio, _msg, _from, _to, bitrate, startTime);
	}

	@Override
	public int compareTo(TransferOpportunity t) {
		if ( _msg instanceof UnicastMessage ){
			if ( t._msg instanceof UnicastMessage )
				return nearestExpirationTime(t._msg);
			else
				return 1;
		} else if ( _msg instanceof MulticastMessage ){
			if ( t._msg instanceof UnicastMessage )
				return -1;
			else if ( t._msg instanceof MulticastMessage )
				return nearestExpirationTime(t._msg);
			else
				return 1;
		} else { // Broadcast
			if ( t._msg instanceof BroadcastMessage )
				return nearestExpirationTime(t._msg);
			return -1;
		}
	}
	
	private int nearestExpirationTime(Message msg){
		Long t1 = _msg.expirationTime();
		Long t2 = msg.expirationTime();
		return t1.compareTo(t2);
	}
}
