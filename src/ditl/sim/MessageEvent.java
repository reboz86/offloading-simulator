package ditl.sim;


public class MessageEvent {
	public final static boolean NEW = true;
	public final static boolean EXPIRE = false;
	
	Message _msg;
	boolean is_new;
	
	public MessageEvent(Message msg, boolean isNew){
		_msg = msg;
		is_new = isNew;
	}
	
	public boolean isNew(){
		return is_new;
	}
	
	public Message message(){
		return _msg;
	}
}
