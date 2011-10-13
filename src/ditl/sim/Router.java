package ditl.sim;

import java.io.IOException;
import java.util.*;

import ditl.Bus;

public class Router {
	
	protected Integer _id;
	protected int buffer_max_bytes;
	protected int in_transit_bytes;
	protected int buffer_bytes;
	protected Set<Message> messages = new HashSet<Message>();
	protected Bus<BufferEvent> _bus;
	protected Set<Transfer> incoming_transfers = new HashSet<Transfer>();
	
	public Router(Integer id, int bufferSize, Bus<BufferEvent> bus){
		_id = id;
		buffer_max_bytes = bufferSize;
		buffer_bytes = 0;
		in_transit_bytes = 0;
		_bus = bus;
	}
	
	public Integer id(){
		return _id;
	}
	
	public boolean acceptMessage(Message msg){
		return msg.isDestinedTo(this) && ! messages.contains(msg);
	}
	
	public void newMessage(long time, Message msg) throws IOException {
		pushMessage(time, msg);
	}
	
	protected void pushMessage(long time, Message msg) throws IOException {
		buffer_bytes += msg.bytes();
		messages.add(msg);
		_bus.signal(time, Collections.singleton(new BufferEvent(this, msg, BufferEvent.ADD)));
	}
	
	protected void removeMessage(long time, Message msg) throws IOException {
		messages.remove(msg);
		buffer_bytes -= msg.bytes();
		_bus.signal(time, Collections.singleton(new BufferEvent(this, msg, BufferEvent.REMOVE)));
	}
	
	public boolean hasRoomFor(Message msg){
		return ( buffer_max_bytes-buffer_bytes-in_transit_bytes >= msg.bytes() );
	}
	
	public void completeTransfer(long time, Transfer transfer) throws IOException {
		Message msg = transfer.message();
		if ( transfer.to().equals(this) ){
			incoming_transfers.remove(transfer);
			in_transit_bytes -= msg.bytes();
			receiveMessage(time, msg, transfer.radio());
			check_other_copy_in_transit(time, msg);
		} else { // from me
			sentMessage(time, msg, transfer.to(), transfer.radio());
		}
	}
	
	protected void receiveMessage(long time, Message msg, Radio radio) throws IOException {
		pushMessage(time,msg);
	}
	
	protected void sentMessage(long time, Message msg, Router to, Radio radio) throws IOException {
		if ( msg instanceof UnicastMessage )
			removeMessage(time, msg);
		else if ( msg instanceof MulticastMessage && ! msg.isDestinedTo(this) )
			removeMessage(time, msg);
	}
	
	private void check_other_copy_in_transit(long time, Message msg) throws IOException {
		for ( Iterator<Transfer> i = incoming_transfers.iterator(); i.hasNext(); ){
			Transfer transfer = i.next();
			if ( transfer.isMsg(msg) ){
				i.remove();
				transfer.abort(time);
			}
		}
	}

	public void abortTransfer(long time, Transfer transfer){
		Message msg = transfer.message();
		in_transit_bytes -= msg.bytes();
		incoming_transfers.remove(transfer);
	}
	
	public void startTransfer(Transfer transfer){
		in_transit_bytes += transfer.message().bytes();
		incoming_transfers.add(transfer);
	}
	
	public void expireMessage(long time, Message msg) throws IOException {
		check_other_copy_in_transit(time, msg);
		if ( messages.contains(msg) )
			removeMessage(time, msg);
	}
	
	public TransferOpportunity getBestTransferOpportunity(long time, Radio radio){
		return null; // this router is passive
	}

}
