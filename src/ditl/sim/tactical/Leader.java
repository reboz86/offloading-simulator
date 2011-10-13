package ditl.sim.tactical;

import java.io.IOException;
import java.util.*;

import ditl.Bus;
import ditl.sim.*;

public class Leader extends Node {
	
	protected AdHocRadio uhf;
	protected AdHocRadio vhf_data;
	protected AdHocRadio vhf_control;
	protected Set<AckMessage> pending_acks = new HashSet<AckMessage>();
	protected Map<Integer,Leader> _leaders = new HashMap<Integer,Leader>();
	protected Map<Message,Set<Leader>> not_yet_acknowledged = new HashMap<Message,Set<Leader>>();
	protected long guard_time;
	
	public Leader(AdHocRadio adhocRadio, AdHocRadio vhfData, AdHocRadio vhfControl, Integer id, 
			Integer gid, int bufferSize, long guardTime,
			Bus<BufferEvent> bus, Map<Integer,Leader> leaders) {
		super(adhocRadio, id, gid, bufferSize, bus);
		uhf = adhoc_radio;
		vhf_data = vhfData;
		vhf_control = vhfControl;
		_leaders = leaders;
		guard_time = guardTime;
	}
	
	protected boolean allowVHFTransfer(long time, Message msg, Router dest){
		return msg.expirationTime()-time > guard_time;
	}
	
	protected TransferOpportunity getBestVHFDataTransferTo(long time, Router dest){
		TransferOpportunity best = null;
		for ( Message msg : messages ){
			Set<Leader> not_acknowledged = not_yet_acknowledged.get(msg);
			if ( not_acknowledged != null ){
				if ( dest.acceptMessage(msg) && not_acknowledged.contains(dest) ){
					if ( allowVHFTransfer(time,msg,dest) ){
					TransferOpportunity opp = new TransferOpportunity(this, dest, msg);
					if ( best == null || opp.compareTo(best) < 0 )
						best = opp;
					}
				}
			}
		}
		return best;
	}
	
	protected TransferOpportunity getBestVHFControlTransferTo(Router dest){
		TransferOpportunity best = null;
		for ( AckMessage ack : pending_acks ){
			if ( ack.to().equals(dest) ){
				TransferOpportunity opp = new TransferOpportunity(this, dest, ack);
				if ( best == null || opp.compareTo(best) < 0 )
					best = opp;
			}
		}
		return best;
	}
	
	@Override
	protected TransferOpportunity getBestTransferTo(long time, Radio radio, Router dest){
		if ( radio == uhf ){
			return super.getBestTransferTo(time, radio, dest);
		} else if ( radio == vhf_control ){
			return getBestVHFControlTransferTo(dest);
		}
		return getBestVHFDataTransferTo(time, dest);
	}
	
	protected boolean amResponsibleFor(Message msg){
		if ( msg instanceof GroupMessage ){
			Integer gid = ((GroupMessage)msg).gid();
			if ( gid.equals(_gid)) // message originated in my group
				return true;
		}
		return false;
	}
	
	@Override
	protected void receiveMessage(long time, Message msg, Radio radio) throws IOException {
		if ( radio == vhf_control ){
			AckMessage ack = (AckMessage)msg;
			Message acked_msg = ack.message();
			if ( ! not_yet_acknowledged.containsKey(acked_msg) )
				takeResponsability(time,acked_msg);
			ackLeader(acked_msg, msg.from());
		} else if ( radio == vhf_data ){
			super.receiveMessage(time, msg, radio);
			trySendImmediately(time,uhf);
		} else {
			super.receiveMessage(time, msg, radio);
			if ( amResponsibleFor(msg) ){
				if ( ! not_yet_acknowledged.containsKey(msg) )
					takeResponsability(time,msg);
				trySendImmediately(time, vhf_data);
			} else {
				queueAck(time, (GroupMessage)msg);
				trySendImmediately(time, vhf_control);
			}
		}
	}
	
	@Override
	protected void sentMessage(long time, Message msg, Router to, Radio radio) throws IOException{
		if ( radio == vhf_data ){
			ackLeader(msg, to);
			super.sentMessage(time, msg, to, radio);
		} else if ( radio == vhf_control ){
			pending_acks.remove(msg);
		} else {
			super.sentMessage(time, msg, to, radio);
		}
		
	}
	
	protected void queueAck(long time, GroupMessage msg){
		Router to = _leaders.get(msg.gid());
		AckMessage ack = new AckMessage(this, to, msg, time);
		pending_acks.add(ack);
	}
	
	protected void takeResponsability(long time, Message msg){
		Set<Leader> other_leaders = new HashSet<Leader>(_leaders.values());
		other_leaders.remove(this);
		not_yet_acknowledged.put(msg, other_leaders);
	}
	
	protected void ackLeader(Message msg, Router router){
		not_yet_acknowledged.get(msg).remove(router);
	}
	
	@Override
	public void expireMessage(long time, Message msg) throws IOException {
		super.expireMessage(time, msg);
		not_yet_acknowledged.remove(msg);
		for ( Iterator<AckMessage> i=pending_acks.iterator(); i.hasNext(); ){
			AckMessage ack = i.next();
			if ( ack.message().equals(msg) )
				i.remove();
		}
	}

	protected void trySendImmediately(long time, Radio radio){
		TransferOpportunity opp = getBestTransferOpportunity(time, radio);
		if ( opp != null )
			radio.startNewTransfer(time, opp);
	}
	
	@Override
	public void newMessage(long time, Message msg) throws IOException {
		super.newMessage(time, msg);
		if ( amResponsibleFor(msg) )
			takeResponsability(time, msg);
	}
}
