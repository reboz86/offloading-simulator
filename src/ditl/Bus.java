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
package ditl;

import java.util.*;
import java.io.IOException;

public class Bus <E> {
	private static final boolean D = false;
	private List<Listener<E>> listeners;
	private TreeMap<Long,List<E>> buffer;
	
	public Bus(){
		listeners = new LinkedList<Listener<E>>();
		buffer = new TreeMap<Long,List<E>>();
	}
	
	public void reset(){
		if (D) System.out.println("Bus.reset");
		buffer.clear();
		for ( Listener<E> listener : listeners )
			if ( listener instanceof StatefulListener<?> )
				((StatefulListener<?>)listener).reset();
	}
	
	public void addListener(Listener<E> listener){
		if ( listener != null ){
			listeners.add(listener);
			if (D) System.out.println("Bus.addListener:"+listener.toString());
		}
	}
	
	public void signal(long time, Collection<E> events) throws IOException {
		if (D) System.out.println("start Bus.signal:"+time);
		for ( Listener<E> listener : listeners ){
			if (D) System.out.println("	listener:"+listener.toString());
			listener.handle(time, events);
		}
		if (D) System.out.println("end Bus.signal");
	}
	
	public void queue(long time, Collection<E> events) {
		if (D) System.out.println("Bus.queue:"+time);
		if ( ! buffer.containsKey(time) )
			buffer.put(time, new LinkedList<E>());
		buffer.get(time).addAll(events);
	}
	
	public void queue(long time, E event) {
		if (D) System.out.println("Bus.queue:"+time+";"+event.toString());
		if ( ! buffer.containsKey(time) )
			buffer.put(time, new LinkedList<E>());
		buffer.get(time).add(event);
	}
	
	public void signalNext() throws IOException {
		if (D) System.out.println("Bus.signalNext");
		Map.Entry<Long, List<E>> e = buffer.pollFirstEntry();
		signal(e.getKey(), e.getValue());
	}
	
	public boolean removeFromQueueAfterTime (long time, Matcher<E> matcher){
		boolean changed = false;
		Iterator<Long> i = buffer.keySet().iterator();
		while ( i.hasNext() ){
			Long t = i.next();
			if ( t >= time ){
				List<E> events = buffer.get(t);
				Iterator<E> j = events.iterator();
				while ( j.hasNext() ){
					E event = j.next();
					if ( matcher.matches(event) ){
						j.remove();
						changed = true;
					}
				}
				if (events.isEmpty())
					i.remove();
			}
		}
		return changed;
	}
	
	public void flush() throws IOException {
		while ( ! buffer.isEmpty() )
			signalNext();
	}
	
	public void flush(long maxTime) throws IOException {
		while ( ! buffer.isEmpty() && buffer.firstKey() <= maxTime )
			signalNext();
	}
	
	public boolean hasNextEvent(){
		return ! buffer.isEmpty();
	}
	
	public long nextEventTime(){
		return buffer.firstKey();
	}
}
