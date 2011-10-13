package ditl.sim;

import java.io.IOException;
import java.util.Collection;

import ditl.*;
import ditl.transfers.MessageEvent;
import ditl.transfers.Message;

public class MessageEventLogger implements Logger<ditl.sim.MessageEvent> {
	
	private StatefulWriter<MessageEvent,Message> message_writer;
	
	public MessageEventLogger(StatefulWriter<MessageEvent,Message> messageWriter){
		message_writer = messageWriter;
	}

	@Override
	public void handle(long time, Collection<ditl.sim.MessageEvent> events) throws IOException {
		for ( ditl.sim.MessageEvent event : events ){
			MessageEvent mev;
			Integer msgId = event.message().msgId();
			if ( event.isNew() )
				mev = new MessageEvent(msgId, MessageEvent.NEW);
			else
				mev = new MessageEvent(msgId, MessageEvent.EXPIRE);
			message_writer.append(time, mev);
		}
	}

	@Override
	public void close() throws IOException {
		message_writer.close();
	}
}
