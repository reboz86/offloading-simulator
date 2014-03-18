/*******************************************************************************
 * This file is part of DITL.                                                  *
 *                                                                             *
 * Copyright (C) 2011-2012 John Whitbeck <john@whitbeck.fr>                    *
 *                                                                             *
 * DITL is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU General Public License as published by        *
 * the Free Software Foundation, either version 3 of the License, or           *
 * (at your option) any later version.                                         *
 *                                                                             *
 * DITL is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of              *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the               *
 * GNU General Public License for more details.                                *
 *                                                                             *
 * You should have received a copy of the GNU General Public License           *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.       *
 *******************************************************************************/
package ditl.transfers;



import java.io.IOException;
import java.util.*;

import ditl.*;

public class BufferTrace extends StatefulTrace<BufferEvent, Buffer> {
	
	public final static boolean D=false;
	
	public final static String type = "buffers";
	public final static String defaultName = "buffers";
	
	public final static class Updater implements StateUpdater<BufferEvent,Buffer>{

		private Map<Integer,Buffer> buffer_map = new HashMap<Integer,Buffer>();
		private Set<Buffer> buffers = new HashSet<Buffer>();
		
		/*// TRY and ERROR FILIPPO
		public Updater(){
			System.out.println("BufferTrace.Updater constructor called");
			Buffer b =  new Buffer(-42);
			List<Buffer> collection = new ArrayList<Buffer>();
			collection.add(b);
			setState(collection);
		}*/
		
		@Override
		public void handleEvent(long time, BufferEvent event) {
			if(D) System.out.println("BufferTrace.handleEvent:"+time+";"+event.toString());
			Buffer b;
			
			Integer id = event._id;
			switch ( event._type ){
			case BufferEvent.ADD:
				if(D) System.out.println("BufferTrace.update: ADD");
				if(D) System.out.println("id:"+id);
				if(D) System.out.println("msg_id:"+event.msg_id);
				
				b = buffer_map.get(id);
				if(b!=null){
					if(D) System.out.println("b:"+b.toString());
					b.msg_ids.add(event.msg_id);
				}
				break;
			case BufferEvent.REMOVE:
				b = buffer_map.get(id);
				b.msg_ids.remove(event.msg_id);
				break;
			case BufferEvent.IN:
				b = new Buffer(id);
				buffer_map.put(id, b);
				break;
			case BufferEvent.OUT:
				b = buffer_map.get(id);
				buffers.remove(b);
			//	buffer_map.remove(id);
				break;
			}
		}

		@Override
		public void setState(Collection<Buffer> state) {
			if(D) System.out.println("BufferTrace.setState");
			buffer_map.clear();
			buffers.clear();
			for ( Buffer b : state ){
				buffer_map.put(b._id, b);
				buffers.add(b);
				if(D) System.out.println(b._id);
			}
		}

		@Override
		public Set<Buffer> states() {
			return buffers;
		}
	}
	
	public interface Handler {
		Listener<BufferEvent> bufferEventListener();
		Listener<Buffer> bufferListener();
	}
	
	public BufferTrace(Store store, String name, PersistentMap info) throws IOException {
		super(store, name, info, new BufferEvent.Factory(), new Buffer.Factory(), 
				new StateUpdaterFactory<BufferEvent,Buffer>(){
					@Override
					public StateUpdater<BufferEvent, Buffer> getNew() {
						
						return new BufferTrace.Updater();
					}
		});
	}

}
