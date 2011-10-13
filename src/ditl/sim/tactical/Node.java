package ditl.sim.tactical;

import ditl.Bus;
import ditl.sim.*;

public class Node extends AdHocRouter {

	protected Integer _gid;
	
	public Node(AdHocRadio adhocRadio, Integer id, Integer gid, int bufferSize,
			Bus<BufferEvent> bus) {
		super(adhocRadio, id, bufferSize, bus);
		_gid = gid;
	}
	
	public Integer gid(){
		return _gid;
	}
	
	/*@Override
	public TransferOpportunity getBestTransferOpportunity(long time, Radio radio){
		Set<Router> peers = radio.getPeers(this);
		TransferOpportunity best = null;
		boolean best_out = false;
		TransferOpportunity opp, rev_opp;
		for ( Router peer : peers ){
			if ( sameGrpAs(peer) ){
				if ( ! best_out ){
					opp = getBestTransferTo(time, radio, peer);
					if ( best == null || (opp != null && opp.compareTo(best) < 0) )
						best = opp;
					if ( peer instanceof AdHocRouter ){
						opp = ((Node)peer).getBestTransferTo(time, radio, this);
						if ( best == null || (opp != null && opp.compareTo(best) < 0) )
							best = opp;
					}
				}
			} else {
				opp = getBestTransferTo(time, radio, peer);
				if ( peer instanceof AdHocRouter ){
					rev_opp = ((Node)peer).getBestTransferTo(time, radio, this);
					if ( rev_opp != null && opp != null && opp.compareTo(rev_opp) > 0 )
						opp = rev_opp;
				}
				if ( opp != null ){
					if ( ! best_out ){
						best = opp;
						best_out = true;
					} else {
						if ( opp.compareTo(best) < 0 )
							best = opp;
					}
				}	
			}
		}
		return best;
	}
	
	protected boolean sameGrpAs(Router router){
		return ((Node)router)._gid.equals(_gid);
	}*/
}
