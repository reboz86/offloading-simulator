/*******************************************************************************
 * This file is part of DITL.                                                  *
 *                                                                             *
 * Copyright (C) 2011 John Whitbeck <john@whitbeck.fr>                         *
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
package ditl.sim.pnt;

import java.io.*;
import java.util.*;

import org.apache.commons.cli.*;

import ditl.*;
import ditl.Store.LoadTraceException;
import ditl.Store.NoSuchTraceException;
import ditl.WritableStore.AlreadyExistsException;
import ditl.Reader;
import ditl.cli.*;
import ditl.graphs.cli.GraphOptions;
import ditl.transfers.*;

public class InfectionReport extends Report 
	implements  BufferTrace.Handler,MessageTrace.Handler,  Listener<Object>, Generator {

	private int infected = 0;
	private int present = 0;
	private Set<Integer> infected_ids = new HashSet<Integer>();
	private Bus<Object> update_bus = new Bus<Object>();
	
	public InfectionReport(OutputStream out) throws IOException {
		super(out);
		System.out.println("InfectionReport");
		update_bus.addListener(this);
	}
	
	private void queueUpdate(long time){
		update_bus.queue(time, Collections.emptySet());
	}

	public Listener<BufferEvent> bufferEventListener() {
		return new Listener<BufferEvent>(){
			@Override
			public void handle(long time, Collection<BufferEvent> events)
					throws IOException {
				for ( BufferEvent event : events ){
					if ( event.type() == BufferEvent.ADD ){
						if ( ! event.id().equals(PntScenario.root_id)){
							infected += 1;
							infected_ids.add(event.id());
						}
					}
					
					if ( event.type() == BufferEvent.IN ){
						present += 1;
						if ( infected_ids.contains(event.id()))
							infected +=1;
					}
					if ( event.type() == BufferEvent.OUT ){
						if(present > 0)
							present -= 1;
						if ( infected_ids.contains(event.id()))
							infected -=1;
					}
					
				}
				queueUpdate(time);
			}
		};
	}
	
	@Override
	public Listener<MessageEvent> messageEventListener() {
		return new Listener<MessageEvent>(){

			@Override
			public void handle(long time, Collection<MessageEvent> events) throws IOException {
					for (MessageEvent mev: events){
						if ( mev.isNew()){
							// new message
							infected =0;
							infected_ids.clear();
							
						}else{
							//queueUpdate(time);
							
							
						}
					}
				}
			
		};
	}
	
	@Override
	public Listener<Message> messageListener() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Listener<Buffer> bufferListener() {
		return null;
	}

	
	public static void main(String args[]) throws IOException{
		App app = new ExportApp(){

			GraphOptions graph_options = new GraphOptions(GraphOptions.PRESENCE);
			Long min_time, max_time;
			
			@Override
			protected void initOptions() {
				super.initOptions();
				graph_options.setOptions(options);
			}
			
			@Override
			protected void run() throws IOException, NoSuchTraceException,
					AlreadyExistsException, LoadTraceException {
				
				System.out.println("run: " + _store.listTraces().toString());
				//PresenceTrace presence = (PresenceTrace)_store.getTrace(graph_options.get(GraphOptions.PRESENCE));
				BufferTrace buffers = (BufferTrace)_store.getTrace("buffers");
				MessageTrace messages = (MessageTrace)_store.getTrace("messages");
				min_time = buffers.minTime();
				max_time = buffers.maxTime();
				
				System.out.println("InfectionReport");
				InfectionReport r = new InfectionReport(_out);
				
				//Bus<Presence> presenceBus = new Bus<Presence>();
				//presenceBus.addListener(r.presenceListener());
				//Bus<PresenceEvent> presenceEventBus = new Bus<PresenceEvent>();
				//presenceEventBus.addListener(r.presenceEventListener());
				//StatefulReader<PresenceEvent,Presence> presenceReader = presence.getReader();
				//presenceReader.setBus(presenceEventBus);
				//presenceReader.setStateBus(presenceBus);
				
				Bus<BufferEvent> bufferEventBus = new Bus<BufferEvent>();
				bufferEventBus.addListener(r.bufferEventListener());
				Reader<BufferEvent> bufferReader = buffers.getReader();
				bufferReader.setBus(bufferEventBus);
				
				Bus<MessageEvent> messageEventBus = new Bus<MessageEvent>();
				messageEventBus.addListener(r.messageEventListener());
				Reader<MessageEvent> messageReader = messages.getReader();
				messageReader.setBus(messageEventBus);
				
				Runner runner = new Runner(buffers.maxUpdateInterval(), min_time, max_time);
				runner.addGenerator(bufferReader);
				//runner.addGenerator(presenceReader);
				runner.addGenerator(messageReader);
				runner.addGenerator(r);
				
				runner.run();
				
				r.finish();
				//presenceReader.close();
				bufferReader.close();
				messageReader.close();
			}
			
			@Override
			protected String getUsageString(){
				return "[OPTIONS] STORE";
			}
			
			@Override
			protected void parseArgs(CommandLine cli, String[] args)
					throws ParseException, ArrayIndexOutOfBoundsException,
					HelpException {
				super.parseArgs(cli, args);
				graph_options.parse(cli);
			}
			
		};
		app.ready("", args);
		app.exec();
	}

//	@Override
//	public Listener<MessageEvent> messageEventListener() {
//		return new Listener<MessageEvent>(){
//			@Override
//			public void handle(long time, Collection<MessageEvent> events) {
//				for ( MessageEvent mev : events ){
//					if ( mev.msgId().equals(msg_id) ){
//						if ( mev.isNew() ){
//							do_update = true;
//						} else {
//							do_update = false;
//						}
//					}
//				}
//			}
//		};
//	}

//	@Override
//	public Listener<Message> messageListener() {
//		return null;
//	}
	
	@Override
	public void handle(long time, Collection<Object> arg1) throws IOException {
			double r = (double)infected/(double)present;
			append(time+" "+r);
	}

	@Override
	public void incr(long arg0) throws IOException {}

	@Override
	public void seek(long arg0) throws IOException {}

	@Override
	public Bus<?>[] busses() {
		return new Bus<?>[]{update_bus};
	}

	@Override
	public int priority() {
		return Integer.MAX_VALUE;
	}

	
}
