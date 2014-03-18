package ditl.sim;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ditl.Bus;

public class AdHocSprayRouter extends Router {

	private static final boolean D=false;
	protected AdHocRadio adhoc_radio;
	private int initialCopies;
	Map<Integer,Integer> copiesLeft=new HashMap<Integer,Integer>();
	
	
	
	public AdHocSprayRouter(AdHocRadio adhocRadio, Integer id, int bufferSize, int initialCopies,  Bus<BufferEvent> bus) {
		super(id, bufferSize, bus);
		//if(D) System.out.println("AdHocSprayRouter:"+ initialCopies+" copies");
		
		adhoc_radio = adhocRadio;
		this.initialCopies=initialCopies;
	}
	
	@Override
	public TransferOpportunity getBestTransferOpportunity(long time, Radio radio){
		Set<Router> peers = ((AdHocRadio)radio).getPeers(this);
		TransferOpportunity best = null;
		TransferOpportunity opp;
		for ( Router peer : peers ){
			opp = getBestTransferTo(time, radio, peer);
			if ( best == null || (opp != null && opp.compareTo(best) < 0) )
				best = opp;
			if ( peer instanceof AdHocRouter ){
				opp = peer.getBestTransferTo(time, radio, this);
				if ( best == null || (opp != null && opp.compareTo(best) < 0) )
					best = opp;
			}
		}
		return best;
	}

	@Override
	protected TransferOpportunity getBestTransferTo(long time, Radio radio, Router dest){
		TransferOpportunity best = null;
		for ( Message msg : messages ){
			if (dest!=null){
				if ( dest.acceptMessage(msg) ){
					TransferOpportunity opp = new TransferOpportunity(this, dest, msg);
					if ( best == null || opp.compareTo(best) < 0 )
						best = opp;

				}
			}
		}
		return best;
	}

	@Override
	public void newMessage(long time, Message msg) throws IOException {
		super.newMessage(time, msg);
		initializeSpray(msg);
		TransferOpportunity opp = getBestTransferOpportunity(time, adhoc_radio);
		if ( opp != null )
			
			adhoc_radio.startNewTransfer(time, opp);
	}

	protected void initializeSpray(Message msg){

		if(!copiesLeft.containsKey(msg.msgId())){
			copiesLeft.put(msg.msgId(), initialCopies);
			if(D) System.out.println(this._id+" initializeSpray msg:"+msg.msgId()+" copies:"+ copiesLeft.get(msg.msgId()));
		}
	}
	
	protected boolean sendIfPossible(Message msg){
		if(copiesLeft.containsKey(msg.msgId())){
			int number=copiesLeft.get(msg.msgId());
			if(number>0){
				copiesLeft.put(msg.msgId(), number-1);
				if(D) System.out.println(this._id+" remaining msg:"+copiesLeft.get(msg.msgId()));
				return true;
			}
		}
		return false;
	}

}
