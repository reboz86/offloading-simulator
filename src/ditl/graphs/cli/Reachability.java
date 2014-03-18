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
package ditl.graphs.cli;
import java.io.*;
import java.util.*;

import org.apache.commons.cli.*;

import ditl.Store.LoadTraceException;
import ditl.Store.NoSuchTraceException;
import ditl.WritableStore.AlreadyExistsException;
import ditl.cli.ConvertApp;
import ditl.graphs.*;



public class Reachability extends ConvertApp {
	
	final static String everyOption = "every";
	final static String noDeleteOption = "no-delete";
	final static String noPruneOption = "no-prune";
	final static String pruneReusedOption = "prune-reused";
	final static String noPruneLastOption = "no-prune-last";
	final static String minDelayOption = "min-delay";
	final static String verboseOption = "verbose";
	final static String prefixOption = "prefix";
	final static String timeFileOption = "times-file"; 
	
	private double u_tau;
	private double u_eta;
	private double u_delay;
	private long tau;
	private long delay;
	private long eta;
	private Long every;
	private GraphOptions graph_options = new GraphOptions(GraphOptions.LINKS, GraphOptions.GROUPS);
	private String timeFileName;
	private BufferedWriter time_writer;
	private String linksName;
	private String ccsName;
	private String prefix;
	private Long min_delay;
	private List<ReachabilityFamily> created_families = new LinkedList<ReachabilityFamily>();
	private Set<Long> families_to_keep = new HashSet<Long>();
	private boolean delete = true;
	private boolean prune = true;
	private boolean prune_last = true;
	private boolean prune_reused = false;
	private boolean verbose = false;
	private long tps;
	private long ref_time;
	
	public final static String PKG_NAME = "graphs";
	public final static String CMD_NAME = "reachability";
	public final static String CMD_ALIAS = "r";
	
	@Override
	protected String getUsageString(){
		return "[OPTIONS] STORE ETA TAU MAXDELAY";
	}

	@Override
	protected void parseArgs(CommandLine cli, String[] args) throws ParseException, ArrayIndexOutOfBoundsException, HelpException {
		super.parseArgs(cli, args);
		graph_options.parse(cli);
		u_eta = Double.parseDouble(args[1]);
		u_tau = Double.parseDouble(args[2]);
		u_delay = Double.parseDouble(args[3]);
		linksName = graph_options.get(GraphOptions.LINKS);
		ccsName = graph_options.get(GraphOptions.GROUPS);
		if ( cli.hasOption(everyOption) )
			every = Long.parseLong(cli.getOptionValue(everyOption));
		if ( cli.hasOption(minDelayOption) )
			min_delay = Long.parseLong(cli.getOptionValue(minDelayOption));
		delete = ! cli.hasOption(noDeleteOption);
		prune_reused = cli.hasOption(pruneReusedOption);
		prune = ! cli.hasOption(noPruneOption);
		prune_last = prune && ! cli.hasOption(noPruneLastOption);
		verbose = cli.hasOption(verboseOption);
		if ( cli.hasOption(prefixOption) )
			prefix = cli.getOptionValue(prefixOption);
		timeFileName = cli.getOptionValue(timeFileOption);
	}
	
	private void clean() throws IOException {
		Set<String> to_keep = new HashSet<String>();
		Set<String> to_remove = new HashSet<String>();
		
		Collections.sort(created_families, new Comparator<ReachabilityFamily>(){
			@Override
			public int compare(ReachabilityFamily f1, ReachabilityFamily f2) {
				if ( f1.delay() < f2.delay() ) return -1;
				if ( f1.delay() > f2.delay() ) return 1;
				return 0;
			}
		});
		
		Iterator<ReachabilityFamily> i = created_families.iterator();
		while ( i.hasNext() ){
			ReachabilityFamily family = i.next();
			if ( ! i.hasNext() && prune_last ){
				to_remove.addAll(family.prunableNames());
			}
			else if ( i.hasNext() && prune )
				to_remove.addAll(family.prunableNames());
			if ( families_to_keep.contains(family.delay()) || ! delete ){
				to_keep.add(family.mainName());
			} else if ( delete ) {
				to_remove.add(family.mainName());
			}
		}
		
		to_remove.removeAll(to_keep);
		for ( String name : to_remove )
			dest_store.deleteTrace(name);
	}
	
	private ReachabilityFamily getFamily(long D) {
		return new ReachabilityFamily(dest_store,prefix,eta,tau,D);
	}

	@Override
	protected void initOptions() {
		super.initOptions();
		graph_options.setOptions(options);
		options.addOption(null, everyOption, true, "Keep only every <arg> graphs");
		options.addOption(null, noDeleteOption, false, "Do not delete intermediate reachability families");
		options.addOption(null, pruneReusedOption, false, "Consider reused traces for pruning");
		options.addOption(null, noPruneOption, false, "Do not prune siblings from calculated reachability families");
		options.addOption(null, noPruneLastOption, false, "Do not prune siblings from the last calculated reachability family");
		options.addOption(null, minDelayOption, true, "Skip straight to calculating reachability traces greater than <arg>");
		options.addOption(null, verboseOption, false, "Be verbose");
		options.addOption(null, prefixOption, true, "Prefix for reachability traces (default: name of the 'links' trace)");
		options.addOption(null, timeFileOption, true, "Write calculation times in milliseconds to file <arg>");
	}

	private void initTimeFile() throws IOException{
		if ( timeFileName != null ){
			time_writer = new BufferedWriter(new FileWriter(timeFileName));
		}
	}
	
	private void stopTimeFile() throws IOException{
		if ( time_writer != null )
			time_writer.close();
	}
	
	private void armTimer(){
		ref_time = System.currentTimeMillis();
	}
	
	private void stopTimer(long delay) throws IOException{
		long dt = System.currentTimeMillis()-ref_time;
		if ( time_writer != null )
			time_writer.write(delay+" "+dt+"\n");
	}
	
	@Override
	protected void run() throws IOException, AlreadyExistsException, LoadTraceException, NoSuchTraceException {
		initTimeFile();
		
		if ( u_tau == 0 ){
			tps = orig_store.getTrace(ccsName).ticsPerSecond();
			if ( prefix == null ) prefix = ccsName;
		} else {
			tps = orig_store.getTrace(linksName).ticsPerSecond();
			if ( prefix == null ) prefix = linksName;
		}
		
		eta = (long)(u_eta*tps);
		tau = (long)(u_tau*tps);
		delay = (long)(u_delay*tps);
		if ( tau < eta && tau > 0 ){ // we assume here that tau is divisor of eta, hence a eta-regular graph is also tau-regular
			eta = tau;
		}
		
		long T = (tau==0)? eta : tau; // for tau=0 traces, use eta 
		
		long q=0;
		ReachabilityFamily rf_e=null, rf_m=null, rf=null, tmp=null;
		
		// first check what we already have in dest_store
		// and figure out the largest quotient that we will have to calculate
		if ( every != null ){
			every *= tps;
			rf_e = getFamily(every);
			if ( ! rf_e.isComplete() ){
				q = every/T;
			} else {
				log("Reachability family "+every/tps+" is complete. Reusing.");
				if ( prune_reused )
					created_families.add(rf_e);
			}
			if ( min_delay != null ){
				min_delay *= tps;
				rf_m = getFamily(min_delay);
				if ( ! rf_m.isComplete() ){
					q = Math.max(q, min_delay/T);
				} else {
					log("Reachability family "+min_delay/tps+" is complete. Reusing.");
					if ( prune_reused )
						created_families.add(rf_m);
				}
			}
		} else {
			rf = getFamily(delay);
			if ( ! rf.hasMain() ){
				q = delay/T;
			} else {
				log("Reachability trace "+delay/tps+" already exists.");
				if ( prune_reused )
					created_families.add(rf);
			}
		}
		
		// if we have a strictly positive quotient, run the exponential calculation
		if ( q > 0 ){
			if ( tau == 0 ){
				tmp = fromConnectedComponents();
			} else {
				tmp = fromLinks();
			}
			exponentiate(tmp, getMaxExponent(q));
		}
		
		// then combine the results
		if ( every != null ){
			long d = 0;
			if ( ! rf_e.isComplete() )
				rf_e = combine(T,every);
			rf = rf_e;
			d = rf_e.delay();
			if ( min_delay != null ){
				if ( ! rf_m.isComplete() )
					rf_m = combine(T,min_delay);
				rf = rf_m;
				d = rf_m.delay();
			}
			families_to_keep.add(rf.delay());
			while ( d < delay ){
				tmp = add(rf, rf_e);
				d += rf_e.delay();
				rf = tmp;
				families_to_keep.add(rf.delay());
			}
		} else {
			if ( ! rf.hasMain() )
				rf = combine(T,delay);
			families_to_keep.add(rf.delay());
		} 
		
		clean();
		stopTimeFile();
	}
	
	private void log(String str){
		if ( verbose )
			System.out.println(str);
	}
	
	private ReachabilityFamily add(ReachabilityFamily rf1, ReachabilityFamily rf2) throws IOException, AlreadyExistsException, LoadTraceException {
		long _delay = rf1.delay() + rf2.delay();
		log("Initializing reachability family "+_delay/tps+" from "+rf1.delay()/tps+"+"+rf2.delay()/tps);
		ReachabilityFamily rf = getFamily(_delay);
		boolean has_new = false;
		for ( Long d : rf.delays() ){
			if ( rf.hasMember(d) ){
				log("Reachability trace "+d/tps+" already exists in family. Skipping.");
				if ( prune_reused )
					created_families.add(rf);
			} else {
				log("Calculating reachability trace "+d/tps);
				armTimer();
				new AddingReachableConverter(rf.newMember(d), rf1, rf2, d).convert();
				stopTimer(d);
				has_new = true;
			}
		}
		separator();
		if ( has_new )
			created_families.add(rf);
		return rf;
	}
	
	private void separator(){
		log("---------------------------");
	}
	
	
	private ReachabilityFamily fromLinks() throws AlreadyExistsException, LoadTraceException, NoSuchTraceException, IOException{
		LinkTrace links = (LinkTrace)orig_store.getTrace(linksName);
		log("Initializing reachability family "+tau/tps+" from link trace '"+links.name()+"'");
		ReachabilityFamily rf = getFamily(tau);
		boolean has_new = false;
		for ( Long d : rf.delays() ){
			if ( rf.hasMember(d) ){
				log("Reachability trace "+d/tps+" already exists in family. Skipping.");
				if ( prune_reused )
					created_families.add(rf);
			} else {
				log("Calculating reachability trace "+d/tps);
				armTimer();
				new LinksToReachableConverter(rf.newMember(d), links, eta, tau, d).convert();
				stopTimer(d);
				has_new = true;
			}
		}
		if ( has_new )
			created_families.add(rf);
		separator();
		return rf;
	}
	
	private ReachabilityFamily fromConnectedComponents() throws IOException, AlreadyExistsException, LoadTraceException, NoSuchTraceException{
		GroupTrace ccs = (GroupTrace)orig_store.getTrace(ccsName);
		log("Initializing reachability family "+eta/tps+" from connected components trace '"+ccs.name()+"'");
		ReachabilityFamily rf = getFamily(0);
		if ( rf.hasMember(0) ){
			log("Reachability trace 0 already exists in family. Skipping.");
			if ( prune_reused )
				created_families.add(rf);
		} else {
			log("Calculating reachability trace 0");
			armTimer();
			new ConnectedComponentsToReachableConverter(rf.newMember(0), ccs, eta).convert();
			stopTimer(0);
			created_families.add(rf);
		}
		ReachabilityFamily rf1 = getFamily(eta);
		if ( rf1.hasMember(eta) ){
			log("Reachability trace "+eta/tps+" already exists in family. Skipping.");
		} else {
			log("Calculating reachability trace "+eta/tps);
			armTimer();
			new AddingReachableConverter(rf1.newMember(eta), rf, rf, eta).convert();
			stopTimer(eta);
			created_families.add(rf1);
		}
		separator();
		return rf1;
	}
	
	
	private int getMaxExponent(long n){
		int i = 0;
		while ( n != 0 ){
			n >>= 1;
			i++;
		}
		return i;
	}
	
	private void exponentiate(ReachabilityFamily rf, int e) throws AlreadyExistsException, LoadTraceException, IOException{
		ReachabilityFamily next;
		ReachabilityFamily cur = rf;
		for ( int i=1; i<e; ++i){
			next = add(cur, cur);
			cur = next;
		}
	}
	
	private ReachabilityFamily combine(long T, long delay) throws LoadTraceException, NoSuchTraceException, AlreadyExistsException, IOException{
		ReachabilityFamily rf = null;
		long d=1;
		long mul = delay/T;
		while ( mul != 0 ){
			if ( (mul&1) == 1 ){
				if ( rf == null ){
					rf = getFamily(d*T);
				} else {
					ReachabilityFamily o = getFamily(d*T);
					ReachabilityFamily tmp = add(rf, o);
					rf = tmp;
				}
			}
			mul >>= 1;
			d*=2;
		}
		return rf;
	}
}
