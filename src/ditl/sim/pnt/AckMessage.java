package ditl.sim.pnt;

import ditl.sim.*;

public class AckMessage extends ControlMessage {

	private final Integer msg_id;
	
	public AckMessage(Router from, Router to, long creationTime, Integer msgId){
		super(from, to, creationTime);
		msg_id = msgId;
	}
	
	public Integer ackMsgId(){
		return msg_id;
	}
	
	@Override
	public String toString(){
		return _from+" ack "+msg_id;
	}
}
