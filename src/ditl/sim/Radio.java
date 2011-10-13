package ditl.sim;

import ditl.*;


public abstract class Radio extends Bus<TransferEvent> implements Generator, Listener<TransferEvent>{
	public abstract void startNewTransfer(long time, TransferOpportunity opp);
}
