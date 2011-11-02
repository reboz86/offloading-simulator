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
import ditl.cli.*;
import ditl.graphs.*;
import ditl.graphs.cli.GraphOptions;

public class NodesPerCCReport extends StateTimeReport 
	implements PresenceTrace.Handler, GroupTrace.Handler, Generator, Listener<Object> {

	private int n_nodes;
	private Map<Integer,Integer> group_sizes = new HashMap<Integer,Integer>();
	private Bus<Object> update_bus = new Bus<Object>();
	
	public NodesPerCCReport(OutputStream out) throws IOException {
		super(out);
		update_bus.addListener(this);
		appendComment("time | duration | n_nodes | n_ccs | n_singletons | avg_cc_size | score");
	}
	
	private void queueUpdate(long time){
		update_bus.queue(time, Collections.emptySet());
	}

	@Override
	public Listener<PresenceEvent> presenceEventListener() {
		return new Listener<PresenceEvent>(){
			@Override
			public void handle(long time, Collection<PresenceEvent> events) {
				for ( PresenceEvent pev : events ){
					if ( pev.isIn() ){
						n_nodes += 1;
					} else {
						n_nodes -= 1;
					}
				}
				queueUpdate(time);
			}
		};
	}

	@Override
	public Listener<Presence> presenceListener() {
		return new Listener<Presence>(){
			@Override
			public void handle(long time, Collection<Presence> events){
				n_nodes = events.size();
				queueUpdate(time);
			}
		};
	}

	@Override
	public void incr(long dt) throws IOException {}

	@Override
	public void seek(long time) throws IOException {}

	@Override
	public void handle(long time, Collection<Object> arg1) throws IOException {
		int n_in_cc = 0;
		for ( Integer n : group_sizes.values() ){
			n_in_cc += n;
		}
		int n_singletons = n_nodes - n_in_cc;
		int n_ccs = group_sizes.size() + n_singletons;
		double avg_cc_size = (double)n_nodes/(double)n_ccs;
		double score = Math.log(avg_cc_size) / Math.log(n_nodes);
		append(time, n_nodes+" "+group_sizes.size()+" "+n_singletons+" "+avg_cc_size+" "+score);
	}

	@Override
	public Bus<?>[] busses() {
		return new Bus<?>[]{update_bus};
	}

	@Override
	public int priority() {
		return Integer.MAX_VALUE;
	}

	@Override
	public Listener<GroupEvent> groupEventListener() {
		return new Listener<GroupEvent>(){
			@Override
			public void handle(long time, Collection<GroupEvent> events) {
				Integer cur;
				for ( GroupEvent gev : events ){
					switch ( gev.type() ){
					case GroupEvent.NEW: 
						group_sizes.put(gev.gid(), 0); 
						break;
					case GroupEvent.JOIN:
						cur = group_sizes.get(gev.gid());
						group_sizes.put(gev.gid(), cur+gev.members().size());
						break;
					case GroupEvent.LEAVE:
						cur = group_sizes.get(gev.gid());
						group_sizes.put(gev.gid(), cur-gev.members().size());
						break;
					case GroupEvent.DELETE:
						group_sizes.remove(gev.gid());
						break;
					}
				}
				queueUpdate(time);
			}
		};
	}

	@Override
	public Listener<Group> groupListener() {
		return new Listener<Group>(){
			@Override
			public void handle(long time, Collection<Group> events){
				for ( Group g : events ){
					group_sizes.put(g.gid(), g.size());
				}
				queueUpdate(time);
			}
		};
	}

	public static void main(String args[]) throws IOException{
		App app = new ExportApp(){
	
			GraphOptions graph_options = new GraphOptions(GraphOptions.PRESENCE, GraphOptions.GROUPS);
			
			@Override
			protected void initOptions() {
				super.initOptions();
				graph_options.setOptions(options);
			}
			
			@Override
			protected void run() throws IOException, NoSuchTraceException,
					AlreadyExistsException, LoadTraceException {
				PresenceTrace presence = (PresenceTrace)_store.getTrace(graph_options.get(GraphOptions.PRESENCE));
				GroupTrace groups = (GroupTrace)_store.getTrace(graph_options.get(GraphOptions.GROUPS));
				
				NodesPerCCReport r = new NodesPerCCReport(System.out);
				
				Bus<PresenceEvent> presenceEventBus = new Bus<PresenceEvent>();
				presenceEventBus.addListener(r.presenceEventListener());
				Bus<Presence> presenceBus = new Bus<Presence>();
				presenceBus.addListener(r.presenceListener());
				StatefulReader<PresenceEvent,Presence> presenceReader = presence.getReader();
				presenceReader.setBus(presenceEventBus);
				presenceReader.setStateBus(presenceBus);
				
				Bus<GroupEvent> groupEventBus = new Bus<GroupEvent>();
				groupEventBus.addListener(r.groupEventListener());
				Bus<Group> groupBus = new Bus<Group>();
				groupBus.addListener(r.groupListener());
				StatefulReader<GroupEvent,Group> groupReader = groups.getReader();
				groupReader.setBus(groupEventBus);
				groupReader.setStateBus(groupBus);
				
				
				Runner runner = new Runner(groups.maxUpdateInterval(), presence.minTime(), presence.maxTime());
				runner.addGenerator(groupReader);
				runner.addGenerator(presenceReader);
				runner.addGenerator(r);
				
				runner.run();
				
				r.finish(presence.maxTime());
				presenceReader.close();
				groupReader.close();
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
			}
			
		};
		app.ready("", args);
		app.exec();
	}
}
