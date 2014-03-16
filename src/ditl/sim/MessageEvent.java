package ditl.sim;


// Qui non dovrebbe esserci la factory perchè c'è una classe generatore (messagegenerator???)

public class MessageEvent {
	public final static boolean NEW = true;
	public final static boolean EXPIRE = false;
	
	Message _msg;
	boolean is_new;
	Long _size;
	
	public MessageEvent(Message msg, boolean isNew){
		_msg = msg;
		is_new = isNew;
		
	}
	
	public MessageEvent(Message msg, boolean isNew, Long size){
		_msg = msg;
		is_new = isNew;
		_size= size;
		
	}
	
	public boolean isNew(){
		return is_new;
	}
	
	public Message message(){
		return _msg;
	}
	
	public long size(){
		return _size;
	}
	
}
