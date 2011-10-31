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
import ditl.graphs.*;
import ditl.graphs.cli.GraphOptions;
import ditl.transfers.*;

public class InfectionReport extends Report 
	implements PresenceTrace.Handler, BufferTrace.Handler, MessageTrace.Handler {

	private Integer msg_id;
	private int infected = 0;
	private int present = 0;
	private long shift_time;
	private Set<Integer> infected_ids = new HashSet<Integer>();
	private boolean do_update = false;
	
	public InfectionReport(OutputStream out, Integer msgId, long shiftTime) throws IOException {
		super(out);
		msg_id = msgId;
		shift_time = shiftTime;
	}

	public Listener<BufferEvent> bufferEventListener() {
		return new Listener<BufferEvent>(){
			@Override
			public void handle(long time, Collection<BufferEvent> events)
					throws IOException {
				for ( BufferEvent event : events ){
					if ( event.type() == BufferEvent.ADD ){
						if ( event.msgId().equals(msg_id) && ! event.id().equals(PntScenario.root_id)){
							infected += 1;
							infected_ids.add(event.id());
						}
					}
				}
				update(time);
			}
		};
	}

	@Override
	public Listener<Buffer> bufferListener() {
		return null;
	}

	@Override
	public Listener<PresenceEvent> presenceEventListener() {
		return new Listener<PresenceEvent>(){
			@Override
			public void handle(long time, Collection<PresenceEvent> events) throws IOException {
				for ( PresenceEvent pev : events ){
					if ( pev.isIn() ){
						present += 1;
						if ( infected_ids.contains(pev.id()))
							infected +=1;
					} else {
						present -= 1;
						if ( infected_ids.contains(pev.id()))
							infected -=1;
					}
					update(time);
				}
			}
		};
	}

	@Override
	public Listener<Presence> presenceListener() {
		return new Listener<Presence>(){
			@Override
			public void handle(long time, Collection<Presence> events) throws IOException {
				present = events.size();
				update(time);
			}
		};
	}
	
	private void update(long time) throws IOException {
		if ( do_update ){
			double r = (double)infected/(double)present;
			append((time-shift_time)+" "+r);
		}
	}

	
	public static void main(String args[]) throws IOException{
		App app = new ExportApp(){

			GraphOptions graph_options = new GraphOptions(GraphOptions.PRESENCE);
			Long min_time, max_time;
			long shift_time;
			Integer msg_id;
			String shiftOption = "time-shift";
			
			@Override
			protected void initOptions() {
				super.initOptions();
				graph_options.setOptions(options);
				options.addOption(null, shiftOption, true, "Shift starting time by <arg>");
			}
			
			@Override
			protected void run() throws IOException, NoSuchTraceException,
					AlreadyExistsException, LoadTraceException {
				PresenceTrace presence = (PresenceTrace)_store.getTrace(graph_options.get(GraphOptions.PRESENCE));
				BufferTrace buffers = (BufferTrace)_store.getTrace("buffers");
				MessageTrace messages = (MessageTrace)_store.getTrace("messages");
				min_time = presence.minTime();
				max_time = presence.maxTime();
				shift_time *= presence.ticsPerSecond();
				
				InfectionReport r = new InfectionReport(System.out, msg_id, shift_time);
				
				Bus<Presence> presenceBus = new Bus<Presence>();
				presenceBus.addListener(r.presenceListener());
				Bus<PresenceEvent> presenceEventBus = new Bus<PresenceEvent>();
				presenceEventBus.addListener(r.presenceEventListener());
				StatefulReader<PresenceEvent,Presence> presenceReader = presence.getReader();
				presenceReader.setBus(presenceEventBus);
				presenceReader.setStateBus(presenceBus);
				
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
				runner.addGenerator(presenceReader);
				runner.addGenerator(messageReader);
				
				runner.run();
				
				r.finish();
				presenceReader.close();
				bufferReader.close();
			}
			
			@Override
			protected String getUsageString(){
				return "[OPTIONS] STORE MSGID";
			}
			
			@Override
			protected void parseArgs(CommandLine cli, String[] args)
					throws ParseException, ArrayIndexOutOfBoundsException,
					HelpException {
				super.parseArgs(cli, args);
				graph_options.parse(cli);
				msg_id = Integer.parseInt(args[1]);
				shift_time = Long.parseLong(cli.getOptionValue(shiftOption,"0"));
			}
			
		};
		app.ready("", args);
		app.exec();
	}

	@Override
	public Listener<MessageEvent> messageEventListener() {
		return new Listener<MessageEvent>(){
			@Override
			public void handle(long time, Collection<MessageEvent> events) {
				for ( MessageEvent mev : events ){
					if ( mev.msgId().equals(msg_id) ){
						if ( mev.isNew() ){
							do_update = true;
						} else {
							do_update = false;
						}
					}
				}
			}
		};
	}

	@Override
	public Listener<Message> messageListener() {
		return null;
	}
}
