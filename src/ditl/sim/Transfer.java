package ditl.sim;

import java.io.IOException;

import ditl.graphs.Link;

public class Transfer {
	
	private static final boolean D=true;
	
	private double _bitrate;
	private long start_time;
	private Message _msg;
	private Router _from;
	private Router _to;
	private Radio _radio;
	
	public Transfer(Radio radio, Message msg, Router from, Router to, double bitrate, long startTime) {
		_bitrate = bitrate;
		start_time = startTime;
		_msg = msg;
		_from = from;
		_to = to;
		_radio = radio;
	}
	
	public Link link(){
		return new Link(_from.id(),_to.id());
	}
	
	public Radio radio(){
		return _radio;
	}
	
	public Router to(){
		return _to;
	}
	
	public Router from(){
		return _from;
	}
	
	public Message message(){
		return _msg;
	}
	
	private long bytesTransferred(long time){
		return (long)((time-start_time) * _bitrate);
	}
	
	public TransferEvent getStartEvent(){
		return new TransferEvent(this, TransferEvent.START);
	}
	
	public TransferEvent getAbortEvent(long time){
		long bytes = bytesTransferred(time);
		return new TransferEvent(this, TransferEvent.ABORT, bytes);
	}
	
	public TransferEvent getCompleteEvent(){
		return new TransferEvent(this, TransferEvent.COMPLETE, _msg.bytes());
	}
	
	public long getCompleteTime(){
		return start_time + (long)(_msg.bytes() / _bitrate);
	}
	
	public boolean isMsg(Message msg){
		return _msg.equals(msg);
	}
	
	public void start(long time) {
		_to.startTransfer(this);
	}
	
	public void complete(long time) throws IOException {
		_to.completeTransfer(time, this);
		_from.completeTransfer(time, this);
	}
	
	public void abort(long time) throws IOException {
		_to.abortTransfer(time, this);
		_radio.queue(time, getAbortEvent(time));
	}

}
