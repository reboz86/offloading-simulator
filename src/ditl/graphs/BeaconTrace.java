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
package ditl.graphs;

import java.io.IOException;
import java.util.Set;

import ditl.*;

public class BeaconTrace extends Trace<Edge> implements Trace.Filterable<Edge> {
	
	public final static String type = "beacons";
	public final static String defaultName = "beacons";
	
	public final static String beaconningPeriodKey = "beaconning period";
	
	public BeaconTrace(Store store, String name, PersistentMap info) throws IOException {
		super(store, name, info, new Edge.Factory());
	}

	@Override
	public Filter<Edge> eventFilter(Set<Integer> group) {
		return new Edge.InternalGroupFilter(group);
	}
	
	public long beaconningPeriod(){
		return Long.parseLong(getValue(beaconningPeriodKey));
	}

	@Override
	public void copyOverTraceInfo(Writer<Edge> writer) {
		writer.setProperty( beaconningPeriodKey, getValue(beaconningPeriodKey));
	}
}
