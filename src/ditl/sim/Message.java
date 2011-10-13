package ditl.sim;

public abstract class Message {
	
	private static int next_id = 0;
	
	private Integer msg_id;
	protected Router _from;
	private long _bytes; // bytes
	private long creation_time;
	private long expiration_time;
	
	public Message( Router from, long bytes, long creationTime, long expirationTime ){
		msg_id = next_id++;
		_bytes = bytes;
		creation_time = creationTime;
		expiration_time = expirationTime;
		_from = from;
	}
	
	public Router from(){
		return _from;
	}
	
	public abstract boolean isDestinedTo(Router to);
	
	public long creationTime(){
		return creation_time;
	}
	
	public long expirationTime(){
		return expiration_time;
	}
	
	public Integer msgId(){
		return msg_id;
	}
	
	public long bytes(){
		return _bytes;
	}
	
	@Override
	public String toString(){
		return String.valueOf(msg_id);
	}
	
	@Override
	public int hashCode(){
		return msg_id;
	}
	
	@Override
	public boolean equals(Object o){
		Message m = (Message)o;
		return (m.msg_id == msg_id);
	}
	
	public MessageEvent getNewEvent(){
		return new MessageEvent(this, MessageEvent.NEW);
	}
	
	public MessageEvent getExpireEvent(){
		return new MessageEvent(this, MessageEvent.EXPIRE);
	}
}
