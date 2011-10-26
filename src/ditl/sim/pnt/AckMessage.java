package ditl.sim.pnt;

import ditl.sim.*;

public class AckMessage extends ControlMessage {

	private final Integer acked_msg_id;
	
	public AckMessage(Router from, Router to, long creationTime, Integer msgId){
		super(from, to, creationTime);
		acked_msg_id = msgId;
	}
	
	public Integer ackMsgId(){
		return acked_msg_id;
	}
	
	@Override
	public String toString(){
		return _from+" ack "+acked_msg_id;
	}
}
