package ditl.sim;

import java.io.IOException;
import java.util.Collection;

import ditl.*;
import ditl.transfers.TransferEvent;
import ditl.transfers.Transfer;

public class TransferEventLogger implements Logger<ditl.sim.TransferEvent> {
	
	private StatefulWriter<TransferEvent,Transfer> transfer_writer;
	
	public TransferEventLogger(StatefulWriter<TransferEvent,Transfer> transferWriter){
		transfer_writer = transferWriter;
	}

	@Override
	public void handle(long time, Collection<ditl.sim.TransferEvent> events) throws IOException {
		for ( ditl.sim.TransferEvent event : events ){
			TransferEvent tev;
			ditl.sim.Transfer transfer = event.transfer();
			Integer from = transfer.from().id();
			Integer to = transfer.to().id();
			Integer msgId = transfer.message().msgId();
			switch ( event.type() ){
			case ditl.sim.TransferEvent.START:
				tev = new TransferEvent(msgId, from, to, TransferEvent.START);
				break;
			case ditl.sim.TransferEvent.ABORT:
				tev = new TransferEvent(msgId, from, to, TransferEvent.ABORT, event.bytesTransferred());
				break;
			default:
				tev = new TransferEvent(msgId, from, to, TransferEvent.COMPLETE, event.bytesTransferred());
				break;
			}
			transfer_writer.append(time, tev);
		}
	}
	
	@Override
	public void close() throws IOException {
		transfer_writer.close();
	}

}
