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

import java.io.IOException;
import java.util.*;

import org.apache.commons.cli.*;

import ditl.*;
import ditl.Store.LoadTraceException;
import ditl.Store.NoSuchTraceException;
import ditl.WritableStore.AlreadyExistsException;
import ditl.cli.ConvertApp;
import ditl.graphs.*;
import ditl.graphs.cli.GraphOptions;
import ditl.sim.RNG;


public class DominatingSetConverter extends Bus<Object> implements 
	GroupTrace.Handler, PresenceTrace.Handler, Generator, Converter, Listener<Object> {

	private long _delay;
	private long min_time;
	private boolean started = false;
	private static final Integer ds_gid = 0;
	
	private StatefulWriter<GroupEvent,Group> ds_writer;
	
	private GroupTrace _ccs;
	private PresenceTrace _presence;
	private GroupTrace _ds;
	
	private Set<Integer> present = new HashSet<Integer>();
	private Map<Integer,Long> arrivals = new HashMap<Integer,Long>();
	private Map<Integer,Long> departures = new HashMap<Integer,Long>();
	private Map<Integer, Group> groups = new HashMap<Integer,Group>();
	private Map<Integer,Set<Integer>> starting_groups = new HashMap<Integer,Set<Integer>>();
	private Map<Integer,Set<Integer>> node_groups = new HashMap<Integer,Set<Integer>>();
	private Map<Integer,Set<Integer>> current_groups = new HashMap<Integer,Set<Integer>>();
	
	public DominatingSetConverter(GroupTrace ds, PresenceTrace presence,
			GroupTrace ccs, long period, long minTime) {
		min_time = minTime;
		_ds = ds;
		_presence = presence;
		_ccs = ccs;
		_delay = period;
		addListener(this);
	}
	
	
	@Override
	public void incr(long time) throws IOException {}
	
	
	@Override
	public void seek(long time) throws IOException {
		queue(min_time, Collections.emptySet());
	}
	
	@Override
	public void convert() throws IOException {
		StatefulReader<GroupEvent,Group> ccs_reader = _ccs.getReader();
		StatefulReader<PresenceEvent,Presence> presence_reader = _presence.getReader();
		
		ds_writer = _ds.getWriter(_ccs.snapshotInterval());
		
		ds_writer.setProperty(Trace.ticsPerSecondKey, _ccs.ticsPerSecond());
		ds_writer.setProperty(Trace.minTimeKey, _ccs.minTime());
		ds_writer.setProperty(Trace.maxTimeKey, _ccs.maxTime());
		
		Bus<Group> groupBus = new Bus<Group>();
		Bus<GroupEvent> groupEventBus = new Bus<GroupEvent>();
		groupBus.addListener(groupListener());
		groupEventBus.addListener(groupEventListener());
		ccs_reader.setBus(groupEventBus);
		ccs_reader.setStateBus(groupBus);
		
		Bus<Presence> presenceBus = new Bus<Presence>();
		Bus<PresenceEvent> presenceEventBus = new Bus<PresenceEvent>();
		presenceBus.addListener(presenceListener());
		presenceEventBus.addListener(presenceEventListener());
		presence_reader.setBus(presenceEventBus);
		presence_reader.setStateBus(presenceBus);
		
		Runner runner = new Runner(_ccs.maxUpdateInterval(), _ccs.minTime(), _ccs.maxTime());
		runner.addGenerator(presence_reader);
		runner.addGenerator(ccs_reader);
		runner.addGenerator(this);
		runner.run();
		
		ds_writer.close();
		ccs_reader.close();
		presence_reader.close();
	}

	@Override
	public Bus<?>[] busses() {
		return new Bus<?>[]{ this };
	}

	@Override
	public int priority() {
		return Integer.MAX_VALUE; // this should come after all other events
	}
	
	
	private Set<Integer> curDSNodes(){
		for ( Group g : ds_writer.states() ){
			return g.members();
		}
		return null; // should never get here
	}
	
	@Override
	public void handle(long time, Collection<Object> events) throws IOException {
		if ( started ){
			// first handle previous time period
			long t = time - _delay;
			purgeBeforeTime(t, departures); // remove those that departed a time periods ago
			purgeBeforeTime(t, arrivals);
			Set<Integer> ds = new DSCalculator().calculateNewDS();
			if ( t == _ccs.minTime() ){ // this should be the initial state  
				ds_writer.setInitState(min_time, Collections.singleton(new Group(ds_gid, ds)));
			} else {
				if ( ds_writer.states().isEmpty() )
					ds_writer.append(t, new GroupEvent(ds_gid, GroupEvent.NEW));
				Set<Integer> prev_ds_nodes = curDSNodes();
				Set<Integer> joining = new HashSet<Integer>();
				for ( Integer i : ds )
					if ( ! prev_ds_nodes.contains(i) )					
						joining.add(i);
				Set<Integer> leaving = new HashSet<Integer>();
				for ( Integer i : prev_ds_nodes )
					if ( ! ds.contains(i) )					
						leaving.add(i);
						
				if ( ! leaving.isEmpty() )
					ds_writer.queue(t, new GroupEvent(ds_gid, GroupEvent.LEAVE, leaving));
										
				// handle new ds nodes that arrive or leave during the time period
				for ( Iterator<Integer> i=joining.iterator(); i.hasNext(); ){
					Integer k = i.next();
					Long arr_time = arrivals.get(k);
					Long dep_time = departures.get(k);
					if ( arr_time != null ){ // node was present at beginning of time period
						ds_writer.queue(arr_time, new GroupEvent(ds_gid, GroupEvent.JOIN, new Integer[]{k}));
						i.remove();
					}
					if ( dep_time != null ){ // node left during the time period
						ds_writer.queue(dep_time, new GroupEvent(ds_gid, GroupEvent.LEAVE, new Integer[]{k}));
					}
				}
				if ( ! joining.isEmpty() )
					ds_writer.queue(t, new GroupEvent(ds_gid, GroupEvent.JOIN, joining));
				
				ds_writer.flush();
			}
		
			// then clear state and prepare for new dominating set
			init();
			
		} else {
			started = true;
			init();
			if ( min_time > _ccs.minTime() ) // starting after min_time => empty initial state
				ds_writer.setInitState(_ccs.minTime(), Collections.<Group>emptySet());
		}
		
		queue(time+_delay, Collections.emptySet());
	}
	

	@Override
	public Listener<Presence> presenceListener() {
		return new Listener<Presence>(){
			@Override
			public void handle(long time, Collection<Presence> events) {
				for ( Presence p : events )
					if ( ! p.id().equals(PntScenario.root_id)){
						arrivals.put(p.id(),time);
						present.add(p.id());
					}
			}
		};
	}

	@Override
	public Listener<PresenceEvent> presenceEventListener() {
		return new Listener<PresenceEvent>(){
			@Override
			public void handle(long time, Collection<PresenceEvent> events) {
				for ( PresenceEvent pev : events ){
					Integer id = pev.id();
					if ( pev.isIn() ){
						present.add(id);
						arrivals.put(id,time);
					} else {
						present.remove(id);
						departures.put(id,time);
					}
				}
			}
		};
	}

	@Override
	public Listener<GroupEvent> groupEventListener() {
		return new Listener<GroupEvent>(){
			@Override
			public void handle(long time, Collection<GroupEvent> events) {
				Group g;
				Integer gid;
				for ( GroupEvent gev : events ){
					gid = gev.gid();
					switch ( gev.type() ){
					case GroupEvent.NEW:
						g = new Group(gid);
						groups.put(gid, g);
						break;
					case GroupEvent.DELETE:
						groups.remove(gid);
						break;
					case GroupEvent.LEAVE:
						g = groups.get(gid);
						g.handleEvent(gev);
						break;
					case GroupEvent.JOIN:
						g = groups.get(gid);
						Set<Integer> gids = getGroupIds(gev.members());
						if ( g.members().isEmpty() ){ // we are joining an new group
							if ( gids.isEmpty() ){ // several singletons uniting into a new cc
								Set<Integer> members = new HashSet<Integer>();
								for ( Integer m : gev.members() ){
									members.add(m);
									node_groups.put(m, new HashSet<Integer>(Collections.singleton(gid)));
								}
								starting_groups.put(gid, members);
								current_groups.put(gid, members);
								
							} else { // merge all ids
								for ( Integer k : gev.members() )
									markNode(k, gids);
							}
						} else { // merge all groups
							Set<Integer> gids2 = getGroupIds(g.members());
							for ( Integer k : g.members() ){
								markNode(k, gids);
							}
							for ( Integer k : gev.members() ){
								markNode(k, gids2);
							}
							
						}
						g.handleEvent(gev);
						break;
					}
				}
			}
		};
	}

	@Override
	public Listener<Group> groupListener() {
		return new Listener<Group>(){
			@Override
			public void handle(long time, Collection<Group> events) {
				for ( Group g : events )
					groups.put(g.gid(), g);
			}
		};
	}
	
	private Set<Integer> getGroupIds(Iterable<Integer> nids){
		Set<Integer> union = new HashSet<Integer>();
		for ( Integer nid : nids ){
			Set<Integer> gids = node_groups.get(nid);
			if ( gids != null )
				union.addAll(gids);
		}
		return union;
	}
	
	private void markNode(Integer id, Collection<Integer> gids){
		Set<Integer> cur_gids = node_groups.get(id);
		if ( cur_gids == null ){
			node_groups.put(id, new HashSet<Integer>(gids));
		} else {
			cur_gids.addAll(gids);
		}
		for ( Integer gid : gids )
			current_groups.get(gid).add(id);
	}
	
	private void purgeBeforeTime(long time, Map<Integer,Long> map){
		for ( Iterator<Map.Entry<Integer, Long>> i=map.entrySet().iterator(); i.hasNext(); ){
			Map.Entry<Integer, Long> e = i.next();
			if ( e.getValue() <= time ){
				i.remove();
			}
		}
	}
	
	private void init(){
		starting_groups.clear();
		current_groups.clear();
		node_groups.clear();
		for ( Group g : groups.values() ){
			starting_groups.put(g.gid(), new HashSet<Integer>(g.members()));
			current_groups.put(g.gid(), new HashSet<Integer>(g.members()));
			for ( Integer i : g.members() )
				node_groups.put(i, new HashSet<Integer>(Collections.singleton(g.gid())));
		}
	}
	
	
	class DSCalculator {
		TreeMap<Integer,Set<Integer>> degree_map = new TreeMap<Integer,Set<Integer>>(); 
		Map<Integer,Set<Integer>> remainders = new HashMap<Integer,Set<Integer>>();
		Set<Integer> covered = new HashSet<Integer>();
		Set<Integer> new_ds = new HashSet<Integer>();
		
		DSCalculator() {
			// first put all complete singletons
			for ( Integer id : present ){
				if ( ! node_groups.containsKey(id) ){ // this node was never marked as being part of a group
					new_ds.add(id);
					covered.add(id);
				}
			}
			// then prepare the remainder map
			for ( Map.Entry<Integer, Set<Integer>> e : current_groups.entrySet() ){
				Integer gid = e.getKey();
				Set<Integer> r_dest = new HashSet<Integer>(e.getValue());
				remainders.put(gid, r_dest);
				setGroupDegree(gid, r_dest.size());
			}
		}
		
		Set<Integer> calculateNewDS(){
			while ( ! allCovered() ){
				pick();
			}
			return new_ds;
		}
		
		boolean allCovered(){
			return covered.size() >= present.size(); // covered may be greater than the number of present nodes (e.g., edges in reachability traces)
		}
		
		void pick(Integer gid){
			Integer id = RNG.randomFromSet(starting_groups.get(gid));
			new_ds.add(id);
			Set<Integer> newly_covered = remainders.remove(gid);
			degree_map.clear();
			for ( Map.Entry<Integer, Set<Integer>> e : remainders.entrySet()){
				Integer grp_id = e.getKey();
				Set<Integer> dests = e.getValue();
				dests.removeAll(newly_covered);
				setGroupDegree(grp_id, dests.size());
			}
			covered.addAll(newly_covered);
		}
		
		
		void pick(){
			Integer gid = null;
			Iterator<Integer> i = greedyChoice().iterator();
			while ( i.hasNext() ){
				gid = i.next();
				break;
			}
			pick(gid);
		}
		
		Set<Integer> greedyChoice(){
			return degree_map.lastEntry().getValue();
		}
		
		void setGroupDegree(Integer node, Integer new_degree ){
			if ( ! degree_map.containsKey(new_degree) )
				degree_map.put(new_degree, new HashSet<Integer>());
			degree_map.get(new_degree).add(node);
		}
	}

	
	public static void main(String[] args) throws IOException{
		ConvertApp app = new ConvertApp(){

			GraphOptions graph_options = new GraphOptions(GraphOptions.GROUPS, GraphOptions.PRESENCE);
			Long min_time;
			long delay;
			String ds_name;
			final static String dsNameOption = "ds-name";
			
			@Override
			protected void parseArgs(CommandLine cli, String[] args)
					throws ParseException, ArrayIndexOutOfBoundsException,
					HelpException {
				super.parseArgs(cli, args);
				graph_options.parse(cli);
				delay = Long.parseLong(args[1]);
				ds_name = cli.getOptionValue(dsNameOption);
				if ( cli.hasOption(minTimeOption) )
					min_time = Long.parseLong(cli.getOptionValue(minTimeOption));
			}
			
			@Override
			protected void initOptions() {
				super.initOptions();
				graph_options.setOptions(options);
				options.addOption(null, minTimeOption, true, "Start first ds at time <arg>");
				options.addOption(null, dsNameOption, true, "Name of dominating set trace");
			}
			
			@Override
			protected void run() throws IOException, NoSuchTraceException,
					AlreadyExistsException, LoadTraceException {
				GroupTrace ccs = (GroupTrace)orig_store.getTrace(graph_options.get(GraphOptions.GROUPS));
				PresenceTrace presence = (PresenceTrace)orig_store.getTrace(graph_options.get(GraphOptions.PRESENCE));
				String name = (ds_name == null)? GroupTrace.defaultName : ds_name;
				GroupTrace ds = (GroupTrace)dest_store.newTrace(name, GroupTrace.type, force);
				delay *= ccs.ticsPerSecond();
				if ( min_time == null )
					min_time = presence.minTime();
				else
					min_time *= ccs.ticsPerSecond();
				new DominatingSetConverter(ds, presence, ccs, delay, min_time).convert();
			}
			
			@Override
			protected String getUsageString(){
				return "[OPTIONS] STORE PERIOD";
			}
		};
		app.ready("", args);
		app.exec();
	}
}
