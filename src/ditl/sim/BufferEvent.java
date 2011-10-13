package ditl.sim;

public class BufferEvent {

	public final static boolean ADD = true;
	public final static boolean REMOVE = false;
	
	Router _router;
	Message _msg;
	boolean is_add;
	
	public BufferEvent(Router router, Message msg, boolean isAdd){
		_router = router;
		_msg = msg;
		is_add = isAdd;
	}
	
	public boolean isAdd(){
		return is_add;
	}
	
	public Router router(){
		return _router;
	}
	
	public Message message(){
		return _msg;
	}
}
