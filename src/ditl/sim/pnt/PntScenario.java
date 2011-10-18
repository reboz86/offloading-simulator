package ditl.sim.pnt;

import java.io.*;
import java.util.Arrays;

import org.apache.commons.cli.*;

import ditl.*;
import ditl.Store.*;
import ditl.WritableStore.AlreadyExistsException;
import ditl.cli.*;
import ditl.graphs.*;
import ditl.graphs.cli.GraphOptions;
import ditl.sim.*;
import ditl.sim.BufferEvent;
import ditl.transfers.*;

public class PntScenario extends WriteApp {
	
	protected final static String bufferSizeOption = "buffer-size";
	protected final static String upBitrateOption = "up-bitrate";
	protected final static String downBitrateOption = "down-bitrate";
	protected final static String adhocBitrateOption = "adhoc-bitrate";
	protected final static String msgDelayOption = "message-delay";
	protected final static String msgPeriodOption = "message-period";
	protected final static String minTimeOption = "min-time";
	protected final static String maxTimeOption = "max-time";
	protected final static String msgSizeOption = "message-size";
	protected final static String panicTimeOption = "panic-time";
	protected final static String seedOption = "seed";
	protected final static String numInitCopiesOption = "n-init-copies";
	protected final static String randomPushOption = "rand-push";
	protected final static String sendIncrOption = "send-incr";
	
	private Integer root_id = -42;
	
	GraphOptions graph_options = new GraphOptions(GraphOptions.PRESENCE, GraphOptions.LINKS, GraphOptions.GROUPS);
	Long seed;
	Long min_time, max_time;
	double adhoc_bitrate;
	double infra_up_bitrate;
	double infra_down_bitrate;
	int msgSize;
	long msgPeriod;
	long msgDelay;
	int bufferSize;
	long panic_time;
	long send_incr;
	Store orig_store;
	String[] store_names;
	NumToPush num_to_push;
	WhoToPush who_to_push;
	private boolean simple;
	
	public final static String PKG_NAME = "pnt";
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
		
		String linksName = graph_options.get(GraphOptions.LINKS); 
		LinkTrace links = (LinkTrace)orig_store.getTrace(linksName);
		StatefulReader<LinkEvent,Link> linkReader = links.getReader();
		Bus<Link> linkBus = new Bus<Link>();
		Bus<LinkEvent> linkEventBus = new Bus<LinkEvent>();
		linkReader.setBus(linkEventBus);
		linkReader.setStateBus(linkBus);
		
		runner.addGenerator(presenceReader);
		runner.addGenerator(linkReader);
		
		Bus<BufferEvent> bufferBus = new Bus<BufferEvent>();
		
		long tps = presence.ticsPerSecond();
		
		World world = new World();
		
		adhoc_bitrate /= 8*tps;
		AdHocRadio adhoc = new AdHocRadio(world, new ConstantBitrateGenerator(adhoc_bitrate));
		runner.addGenerator(adhoc);
		
		infra_up_bitrate /= 8*tps;
		infra_down_bitrate /= 8*tps;
		PervasiveInfraRadio infra = new PervasiveInfraRadio(world, 
				new ConstantBitrateGenerator(infra_down_bitrate), 
				new ConstantBitrateGenerator(infra_up_bitrate), 
				root_id);
		runner.addGenerator(infra);
		
		msgPeriod *= tps;
		msgDelay *= tps;
		panic_time *= tps;
		send_incr *= tps;
		
		min_time = (min_time != null)? min_time * tps : Long.MIN_VALUE;
		max_time = (max_time != null)? max_time * tps : Long.MAX_VALUE;
		
		final InfraRouter infra_router = new InfraRouter(infra, who_to_push, num_to_push, send_incr, panic_time, root_id, bufferSize, bufferBus);
		runner.addGenerator(infra_router);
		presenceBus.addListener(infra_router.presenceListener());
		presenceEventBus.addListener(infra_router.presenceEventListener());
		
		MessageGenerator msgGenerator = new MessageGenerator(msgPeriod,msgPeriod,msgDelay,msgDelay, 
				new MessageFactory<BroadcastMessage>(msgSize, msgSize){
					@Override
					public BroadcastMessage getNew(long creationTime, long expirationTime) {
						return new BroadcastMessage(infra_router, nextBytes(), creationTime, expirationTime);
					}
			});
		runner.addGenerator(msgGenerator);
		
		PntRouterFactory routerFactory = null;		
		if ( simple ){
			routerFactory = new PntRouterFactory(infra_router, infra, adhoc, bufferSize, bufferBus);
		}
		
		world.setRouterFactory(routerFactory);
		presenceBus.addListener(world.presenceListener());
		presenceEventBus.addListener(world.presenceEventListener());
		
		linkBus.addListener(adhoc.linkListener());
		linkEventBus.addListener(adhoc.linkEventListener());
		
		MessageTrace messages = (MessageTrace)_store.newTrace("messages", MessageTrace.type, force);
		BufferTrace buffers = (BufferTrace)_store.newTrace("buffers", BufferTrace.type, force);
		TransferTrace adhoc_transfers = (TransferTrace)_store.newTrace("adhoc_transfers", TransferTrace.type, force);
		TransferTrace infra_transfers = (TransferTrace)_store.newTrace("infra_transfers", TransferTrace.type, force);
		MessageEventLogger msgLogger = new MessageEventLogger(messages.getWriter(links.snapshotInterval()));
		BufferEventLogger bufferLogger = new BufferEventLogger(buffers.getWriter(links.snapshotInterval()));
		TransferEventLogger adhocTransferLogger = new TransferEventLogger(adhoc_transfers.getWriter(links.snapshotInterval()));
		TransferEventLogger infraTransferLogger = new TransferEventLogger(infra_transfers.getWriter(links.snapshotInterval()));
		
		msgGenerator.addListener(msgLogger);
		bufferBus.addListener(bufferLogger);
		presenceBus.addListener(bufferLogger.presenceListener());
		presenceEventBus.addListener(bufferLogger.presenceEventListener());
		adhoc.addListener(adhocTransferLogger);
		infra.addListener(infraTransferLogger);
		
		msgGenerator.addListener(world);
		
		runner.run();
		
		msgLogger.close();
		bufferLogger.close();
		adhocTransferLogger.close();
		infraTransferLogger.close();
	}
	
	@Override
	protected void initOptions() {
		super.initOptions();
		graph_options.setOptions(options);
		options.addOption(null,bufferSizeOption,true,"Size of router buffers (bytes)");
		options.addOption(null,adhocBitrateOption,true,"Ad Hoc radio bitrate (bits/s)");
		options.addOption(null,upBitrateOption,true,"Infrastructure upload bitrate (bits/s)");
		options.addOption(null,downBitrateOption,true,"Infrastructure download bitrate (bits/s)");
		options.addOption(null,msgDelayOption,true,"Maximum allowed delay for messages (s)");
		options.addOption(null,msgPeriodOption,true,"Message creation period (s)");
		options.addOption(null,msgSizeOption,true,"Message size (bytes)");
		options.addOption(null,seedOption,true,"Seed for the random number generator");
		options.addOption(null,panicTimeOption,true,"Panic time");
		options.addOption(null,minTimeOption,true,"Time of first sent message");
		options.addOption(null,maxTimeOption,true,"Time of last sent message");
		options.addOption(null,sendIncrOption,true,"Sending increment");
		OptionGroup whoGroup = new OptionGroup();
		whoGroup.setRequired(false);
		whoGroup.addOption(new Option(null,randomPushOption,false,"Push to random nodes"));
		options.addOptionGroup(whoGroup);
		OptionGroup numGroup = new OptionGroup();
		numGroup.setRequired(false);
		numGroup.addOption(new Option(null,numInitCopiesOption,true,"Push <arg> copies and then wait"));
		options.addOptionGroup(numGroup);
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
		adhoc_bitrate = Double.parseDouble(cli.getOptionValue(adhocBitrateOption,"8388608")); // default 1 MBytes/s
		infra_up_bitrate = Double.parseDouble(cli.getOptionValue(upBitrateOption,"81920")); // default 10Kbytes/s
		infra_down_bitrate = Double.parseDouble(cli.getOptionValue(downBitrateOption,"819200")); // default 100Kbytes/s
		
		msgSize = Integer.parseInt(cli.getOptionValue(msgSizeOption,"1048576")); // default 1 Mbyte
		msgPeriod = Long.parseLong(cli.getOptionValue(msgPeriodOption,"600")); // default: 1 msg/10minute
		msgDelay = Long.parseLong(cli.getOptionValue(msgDelayOption,"600")); // default: 10 minutes
		
		if ( cli.hasOption(minTimeOption) )
			min_time = Long.parseLong(cli.getOptionValue(minTimeOption) );
		if ( cli.hasOption(maxTimeOption))
			max_time = Long.parseLong(cli.getOptionValue(maxTimeOption) );
		
		bufferSize = Integer.parseInt(cli.getOptionValue(bufferSizeOption,"104857600")); // default 100 Mbytes
		panic_time = Long.parseLong(cli.getOptionValue(panicTimeOption,"11")); // With default parameters, it takes a little over 10 seconds to download a msg from the infrastructure
		send_incr = Long.parseLong(cli.getOptionValue(sendIncrOption, "1")); // by default, try sending every second 
		
		// parse who to push
		if ( cli.hasOption(randomPushOption) ){
			who_to_push = new RandomWho();
		} else {
			who_to_push = new DefaultWhoToPush();
		}
		
		// parse num to push
		if ( cli.hasOption(numInitCopiesOption) ){
			num_to_push = new SinglePusher( Integer.parseInt(cli.getOptionValue(numInitCopiesOption)) );
		} else {
			num_to_push = new DefaultNumToPush();
		}
		
		simple = true; // TODO
		
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
		App app = new PntScenario();
		app.ready("", args);
		app.exec();
	}
}
