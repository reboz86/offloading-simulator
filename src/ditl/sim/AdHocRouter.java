package ditl.sim;

import java.io.IOException;
import java.util.Set;

import ditl.Bus;

public class AdHocRouter extends Router {

	protected AdHocRadio adhoc_radio;
	
	public AdHocRouter(AdHocRadio adhocRadio, Integer id, int bufferSize, Bus<BufferEvent> bus) {
		super(id, bufferSize, bus);
		adhoc_radio = adhocRadio;
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
				opp = ((AdHocRouter)peer).getBestTransferTo(time, radio, this);
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
			if ( dest.acceptMessage(msg) ){
				TransferOpportunity opp = new TransferOpportunity(this, dest, msg);
				if ( best == null || opp.compareTo(best) < 0 )
					best = opp;
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

}
