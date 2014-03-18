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
package ditl.cli;

import java.io.*;
import java.util.Arrays;

import org.apache.commons.cli.*;

import ditl.*;

public class CopyTraces extends App {

	protected File inStoreFile;
	protected File outStoreFile;
	protected String[] traceNames;
	
	public final static String PKG_NAME = null;
	public final static String CMD_NAME = "cp";
	public final static String CMD_ALIAS = null;
	
	@Override
	protected void parseArgs(CommandLine cli, String[] args) throws ParseException, HelpException {
		inStoreFile = new File(args[0]);
		outStoreFile = new File(args[1]);
		traceNames = Arrays.copyOfRange(args, 2, args.length);
		if ( traceNames.length == 0 )
			throw new HelpException();
	}

	@Override
	protected String getUsageString() {
		return "[OPTIONS] STORE OUT_STORE TRACE1 [TRACE2...]";
	}
	
	@Override
	protected void run() throws IOException, Store.NoSuchTraceException {
		Store inStore = Store.open(inStoreFile);
		WritableStore outStore = WritableStore.open(outStoreFile);
		for ( String name : traceNames ){
			Trace<?> trace = inStore.getTrace(name);
			outStore.copyTrace(inStore, trace);
		}
		inStore.close();
		outStore.close();
	}
}
