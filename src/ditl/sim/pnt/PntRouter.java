package ditl.sim.pnt;

import java.io.IOException;
import java.util.*;

import ditl.Bus;
import ditl.sim.*;

public class PntRouter extends AdHocRouter {

	protected PervasiveInfraRadio infra_radio;
	protected LinkedList<ControlMessage> upload_buffer = new LinkedList<ControlMessage>();
	private long ackSize;
	
	public PntRouter(AdHocRadio adhocRadio, PervasiveInfraRadio infraRadio, 
			Integer id, int bufferSize, Bus<BufferEvent> bus,long ackSize, String strategy) {
		super(adhocRadio, id, bufferSize, bus,strategy);
		infra_radio = infraRadio;
		this.ackSize=ackSize;
		
	}
	
	@Override
	protected void receiveMessage(long time, Message msg, Radio radio) throws IOException {
		super.receiveMessage(time, msg, radio);
		if ( radio == adhoc_radio ){ // prepare ack
			queue(time, new AckMessage(this, infra_radio.root(), time, msg.msgId(),ackSize));
			trySendControlImmediately(time);
		} else { // try to send over ad hoc radio
			tryRelayImmediately(time);
		}
	}
	
	protected void queue(long time, ControlMessage msg){
		upload_buffer.addLast(msg);
	}
	
	@Override
	protected void sentMessage(long time, Message msg, Router to, Radio radio) throws IOException{
		if ( radio == infra_radio ){
			upload_buffer.remove(msg); // not very efficient
		} else {
			super.sentMessage(time, msg, to, radio);
		}
	}
	
	@Override
	protected TransferOpportunity getBestTransferTo(long time, Radio radio, Router dest){
		if ( radio == adhoc_radio ) {
			return super.getBestTransferTo(time, radio, dest);
		} else {
			return getBestControlTransfer(time);
		}
	}
	
	protected TransferOpportunity getBestControlTransfer(long time){
		if ( ! upload_buffer.isEmpty() ){
			if ( infra_radio.canPushUp(_id) )
				return new TransferOpportunity(this, infra_radio.root(), upload_buffer.pop());
		}
		return null;
	}
	
	protected void trySendControlImmediately(long time){
		TransferOpportunity opp = getBestControlTransfer(time);
		if ( opp != null )
			infra_radio.startNewTransfer(time, opp);
	}
	
	protected void tryRelayImmediately(long time){
		TransferOpportunity opp = getBestTransferOpportunity(time, adhoc_radio);
		if ( opp != null )
			adhoc_radio.startNewTransfer(time, opp);
	}
	
	@Override
	public void expireMessage(long time, Message msg) throws IOException {
		super.expireMessage(time, msg);
		for ( Iterator<ControlMessage> i=upload_buffer.iterator(); i.hasNext(); ){
			ControlMessage ctl_msg = i.next();
			if ( ctl_msg instanceof AckMessage ){
				if ( ((AckMessage)ctl_msg).msgId().equals(msg.msgId()) )
					i.remove();
			}
		}
	}
}
