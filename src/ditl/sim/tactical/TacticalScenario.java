package ditl.sim.tactical;

import java.io.*;
import java.util.*;

import org.apache.commons.cli.*;

import ditl.*;
import ditl.Store.*;
import ditl.WritableStore.AlreadyExistsException;
import ditl.cli.*;
import ditl.graphs.*;
import ditl.graphs.AdjacencySet;
import ditl.graphs.cli.GraphOptions;
import ditl.sim.*;
import ditl.sim.BufferEvent;
import ditl.transfers.*;

public class TacticalScenario extends WriteApp {
	
	protected final static String bufferSizeOption = "buffer-size";
	protected final static String uhfBitrateOption = "uhf-bitrate";
	protected final static String vhfBitrateOption = "vhf-bitrate";
	protected final static String controlBitrateOption = "control-bitrate";
	protected final static String msgDelayOption = "message-delay";
	protected final static String msgPeriodOption = "message-period";
	protected final static String msgSizeOption = "message-size";
	protected final static String intraOnlyOption = "intra-only";
	protected final static String ccOracleOption = "cc-oracle";
	protected final static String reachableOracleOption = "reachable-oracle";
	protected final static String panicTimeOption = "panic-time";
	protected final static String seedOption = "seed";
	
	GraphOptions graph_options = new GraphOptions(GraphOptions.PRESENCE, GraphOptions.LINKS, GraphOptions.GROUPS);
	Long seed;
	String vhfLinksName;
	boolean intra_only;
	double uhf_bitrate;
	double vhf_bitrate;
	double control_bitrate;
	int msgSize;
	long msgPeriod;
	long msgDelay;
	int bufferSize;
	long panic_time;
	long guard_time;
	boolean cc_oracle, reachable_oracle, panic;
	Store orig_store;
	String[] store_names;
	
	public final static String PKG_NAME = "tactical";
	public final static String CMD_NAME = "sim";
	public final static String CMD_ALIAS = null;

	@Override
	protected void run() throws IOException, NoSuchTraceException,
			AlreadyExistsException, LoadTraceException {
		RNG.init(seed);
		
		PresenceTrace presence = (PresenceTrace)orig_store.getTrace(graph_options.get(GraphOptions.PRESENCE));
		StatefulReader<PresenceEvent,Presence> presenceReader = presence.getReader();
		Bus<Presence> presenceBus = new Bus<Presence>();
		Bus<PresenceEvent> presenceEventBus = new Bus<PresenceEvent>();
		presenceReader.setBus(presenceEventBus);
		presenceReader.setStateBus(presenceBus);
		
		Runner runner = new Runner(presence.ticsPerSecond(), presence.minTime(), presence.maxTime());
		
		String linksName = (intra_only)? "intra_links" : graph_options.get(GraphOptions.LINKS); 
		LinkTrace links = (LinkTrace)orig_store.getTrace(linksName);
		StatefulReader<LinkEvent,Link> linkReader = links.getReader();
		Bus<Link> linkBus = new Bus<Link>();
		Bus<LinkEvent> linkEventBus = new Bus<LinkEvent>();
		linkReader.setBus(linkEventBus);
		linkReader.setStateBus(linkBus);
		
		LinkTrace vhf_links = (LinkTrace)orig_store.getTrace("vhf_links");
		StatefulReader<LinkEvent,Link> vhfLinkReader = vhf_links.getReader();
		Bus<Link> vhfLinkBus = new Bus<Link>();
		Bus<LinkEvent> vhfLinkEventBus = new Bus<LinkEvent>();
		vhfLinkReader.setBus(vhfLinkEventBus);
		vhfLinkReader.setStateBus(vhfLinkBus);
		
		runner.addGenerator(presenceReader);
		runner.addGenerator(linkReader);
		runner.addGenerator(vhfLinkReader);
		
		GroupTrace groupTrace = (GroupTrace)orig_store.getTrace("groups");
		Set<Group> groups = groupTrace.staticGroups();
		
		Bus<BufferEvent> bufferBus = new Bus<BufferEvent>();
		
		long tps = presence.ticsPerSecond();
		
		World world = new World();
		
		uhf_bitrate /= 8*tps;
		AdHocRadio uhf = new AdHocRadio(world, new ConstantBitrateGenerator(uhf_bitrate));
		runner.addGenerator(uhf);
		
		vhf_bitrate /= 8*tps;
		AdHocRadio vhf_data = new AdHocRadio(world, new ConstantBitrateGenerator(vhf_bitrate));
		runner.addGenerator(vhf_data);
		
		control_bitrate /= 8*tps;
		AdHocRadio vhf_control = new AdHocRadio(world, new ConstantBitrateGenerator(control_bitrate));
		runner.addGenerator(vhf_control);
		
		msgPeriod *= tps;
		msgDelay *= tps;
		
		MessageGenerator msgGenerator = new MessageGenerator(msgPeriod,msgPeriod,msgDelay,msgDelay,new GroupMessage.Factory(world, msgSize, msgSize));
		runner.addGenerator(msgGenerator);
		
		panic_time *= tps;
		guard_time = (long)(2*(double)msgSize / vhf_bitrate);
		
		TacticalRouterFactory routerFactory = null;		
		if ( cc_oracle ){
			GroupTrace ccs = (GroupTrace)orig_store.getTrace("ccs");
			StatefulReader<GroupEvent,Group> groupReader = ccs.getReader();
			Bus<GroupEvent> groupEventBus = new Bus<GroupEvent>();
			Bus<Group> groupBus = new Bus<Group>();
			groupReader.setBus(groupEventBus);
			groupReader.setStateBus(groupBus);
			runner.addGenerator(groupReader);
			
			PanicBus panicBus = new PanicBus();
			runner.addGenerator(panicBus);
			
			routerFactory = new CCRouterFactory(groups, bufferBus, uhf, vhf_data, vhf_control, 
					bufferSize, guard_time, panicBus, panic_time);
			groupEventBus.addListener(((CCRouterFactory)routerFactory).groupEventListener());
			groupBus.addListener(((CCRouterFactory)routerFactory).groupListener());			
			
		} else if ( reachable_oracle ){
			ReachabilityTrace reachable = null;
			for ( Trace<?> trace : orig_store.listTraces(ReachabilityTrace.type)){
				reachable = (ReachabilityTrace)trace;
				if ( reachable.delay() == msgDelay )
					break;
			}
			if ( reachable == null ){
				System.err.println("Could not find reachability trace for delay "+msgDelay);
				System.exit(1);
			}
			StatefulReader<EdgeEvent,Edge> reachableReader = reachable.getReader();
			Bus<EdgeEvent> reachableEventBus = new Bus<EdgeEvent>();
			Bus<Edge> reachableBus = new Bus<Edge>();
			AdjacencySet.Edges adjacency = new AdjacencySet.Edges();
			reachableEventBus.addListener(adjacency.edgeEventListener());
			reachableBus.addListener(adjacency.edgeListener());
			reachableReader.setBus(reachableEventBus);
			reachableReader.setStateBus(reachableBus);
			runner.addGenerator(reachableReader);
			
			PanicBus panicBus = new PanicBus();
			runner.addGenerator(panicBus);
			
			routerFactory = new ReachableRouterFactory(groups, bufferBus, uhf, vhf_data, vhf_control, 
					bufferSize, guard_time, panicBus, panic_time, adjacency);
			
		} else if ( panic ) {
			PanicBus panicBus = new PanicBus();
			runner.addGenerator(panicBus);
			routerFactory = new PanicRouterFactory(groups, bufferBus, uhf, vhf_data, vhf_control, 
					bufferSize, guard_time, panicBus, panic_time );
		} else {
			routerFactory = new TacticalRouterFactory(groups, bufferBus, uhf, vhf_data, vhf_control, bufferSize, guard_time);
		}
		
		//world.setRouterFactory(routerFactory);
		presenceBus.addListener(world.presenceListener());
		presenceEventBus.addListener(world.presenceEventListener());
		
		linkBus.addListener(uhf.linkListener());
		linkEventBus.addListener(uhf.linkEventListener());
		
		vhfLinkBus.addListener(vhf_data.linkListener());
		vhfLinkEventBus.addListener(vhf_data.linkEventListener());
		vhfLinkBus.addListener(vhf_control.linkListener());
		vhfLinkEventBus.addListener(vhf_control.linkEventListener());
		
		MessageTrace messages = (MessageTrace)_store.newTrace("messages", MessageTrace.type, force);
		BufferTrace buffers = (BufferTrace)_store.newTrace("buffers", BufferTrace.type, force);
		TransferTrace uhf_transfers = (TransferTrace)_store.newTrace("uhf_transfers", TransferTrace.type, force);
		TransferTrace vhf_transfers = (TransferTrace)_store.newTrace("vhf_transfers", TransferTrace.type, force);
		TransferTrace control_transfers = (TransferTrace)_store.newTrace("control_transfers", TransferTrace.type, force);
		MessageEventLogger msgLogger = new MessageEventLogger(messages.getWriter(links.snapshotInterval()));
		BufferEventLogger bufferLogger = new BufferEventLogger(buffers.getWriter(links.snapshotInterval()));
		TransferEventLogger uhfTransferLogger = new TransferEventLogger(uhf_transfers.getWriter(links.snapshotInterval()));
		TransferEventLogger vhfTransferLogger = new TransferEventLogger(vhf_transfers.getWriter(links.snapshotInterval()));
		TransferEventLogger controlTransferLogger = new TransferEventLogger(control_transfers.getWriter(links.snapshotInterval()));
		
		msgGenerator.addListener(msgLogger);
		bufferBus.addListener(bufferLogger);
		presenceBus.addListener(bufferLogger.presenceListener());
		presenceEventBus.addListener(bufferLogger.presenceEventListener());
		uhf.addListener(uhfTransferLogger);
		vhf_data.addListener(vhfTransferLogger);
		vhf_control.addListener(controlTransferLogger);
		
		msgGenerator.addListener(world);
		
		runner.run();
		
		msgLogger.close();
		bufferLogger.close();
		uhfTransferLogger.close();
		vhfTransferLogger.close();
		controlTransferLogger.close();

	}
	
	@Override
	protected void initOptions() {
		super.initOptions();
		graph_options.setOptions(options);
		options.addOption(null,bufferSizeOption,true,"Size of router buffers (bytes)");
		options.addOption(null,uhfBitrateOption,true,"Bitrate of UHF (bits/s)");
		options.addOption(null,vhfBitrateOption,true,"Bitrate of VHF data (bits/s)");
		options.addOption(null,controlBitrateOption,true,"Bitrate of VHF control (bits/s)");
		options.addOption(null,msgDelayOption,true,"Maximum allowed delay for messages (s)");
		options.addOption(null,msgPeriodOption,true,"Message creation period (s)");
		options.addOption(null,msgSizeOption,true,"Message size (bytes)");
		options.addOption(null,seedOption,true,"Seed for the random number generator");
		options.addOption(null,panicTimeOption,true,"Panic time");
		OptionGroup optionGroup = new OptionGroup();
		optionGroup.addOption(new Option(null,intraOnlyOption,false,"Do not allow inter-group communications"));
		optionGroup.addOption(new Option(null,ccOracleOption,false,"Leaders know about connected components"));
		optionGroup.addOption(new Option(null,reachableOracleOption,false,"Leaders know about reachability"));
		options.addOptionGroup(optionGroup);
	}
	
	@Override
	public String getUsageString(){
		return "[OPTIONS] OUT_STORE STORE1 [STORE2...]";
	}
	
	@Override
	protected void parseArgs(CommandLine cli, String[] args)
			throws ParseException, ArrayIndexOutOfBoundsException,
			HelpException {
		
		super.parseArgs(cli, args);
		store_names = Arrays.copyOfRange(args, 1, args.length);
		graph_options.parse(cli);
		seed = Long.parseLong(cli.getOptionValue(seedOption,"0"));
		intra_only = cli.hasOption(intraOnlyOption);
		uhf_bitrate = Double.parseDouble(cli.getOptionValue(uhfBitrateOption,"524288")); // default 512 Kbits/s
		vhf_bitrate = Double.parseDouble(cli.getOptionValue(vhfBitrateOption,"4096")); // default 4Kbits/s
		control_bitrate = Double.parseDouble(cli.getOptionValue(controlBitrateOption,"1024")); // default 1Kbits/s
		
		msgSize = Integer.parseInt(cli.getOptionValue(msgSizeOption,"10240")); // default 10 Kbyte
		msgPeriod = Long.parseLong(cli.getOptionValue(msgPeriodOption,"60")); // default: 1 msg/minute
		msgDelay = Long.parseLong(cli.getOptionValue(msgDelayOption,"600")); // default: 10 minutes
		
		bufferSize = Integer.parseInt(cli.getOptionValue(bufferSizeOption,"104857600")); // default 100 Mbytes
		panic_time = Long.parseLong(cli.getOptionValue(panicTimeOption,"100")); // default 100 seconds (with default parameters, it takes 20 secs to send a message over VHF. 4*20 = 80
		
		cc_oracle = cli.hasOption(ccOracleOption);
		reachable_oracle = cli.hasOption(reachableOracleOption);
		panic = cli.hasOption(panicTimeOption);
	}
	
	@Override
	protected void init() throws IOException {
		super.init();
		File[] files = new File[store_names.length];
		for ( int i=0; i<store_names.length; ++i){
			files[i] = new File(store_names[i]);
		}
		orig_store = Store.open(files);
	}
	
	@Override
	protected void close() throws IOException {
		super.close();
		orig_store.close();
	}
	
	public static void main(String[] args) throws IOException{
		App app = new TacticalScenario();
		app.ready("", args);
		app.exec();
	}
}
