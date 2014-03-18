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
package ditl.graphs;

import java.util.*;

import ditl.*;
import ditl.Store.LoadTraceException;
import ditl.Store.NoSuchTraceException;
import ditl.WritableStore.AlreadyExistsException;

public class ReachabilityFamily {

	private String _prefix;
	private Store _store;
	private long _tau;
	private long _delay;
	private long _eta;
	private ReachabilityTrace[] members;
	
	public ReachabilityFamily(Store store, String prefix, long eta, long tau, long delay){
		_store = store;
		_prefix = prefix;
		_eta = eta;
		_tau = tau;
		_delay = delay;
		loadMembers();
	}
	
	public long tau(){
		return _tau;
	}
	
	public long delay(){
		return _delay;
	}
	
	public long eta(){
		return _eta;
	}
	
	private void loadMembers() {
		if ( _tau > 0 ){
			int n = 2*(int)(_tau/_eta)-1;
			members = new ReachabilityTrace[n];
			for ( long d = _delay-_tau+_eta; d <= _delay+_tau-_eta; d+=_eta){
				try {
					members[j(d)] = getExisting(d); 
				} catch (LoadTraceException e) {
					members[j(d)] = null;
				} catch (NoSuchTraceException e) {
					members[j(d)] = null;
				}
			}
		} else {
			members = new ReachabilityTrace[1];
			try {
				members[0] = getExisting(_delay);
			} catch (LoadTraceException e) {
				members[0] = null;
			} catch (NoSuchTraceException e) {
				members[0] = null;
			}
		}
	}
	
	private int j(long delay){
		if ( _tau > 0 )
			return (int)(((delay-_delay+_tau)/_eta)-1);
		return 0;
	}
	
	public ReachabilityTrace getMember(long delay){
		return members[j(delay)];
	}
	
	public ReachabilityTrace getMemberByOffset(int k){
		if ( _tau > 0 ){
			int n = (int)(_tau/_eta)-1;
			return members[n+k];
		} else {
			return members[0];
		}
	}
	
	public ReachabilityTrace newMember(long delay) throws AlreadyExistsException, LoadTraceException{
		ReachabilityTrace rt = (ReachabilityTrace)((WritableStore)_store).newTrace(ReachabilityTrace.defaultName(_prefix, _tau, delay), ReachabilityTrace.type, true);
		members[j(delay)] = rt;
		return rt;
	}
	
	public boolean hasMember(long delay){
		return members[j(delay)] != null;
	}
	
	public boolean isComplete(){
		for ( ReachabilityTrace rt : members )
			if ( rt == null )
				return false;
		return true;
	}
	
	public boolean hasMain(){
		return members[j(_delay)] != null;
	}
	
	public Collection<String> prunableNames() {
		List<String> prunable = new LinkedList<String>();
		for ( int i=0; i<members.length; ++i ){
			ReachabilityTrace rt = members[i];
			if ( rt != null && rt.delay() != _delay ){
				prunable.add(rt.name());
			}
		}
		return prunable;
	}
	
	public String mainName() {
		ReachabilityTrace rt = members[j(_delay)];
		if ( rt == null ) return null;
		return rt.name();
	}
	
	public Collection<Long> delays(){
		if ( _tau > 0 ){
			List<Long> delays = new LinkedList<Long>();
			for ( long i=_delay-_tau+_eta; i<=_delay+_tau-_eta; i+=_eta)
				delays.add(i);
			return delays;
		}
		return Collections.singleton(_delay);
	}
	
	private ReachabilityTrace getExisting(long delay) throws LoadTraceException, NoSuchTraceException{
		return (ReachabilityTrace)_store.getTrace(ReachabilityTrace.defaultName(_prefix,_tau,delay));
	}

}
