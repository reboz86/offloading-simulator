package ditl.sim;

import com.sun.org.apache.xml.internal.serializer.ToStream;


public class TransferEvent {
	
	public final static int START = 0;
	public final static int COMPLETE = 1;
	public final static int ABORT = 2;
	
	int _type;
	long bytes_transferred;
	Transfer _transfer;
	
	public TransferEvent(Transfer transfer, int type){
		_transfer = transfer;
		_type = type;
	}
	
	public TransferEvent(Transfer transfer, int type, long bytesTransferred){
		_transfer = transfer;
		_type = type;
		bytes_transferred = bytesTransferred;
	}
	
	public long bytesTransferred(){
		return bytes_transferred;
	}
	
	public int type(){
		return _type;
	}
	
	public Transfer transfer(){
		return _transfer;
	}
	
	public String toString(){
		return new String("Type: "+_type);
		
	}
}
