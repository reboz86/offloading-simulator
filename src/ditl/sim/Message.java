package ditl.sim;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class Message {
	
	private static int next_id = 0;
	
	private Integer msg_id;
	protected Router _from;
	private long _bytes; // bytes
	private long creation_time;
	private long expiration_time;
	private Set<Router> recipients;
	
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
	
	public void setRecipients(Set<Router> routers ){
	 this.recipients=routers;

	}

	public void addRecipient(Router router ){
		System.out.println("Added router: "+router._id);
		this.recipients.add(router);

	}

	public Set<Router> getRecipients(){
		return recipients;

	}

	public Set<Integer> getRecipientsId(){
		if(!recipients.isEmpty()){
			
			Iterator<Router> recIter=recipients.iterator();
			
			Set<Integer> routersId= new HashSet<Integer>();
			while(recIter.hasNext()){
				routersId.add(recIter.next().id());
				
			}
			return routersId;
		}
		return null;
		
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
		return new MessageEvent(this, MessageEvent.NEW, bytes());
	}
	
	public MessageEvent getExpireEvent(){
		return new MessageEvent(this, MessageEvent.EXPIRE);
	}
}
