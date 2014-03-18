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
package ditl.graphs.cli;

import java.io.IOException;

import org.apache.commons.cli.*;

import ditl.*;
import ditl.Store.LoadTraceException;
import ditl.WritableStore.AlreadyExistsException;
import ditl.cli.ImportApp;
import ditl.graphs.*;


public class ImportMovement extends ImportApp {
	
	private ExternalFormat ext_fmt = new ExternalFormat(ExternalFormat.NS2, ExternalFormat.ONE);
	private Long maxTime;
	private long ticsPerSecond;
	private Double timeMul;
	private GraphOptions graph_options = new GraphOptions(GraphOptions.MOVEMENT);
	private long offset;
	private boolean fix_pause_times;
	private boolean use_id_map;
	private int min_id;
	
	private String fixPauseTimesOption = "fix-pause-times";
	
	public final static String PKG_NAME = "graphs";
	public final static String CMD_NAME = "import-movement";
	public final static String CMD_ALIAS = "im";

	@Override
	protected void parseArgs(CommandLine cli, String[] args) throws ParseException, ArrayIndexOutOfBoundsException, HelpException {
		super.parseArgs(cli, args);
		graph_options.parse(cli);
		ext_fmt.parse(cli);
		ticsPerSecond = getTicsPerSecond(cli.getOptionValue(destTimeUnitOption,"ms"));
		Long otps = getTicsPerSecond(cli.getOptionValue(origTimeUnitOption,"s"));
		timeMul = getTimeMul(otps,ticsPerSecond);
		if ( timeMul == null )
			throw new HelpException();
		offset = Long.parseLong(cli.getOptionValue(offsetOption,"0")) * ticsPerSecond;
		if ( cli.hasOption(maxTimeOption) )
			maxTime = Long.parseLong(cli.getOptionValue(maxTimeOption)) * ticsPerSecond;
		fix_pause_times = cli.hasOption(fixPauseTimesOption);
		use_id_map = cli.hasOption(stringIdsOption);
		min_id = Integer.parseInt(cli.getOptionValue(minIdOption, "0"));
	}
	
	@Override
	protected void run() throws IOException, AlreadyExistsException, LoadTraceException {
		MovementTrace movement = (MovementTrace) _store.newTrace(graph_options.get(GraphOptions.MOVEMENT), MovementTrace.type, force);
		IdGenerator id_gen = (use_id_map)? new IdMap.Writer(min_id) : new OffsetIdGenerator(min_id);
		if ( ext_fmt.is(ExternalFormat.NS2) )
			NS2Movement.fromNS2(movement, _in, maxTime, timeMul, ticsPerSecond, offset, fix_pause_times, id_gen);
		else
			ONEMovement.fromONE(movement, _in, maxTime, timeMul, ticsPerSecond, offset, id_gen);
	}


	@Override
	protected void initOptions() {
		super.initOptions();
		graph_options.setOptions(options);
		ext_fmt.setOptions(options);
		options.addOption(null, maxTimeOption, true, "maximum movement time in seconds");
		options.addOption(null, origTimeUnitOption, true, "time unit of original trace [s, ms, us, ns] (default: s)");
		options.addOption(null, destTimeUnitOption, true, "time unit of destination trace [s, ms, us, ns] (default: ms)");
		options.addOption(null, offsetOption, true, "offset to add to all times in seconds (default 0)");
		options.addOption(null, fixPauseTimesOption, false, "fix missing pause times in NS2");
		options.addOption(null, stringIdsOption, false, "treat node ids as strings (default: false)");
		options.addOption(null, minIdOption, true, "ensure that all imported ids are greater than <arg> (default: 0)");
	}
}
