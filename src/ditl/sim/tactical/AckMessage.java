package ditl.sim.tactical;

import ditl.sim.*;

public class AckMessage extends UnicastMessage {

	public final static long bytes = 1;
	private Message _msg;
	
	public AckMessage(Router from, Router to, Message msg, long creationTime){
		super(from, to, bytes, creationTime, creationTime);
		_msg = msg;
	}
	
	public Message message(){
		return _msg;
	}
}
