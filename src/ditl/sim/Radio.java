package ditl.sim;

import java.util.Set;

import ditl.*;


public abstract class Radio extends Bus<TransferEvent> implements Generator, Listener<TransferEvent>{
	public abstract Set<Router> getPeers(Router router);
	public abstract void startNewTransfer(long time, TransferOpportunity opp);
}
