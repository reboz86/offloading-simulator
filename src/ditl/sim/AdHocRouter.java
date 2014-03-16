package ditl.sim;

import java.io.IOException;
import java.util.Set;

import ditl.Bus;

public class AdHocRouter extends Router {

	public static final String ACCEPT_STRATEGY = "ACCEPT_ALL";
	public static final String PRIORITY_STRATEGY = "PRIORITY";
	public static final String SELFISH_STRATEGY = "SELFISH";
	
	
	protected AdHocRadio adhoc_radio;
	private String strategy;
	
	public AdHocRouter(AdHocRadio adhocRadio, Integer id, int bufferSize, Bus<BufferEvent> bus, String strategy) {
		super(id, bufferSize, bus);
		adhoc_radio = adhocRadio;
		this.strategy=strategy;
	}

	@Override
	public TransferOpportunity getBestTransferOpportunity(long time, Radio radio){
		Set<Router> peers = ((AdHocRadio)radio).getPeers(this);
		TransferOpportunity best = null;
		TransferOpportunity opp;
		for ( Router peer : peers ){

			if(strategy.equalsIgnoreCase(PRIORITY_STRATEGY)){
				
				// PRIORITY management for recipients
				opp = getBestPrioritizedTransferTo(time, radio, peer);
				if ( best == null || (opp != null && opp.compareTo(best) < 0) )
					best = opp;
				if ( peer instanceof AdHocRouter ){
					opp = peer.getBestPrioritizedTransferTo(time, radio, this);
					if ( best == null || (opp != null && opp.compareTo(best) < 0) )
						best = opp;
				}
				
				if (best == null){
					// If no recipients available send to whatever neighbors
					
					opp = getBestTransferTo(time, radio, peer);
					if ( best == null || (opp != null && opp.compareTo(best) < 0) )
						best = opp;
					if ( peer instanceof AdHocRouter ){
						opp = peer.getBestTransferTo(time, radio, this);
						if ( best == null || (opp != null && opp.compareTo(best) < 0) )
							best = opp;
					}
					
				}

			}
			
			else{

				// No neighbors priority management
				opp = getBestTransferTo(time, radio, peer);
				if ( best == null || (opp != null && opp.compareTo(best) < 0) )
					best = opp;
				if ( peer instanceof AdHocRouter ){
					opp = peer.getBestTransferTo(time, radio, this);
					if ( best == null || (opp != null && opp.compareTo(best) < 0) )
						best = opp;
				}
			}
		}
		return best;
	}

	// The  transfer opportunity choose the best message to send NOT the best peer
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
	
	// chose the best message to send to peers that are recipients
	@Override
	protected TransferOpportunity getBestPrioritizedTransferTo(long time, Radio radio, Router dest){
		TransferOpportunity best = null;
		for ( Message msg : messages ){
			if (dest!=null){
				if ( msg.getRecipientsId()!=null && msg.getRecipientsId().contains(msg.msgId()) && dest.acceptMessage(msg) ){
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
		TransferOpportunity opp = getBestTransferOpportunity(time, adhoc_radio);
		if ( opp != null )
			adhoc_radio.startNewTransfer(time, opp);
	}
	
	@Override
	public boolean acceptMessage(Message msg){
		
		//System.out.println(this.id()+" interested to "+ msg.msgId()+ "? Strategy:"+strategy);
		// all contents are accepted and shared 
		if(strategy.equalsIgnoreCase(ACCEPT_STRATEGY) || strategy.equalsIgnoreCase(PRIORITY_STRATEGY))
			return ! messages.contains(msg); //&& hasRoomFor(msg);
		
//		// all contents are accepted and shared only if there is at least half of the space 
//		else if(strategy.equalsIgnoreCase(PRIORITY_STRATEGY))
//			if (msg.getRecipientsId().contains(this._id))
//				return ! messages.contains(msg) && hasRoomFor(msg);
//			else
//				return ! messages.contains(msg) && hasHalfRoomFor(msg);
		
		// Only interesting contents are accepted
		else if(strategy.equalsIgnoreCase(SELFISH_STRATEGY))
			return msg.isDestinedTo(this) && ! messages.contains(msg) && hasRoomFor(msg);
		
		return false;
	}

}
