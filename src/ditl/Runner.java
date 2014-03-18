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

public class Runner {
	
	private static final boolean D = false;
	private long min_time = Trace.INFINITY;
	private long max_time = -Trace.INFINITY;
	private long incr_time;
	private long cur_time;
	
	private List<Incrementable> incrementors = new LinkedList<Incrementable>(); 
	private List<Generator> generators = new LinkedList<Generator>();
	private LinkedHashSet<Bus<?>> busses = new LinkedHashSet<Bus<?>>();
	
	
	public Runner(long incrTime, long minTime, long maxTime){
		incr_time = incrTime;
		min_time = minTime;
		max_time = maxTime;
		cur_time = min_time;
		
		
	}
	
	public void add(Incrementable incr){
		if(D) System.out.println("Runner.addIncrementable: "+ incr.toString());
		incrementors.add(incr);
	}
	
	public void addGenerator(Generator generator){
		if(D) System.out.println("Runner.addgenerator: "+ generator.toString());
		generators.add(generator);
		Collections.sort(generators, new Comparator<Generator>(){
			@Override
			public int compare(Generator g0, Generator g1) {
				if ( g0.priority() < g1.priority() ) return -1;
				if ( g0.priority() > g1.priority() ) return 1;
				return 0;
			}
		});
		updateBusses();
	}
	
	public void updateBusses(){
		if(D) System.out.println("Runner.updateBusses");
		busses.clear();
		for ( Generator generator : generators ){
			if(D) System.out.println("	Generator: "+ generator.toString());
			for ( Bus<?> bus : generator.busses() )
				if ( bus != null ){
					if(D) System.out.println("	Bus update: "+ bus.toString());
					busses.add(bus);
				}
		}
	}
	
	public void removeGenerator(Reader<?> iterator){
		generators.remove(iterator);
		updateBusses();
	}
	
	public void run() throws IOException {
		if(D) System.out.println("Runner.run:");
		seek(min_time);
		while ( cur_time < max_time ){
			incr();
		}
	}
	
	public void seek(long time) throws IOException {
		for ( Bus<?> bus : busses ){ // tell all busses to reset
			if(D) System.out.println("Runner.seek: bus reset"+bus.toString());
			bus.reset();
			
		}
		for ( Generator generator : generators ){
			if(D) System.out.println("Runner.seek:"+generator.toString());
			generator.seek(time);
		}
		cur_time = time; // important to flush the new states
		flush(true);
		for ( Incrementable incr : incrementors ){
			incr.seek(time);
		}
	}
	
	public void incr() throws IOException {
		//System.out.println("Runner.incr:"+cur_time);
		long dt = Math.min(incr_time, max_time-cur_time);
		cur_time += dt;
		for ( Generator generator : generators ){
			generator.incr(dt);
		}
		flush(false);
		for ( Incrementable incr : incrementors ){
			incr.incr(dt);
		}
	}
	
	private void flush(boolean is_seek) throws IOException {
		//System.out.println("Runner.flush");
		while ( true ){
			long next_bus_time = Long.MAX_VALUE; // should be greater than Trace.INFINITY which is a possible value for next_bus_time
			Bus<?> nextBus = null;
			for ( Bus<?> bus : busses ){
				if ( bus.hasNextEvent() ){
					long t = bus.nextEventTime();
					if ( t < next_bus_time ){
						next_bus_time= t;
						nextBus = bus;
					}
				}
			}
			if ( next_bus_time < cur_time )
				nextBus.signalNext();
			else if ( is_seek && next_bus_time == cur_time )
				nextBus.signalNext();
			else
				break;
		}
		
	}
	
	public long time(){ return cur_time; }
	public long minTime(){ return min_time; }
	public long maxTime(){ return max_time;	}
	public long incrTime(){ return incr_time; }
	public void setIncrTime(long time){ incr_time = time; }
	
}
