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

public class FloatingReport extends Report 
	implements PresenceTrace.Handler, BufferTrace.Handler {

	private Integer msg_id;
	private int succ = 0;
	private int fail = 0;
	private long ignore_time;
	private Set<Integer> infected = new HashSet<Integer>();
	private Map<Integer,Long> entry_times = new HashMap<Integer,Long>();
	
	public FloatingReport(OutputStream out, Integer msgId, long ignoreTime) throws IOException {
		super(out);
		msg_id = msgId;
		ignore_time = ignoreTime;
	}

	public Listener<BufferEvent> bufferEventListener() {
		return new Listener<BufferEvent>(){
			@Override
			public void handle(long time, Collection<BufferEvent> events)
					throws IOException {
				for ( BufferEvent event : events ){
					if ( event.type() == BufferEvent.ADD ){
						Integer msgId = event.msgId();
						if ( msgId.equals(msg_id) ){
							Integer id = event.id();
							Long t = entry_times.get(id);
							if ( t != null ){
								succ += 1;
								infected.add(id);
								append(time-t);
							}
						}
					}
				}
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
			public void handle(long time, Collection<PresenceEvent> events) {
				for ( PresenceEvent pev : events ){
					Integer id = pev.id();
					if ( pev.isIn() ){
						entry_times.put(id, time);
					} else {
						if ( entry_times.containsKey(id) ){
							if ( infected.contains(id) ){
								infected.remove(id);
							} else if ( time-entry_times.get(id) > ignore_time) {
								fail += 1;
							}
							entry_times.remove(pev.id());
						}
					}
				}
			}
		};
	}

	@Override
	public Listener<Presence> presenceListener() {
		return null; // do nothing
	}
	
	@Override
	public void finish() throws IOException {
		double d = (double)succ / (double)(succ+fail);
		appendComment("delivery ratio: "+d);
		super.finish();
	}
	
	public static void main(String args[]) throws IOException{
		App app = new ExportApp(){

			GraphOptions graph_options = new GraphOptions(GraphOptions.PRESENCE);
			Long min_time, max_time;
			long ignore_time;
			Integer msg_id;
			String ignoreOption = "ignore";
			
			@Override
			protected void initOptions() {
				super.initOptions();
				graph_options.setOptions(options);
				options.addOption(null, minTimeOption, true, "Start analysis at time <arg>");
				options.addOption(null, maxTimeOption, true, "Stop analysis at time <arg>");
				options.addOption(null, ignoreOption, true, "Ignore nodes that remain less than time <arg>");
			}
			
			@Override
			protected void run() throws IOException, NoSuchTraceException,
					AlreadyExistsException, LoadTraceException {
				PresenceTrace presence = (PresenceTrace)_store.getTrace(graph_options.get(GraphOptions.PRESENCE));
				BufferTrace buffers = (BufferTrace)_store.getTrace("buffers");
				if ( min_time == null )
					min_time = presence.minTime();
				else
					min_time *= presence.ticsPerSecond();
				if ( max_time == null )
					max_time = presence.maxTime();
				else
					max_time *= presence.ticsPerSecond();
				ignore_time *= presence.ticsPerSecond();
				
				FloatingReport r = new FloatingReport(System.out, msg_id, ignore_time);
				
				Bus<PresenceEvent> presenceEventBus = new Bus<PresenceEvent>();
				presenceEventBus.addListener(r.presenceEventListener());
				Reader<PresenceEvent> presenceReader = presence.getReader();
				presenceReader.setBus(presenceEventBus);
				
				Bus<BufferEvent> bufferEventBus = new Bus<BufferEvent>();
				bufferEventBus.addListener(r.bufferEventListener());
				Reader<BufferEvent> bufferReader = buffers.getReader();
				bufferReader.setBus(bufferEventBus);
				
				Runner runner = new Runner(buffers.maxUpdateInterval(), min_time, max_time);
				runner.addGenerator(bufferReader);
				runner.addGenerator(presenceReader);
				
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
				if ( cli.hasOption(minTimeOption) )	
					min_time = Long.parseLong(cli.getOptionValue(minTimeOption));
				if ( cli.hasOption(maxTimeOption) )
					max_time = Long.parseLong(cli.getOptionValue(maxTimeOption));
				ignore_time = Long.parseLong(cli.getOptionValue(ignoreOption,"0"));
			}
			
		};
		app.ready("", args);
		app.exec();
	}
}
