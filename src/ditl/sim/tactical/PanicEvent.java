package ditl.sim.tactical;

import ditl.sim.Message;

public class PanicEvent {

	private Message _msg;
	
	public PanicEvent(Message msg){
		_msg = msg;
	}
	
	public Message message(){
		return _msg;
	}
}
