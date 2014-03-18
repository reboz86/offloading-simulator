package ditl.sim.pnt;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.ParseException;

import ditl.Bus;
import ditl.Generator;
import ditl.Runner;
import ditl.StatefulReader;
import ditl.Store;
import ditl.Store.LoadTraceException;
import ditl.Store.NoSuchTraceException;
import ditl.WritableStore.AlreadyExistsException;
import ditl.cli.App;
import ditl.cli.WriteApp;
import ditl.graphs.Group;
import ditl.graphs.GroupEvent;
import ditl.graphs.GroupTrace;
import ditl.graphs.Link;
import ditl.graphs.LinkEvent;
import ditl.graphs.LinkTrace;
import ditl.graphs.Presence;
import ditl.graphs.PresenceEvent;
import ditl.graphs.PresenceTrace;
import ditl.graphs.cli.GraphOptions;
import ditl.sim.AdHocRadio;
import ditl.sim.BroadcastMessage;
import ditl.sim.BufferEvent;
import ditl.sim.BufferEventLogger;
import ditl.sim.ConstantBitrateGenerator;
import ditl.sim.MessageEventLogger;
import ditl.sim.MessageFactory;
import ditl.sim.MessageGenerator;
import ditl.sim.PervasiveInfraRadio;
import ditl.sim.RNG;
import ditl.sim.Router;
import ditl.sim.TransferEventLogger;
import ditl.sim.World;
import ditl.transfers.BufferTrace;
import ditl.transfers.MessageTrace;
import ditl.transfers.TransferTrace;

public class PntProvaScenario extends WriteApp{
	
	
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
	protected final static String guardTimeOption = "guard-time";
	protected final static String floatIntervalOption = "float-interval";
	protected final static String seedOption = "seed";
	
	protected final static String numInitCopiesOption = "n-init-copies";
	protected final static String randomPushOption = "rand-push";
	protected final static String gpsGravityOption = "gps-gravity";
	protected final static String gpsDensityOption = "gps-density";
	protected final static String gpsHybridOption = "gps-hybrid";
	protected final static String prioAverageOption = "prio-average";
	protected final static String prioNewestOption = "prio-newest";
	protected final static String prioOldestOption = "prio-oldest";
	protected final static String ccOption = "cc";
	protected final static String infraOnlyOption = "infra-only";
	protected final static String hyperbolicOption = "hyperbolic";
	protected final static String quadraticOption = "quadratic";
	protected final static String linearOption = "linear";
	protected final static String fastStartOption = "fast-start";
	protected final static String slowStartOption = "slow-start";
	protected final static String controlIncrOption = "ctrl-incr";
	protected final static String sendIncrOption = "send-incr";
	protected final static String dsOption = "dominating-set";
	
	// derivative based retransmission
	protected final static String derivativeOption = "derivative";
	protected final static String windowSizeOption = "window-size";
	protected final static String limitDerivativeOption = "deriv-limit";

	final static Integer root_id = -42;

	public final static String PKG_NAME = "pnt";
	public final static String CMD_NAME = "sim_prova";
	public final static String CMD_ALIAS = null;
	protected static final boolean D = false;


	private String[] store_names;
	private Store orig_store;
	
	GraphOptions graph_options = new GraphOptions(GraphOptions.PRESENCE, GraphOptions.LINKS, GraphOptions.GROUPS, GraphOptions.MOVEMENT);
	
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
	long guard_time;
	Long float_interval;
	long send_incr;
	long ctrl_incr;
	NumToPush num_to_push;
	WhoToPush who_to_push;
	private boolean ds_oracle = false;
	private boolean use_cc;
	private boolean infra_only;
	private boolean use_gps;
	private long windowSize;
	private long derivlimit;
	private long ackSize=256;
	
	
	
	@Override
	protected String getUsageString() {
		return "[OPTIONS] OUT_STORE STORE1 [STORE2...]";
		
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
		options.addOption(null,infraOnlyOption, false, "Only use infra");
		options.addOption(null,panicTimeOption,true,"Panic time");
		options.addOption(null,guardTimeOption,true,"Guard time");
		options.addOption(null,sendIncrOption,true,"Sending increment");
		options.addOption(null,controlIncrOption,true,"Sending increment for control messages");
		
		OptionGroup whoGroup = new OptionGroup();
		whoGroup.setRequired(false);
		whoGroup.addOption(new Option(null,randomPushOption,false,"Push to random nodes"));
		whoGroup.addOption(new Option(null,ccOption,false,"Nodes also report their neighbors."));
		whoGroup.addOption(new Option(null,gpsHybridOption,false,"GPS hybrid"));
		whoGroup.addOption(new Option(null,gpsDensityOption,false,"GPS density"));
		whoGroup.addOption(new Option(null,gpsGravityOption,false,"GPS gravity"));
		whoGroup.addOption(new Option(null,prioAverageOption,false,"Prio average nodes"));
		whoGroup.addOption(new Option(null,prioNewestOption,false,"Prio newest nodes"));
		whoGroup.addOption(new Option(null,prioOldestOption,false,"Prio oldest nodes"));
		options.addOptionGroup(whoGroup);
		
		OptionGroup numGroup = new OptionGroup();
		numGroup.setRequired(false);
		numGroup.addOption(new Option(null,numInitCopiesOption,true,"Push <arg> copies and then wait"));
		numGroup.addOption(new Option(null,linearOption,false,"Linear"));
		numGroup.addOption(new Option(null,fastStartOption,false,"Linear segment with 0.25 positive offset"));
		numGroup.addOption(new Option(null,slowStartOption,false,"Linear segment with -0.25 negative offset"));
		numGroup.addOption(new Option(null,hyperbolicOption,false,"Hyperbolic"));
		numGroup.addOption(new Option(null,quadraticOption,false,"Quadratic"));
		numGroup.addOption(new Option(null,derivativeOption,false,"Derivative based reinjection"));
		numGroup.addOption(new Option(null,windowSizeOption,true,"Moving window size"));
		numGroup.addOption(new Option(null,limitDerivativeOption,true,"Threshold value of the derivative before reinjection "));
		options.addOptionGroup(numGroup);
	}

	
	@Override
	protected void parseArgs(CommandLine cli, String[] args)
			throws ParseException, ArrayIndexOutOfBoundsException,
			HelpException {
		super.parseArgs(cli, args);
		
		store_names = Arrays.copyOfRange(args, 1, args.length);
		
		graph_options.parse(cli);
		
		seed = Long.parseLong(cli.getOptionValue(seedOption,"0"));
		// Interfaces Bitrate 
		adhoc_bitrate = Double.parseDouble(cli.getOptionValue(adhocBitrateOption,"8388608")); // default 1 MBytes/s
		infra_up_bitrate = Double.parseDouble(cli.getOptionValue(upBitrateOption,"81920")); // default 10Kbytes/s
		infra_down_bitrate = Double.parseDouble(cli.getOptionValue(downBitrateOption,"819200")); // default 100Kbytes/s
		
		// Size of messages exchanged
		msgSize = Integer.parseInt(cli.getOptionValue(msgSizeOption,"1048576")); // default 1 Mbyte
		msgPeriod = Long.parseLong(cli.getOptionValue(msgPeriodOption,"600")); // default: 1 msg/10 minutes
		msgDelay = Long.parseLong(cli.getOptionValue(msgDelayOption,"600")); // default: 10 minutes

		if ( cli.hasOption(minTimeOption) )
			min_time = Long.parseLong(cli.getOptionValue(minTimeOption) );
		if ( cli.hasOption(maxTimeOption))
			max_time = Long.parseLong(cli.getOptionValue(maxTimeOption) );

		bufferSize = Integer.parseInt(cli.getOptionValue(bufferSizeOption,"104857600")); // default 100 Mbytes

		guard_time = Long.parseLong(cli.getOptionValue(guardTimeOption,"1")); // by default, pad with one second
		send_incr = Long.parseLong(cli.getOptionValue(sendIncrOption, "1")); // by default, try sending every second
		ctrl_incr = Long.parseLong(cli.getOptionValue(controlIncrOption, "60")); // by default, try sending control messages once per minute

		panic_time=Long.parseLong(cli.getOptionValue(panicTimeOption, "-1000")); // by default, never go in panic time;
		
		windowSize = Long.parseLong(cli.getOptionValue(windowSizeOption, "10")); // by default, window size is 10 second
		derivlimit=Long.parseLong(cli.getOptionValue(limitDerivativeOption, "0")); // by default, reinjection only when derivative is 0;
		
		infra_only = cli.hasOption(infraOnlyOption);

		// parse who to push
		if ( ! infra_only ){
			ds_oracle = cli.hasOption(dsOption);
			if ( ! ds_oracle ){
				if ( cli.hasOption(ccOption) ){
					who_to_push = new ConnectedComponentWho();

					use_cc = true;
				}
				else if ( cli.hasOption(randomPushOption) ){
					who_to_push = new RandomWho();
				}else if ( cli.hasOption(gpsGravityOption) ){
					who_to_push = new GPSGravity();
					use_gps = true;
				} else if ( cli.hasOption(gpsDensityOption) ){
					who_to_push = new GPSDensity();
					use_gps = true;
				} else if ( cli.hasOption(gpsHybridOption) ){
					who_to_push = new GPSHybrid();
					use_gps = true;
				} else if ( cli.hasOption(ccOption) ){
					who_to_push = new ConnectedComponentWho();
					use_cc = true;
				} else if ( cli.hasOption(prioAverageOption) ){
					who_to_push = new PrioAverage();
				} else if ( cli.hasOption(prioOldestOption) ){
					who_to_push = new PrioOldest();
				} else if ( cli.hasOption(prioNewestOption) ){
					who_to_push = new PrioNewest();
				}else {
					who_to_push = new DefaultWhoToPush();
				}

				// parse num to push
				if ( cli.hasOption(numInitCopiesOption) ){
					num_to_push = new SinglePusher( Integer.parseInt(cli.getOptionValue(numInitCopiesOption)) );
				} else if ( cli.hasOption(linearOption) ){
					num_to_push = new LinearPusher();
				} else if ( cli.hasOption(fastStartOption) ){
					num_to_push = new LinearSegmentPusher(0.25);
				} else if ( cli.hasOption(slowStartOption) ){
					num_to_push = new LinearSegmentPusher(-0.25);
				} else if ( cli.hasOption(hyperbolicOption) ){
					num_to_push = new HyperbolicPusher();
				} else if ( cli.hasOption(quadraticOption) ){
					num_to_push = new QuadraticPusher();
				} else if ( cli.hasOption(derivativeOption) ){
					num_to_push = new DerivativePusher((long)(windowSize*tps), clipping, derivInitialCopies);
				} else {
					num_to_push = new DefaultNumToPush();
				}
			}
		}

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
	protected void run() throws IOException, NoSuchTraceException,
			AlreadyExistsException, LoadTraceException {
		
		RNG.init(seed);
		
		// read the PRESENCE trace
		PresenceTrace presence = (PresenceTrace)orig_store.getTrace(graph_options.get(GraphOptions.PRESENCE));
		StatefulReader<PresenceEvent,Presence> presenceReader = presence.getReader();
		
		//Presence Bus
		Bus<PresenceEvent> presenceEventBus = new Bus<PresenceEvent>();
		Bus<Presence> presenceBus = new Bus<Presence>();
		
		presenceReader.setBus(presenceEventBus);
		presenceReader.setStateBus(presenceBus);
		
		//  read the LINKS trace
		LinkTrace links = (LinkTrace)orig_store.getTrace(graph_options.get(GraphOptions.LINKS));
		StatefulReader<LinkEvent,Link> linkReader = links.getReader();
		
		// LINKS BUS
		Bus<Link> linkBus = new Bus<Link>();
		Bus<LinkEvent> linkEventBus = new Bus<LinkEvent>();
		
		linkReader.setBus(linkEventBus);
		linkReader.setStateBus(linkBus);
		
		//RUNNER
		Runner runner = new Runner(presence.ticsPerSecond(), presence.minTime(), presence.maxTime());
		
		
		
		// add the generators for the traces
		runner.addGenerator(presenceReader);
		runner.addGenerator(linkReader);
		
		Bus<BufferEvent> bufferBus = new Bus<BufferEvent>();
		
		final World world = new World();
		
		int tps=1000;
		
		// Add AdHoc Router
		adhoc_bitrate /= 8*tps;
		AdHocRadio adhoc = new AdHocRadio(world, new ConstantBitrateGenerator(adhoc_bitrate));
		
		runner.addGenerator(adhoc);
		
		infra_up_bitrate /= 8*tps;
		infra_down_bitrate /= 8*tps;
		//add an INFRA RADIO
		
		msgPeriod *= tps;
		msgDelay *= tps;
		
		//panic_time = -1000; 
		
		panic_time*=tps;
		guard_time *= tps;
		send_incr *= tps;
		ctrl_incr *= tps;
		
		min_time = (min_time != null)? min_time * tps : presence.minTime();
		max_time = (max_time != null)? max_time * tps : presence.maxTime();
		
		
		PervasiveInfraRadio infra = new PervasiveInfraRadio(world, 
				new ConstantBitrateGenerator(infra_down_bitrate), 
				new ConstantBitrateGenerator(infra_up_bitrate), 
				root_id);
		runner.addGenerator(infra);

//		//TODO FIXED NUMBER OF PUSHED TO CHANGE IN REAL CASE
//		num_to_push=new SinglePusher(30);
//		who_to_push= new ConnectedComponentWho();
		
		// Infra Router
		final Router infra_router = (ds_oracle)? 
				new OracleInfraRouter(infra, panic_time, root_id, bufferSize, bufferBus) :
					new InfraRouter(infra, who_to_push, num_to_push, send_incr, panic_time, 
							guard_time, float_interval, root_id, bufferSize, bufferBus,-1);

				//new OracleInfraRouter(infra, panic_time, root_id, bufferSize, bufferBus);
				
		// DS INFRA Router
//		GroupTrace ds_trace = (GroupTrace)orig_store.getTrace(graph_options.get(GraphOptions.GROUPS));
//		StatefulReader<GroupEvent,Group> ds_reader = ds_trace.getReader(Integer.MAX_VALUE); // this trace must have lower priority than the message generator
//		Bus<Group> groupBus = new Bus<Group>();
//		Bus<GroupEvent> groupEventBus = new Bus<GroupEvent>();
//		ds_reader.setBus(groupEventBus);
//		ds_reader.setStateBus(groupBus);
//		groupBus.addListener(((GroupTrace.Handler)infra_router).groupListener());
//		groupEventBus.addListener(((GroupTrace.Handler)infra_router).groupEventListener());
//		runner.addGenerator(ds_reader);

		// link listener for ad hoc transmissions
		if (! infra_only){
			linkBus.addListener(adhoc.linkListener());
			linkEventBus.addListener(adhoc.linkEventListener());
		}
		
		if ( ds_oracle ){
			GroupTrace ds_trace = (GroupTrace)orig_store.getTrace(graph_options.get(GraphOptions.GROUPS));
			StatefulReader<GroupEvent,Group> ds_reader = ds_trace.getReader(Integer.MAX_VALUE); // this trace must have lower priority than the message generator
			Bus<Group> groupBus = new Bus<Group>();
			Bus<GroupEvent> groupEventBus = new Bus<GroupEvent>();
			ds_reader.setBus(groupEventBus);
			ds_reader.setStateBus(groupBus);
			groupBus.addListener(((GroupTrace.Handler)infra_router).groupListener());
			groupEventBus.addListener(((GroupTrace.Handler)infra_router).groupEventListener());
			runner.addGenerator(ds_reader);
		}

		//Connected Components Routers
		if (use_cc){
			NeighborMessageQueuer queuer = new NeighborMessageQueuer(world, ctrl_incr, root_id);
			linkBus.addListener(queuer.linkListener());
			linkEventBus.addListener(queuer.linkEventListener());
			presenceBus.addListener(queuer.presenceListener());
			presenceEventBus.addListener(queuer.presenceEventListener());
			runner.addGenerator(queuer);
			presenceBus.addListener(((ConnectedComponentWho)who_to_push).presenceListener());
			presenceEventBus.addListener(((ConnectedComponentWho)who_to_push).presenceEventListener());
			infra.addListener((ConnectedComponentWho)who_to_push);
		}
		else if ( who_to_push instanceof ArrivalWho ){
			presenceBus.addListener(((ArrivalWho)who_to_push).presenceListener());
			presenceEventBus.addListener(((ArrivalWho)who_to_push).presenceEventListener());
		}
		

		//add generator for infra router
		runner.addGenerator((Generator) infra_router);


		//add a Message Generator for broadcast messages it extends BUS so it behave as a BUS
		MessageGenerator msgGenerator = new MessageGenerator(msgPeriod,msgPeriod,msgDelay,msgDelay, 
				new MessageFactory<BroadcastMessage>(msgSize, msgSize){
			@Override
			public BroadcastMessage getNew(long creationTime, long expirationTime) {
				if(D) System.out.println("PntProvaScenario.Broadcastmessage.getNew: "+creationTime+";"+expirationTime);
				Set<Router> group = world.getRandomRoutersExceptInfra(1);
				return new BroadcastMessage(infra_router,group, nextBytes(), creationTime, expirationTime);
			}
		});


		msgGenerator.setTimeLimits(min_time, max_time);

		//add the generator for messages
		runner.addGenerator(msgGenerator);


		PntRouterFactory routerFactory = new PntRouterFactory(infra_router, infra, adhoc, bufferSize, bufferBus,null,ackSize,"ACCEPT_ALL");

		world.setRouterFactory(routerFactory);
		// add listeners to  the event we are interested into
		presenceBus.addListener(world.presenceListener());
		presenceEventBus.addListener(world.presenceEventListener());
		
		// important to add the infra listener after the world
		presenceEventBus.addListener(infra.presenceEventListener());
		presenceBus.addListener(infra.presenceListener());
		// important at add the infra_router listener after the world
		
		presenceBus.addListener(((PresenceTrace.Handler)infra_router).presenceListener());
		presenceEventBus.addListener(((PresenceTrace.Handler)infra_router).presenceEventListener());


		//		// ADD listener for reports
		MessageTrace messages = (MessageTrace)_store.newTrace("messages", MessageTrace.type, force);
		BufferTrace buffers = (BufferTrace)_store.newTrace("buffers", BufferTrace.type, force);
		
		TransferTrace adhoc_transfers = (TransferTrace)_store.newTrace("adhoc_transfers", TransferTrace.type, force);
		TransferTrace infra_transfers = (TransferTrace)_store.newTrace("infra_transfers", TransferTrace.type, force);
		MessageEventLogger msgLogger = new MessageEventLogger(messages.getWriter(links.snapshotInterval()));
		BufferEventLogger bufferLogger = new BufferEventLogger(buffers.getWriter(links.snapshotInterval()));
		TransferEventLogger adhocTransferLogger = new TransferEventLogger(adhoc_transfers.getWriter(links.snapshotInterval()));
		TransferEventLogger infraTransferLogger = new TransferEventLogger(infra_transfers.getWriter(links.snapshotInterval()));
		//		
		msgGenerator.addListener(msgLogger);
		bufferBus.addListener(bufferLogger);
		presenceBus.addListener(bufferLogger.presenceListener());
		presenceEventBus.addListener(bufferLogger.presenceEventListener());
		adhoc.addListener(adhocTransferLogger);
		infra.addListener(infraTransferLogger);


		msgGenerator.addListener(world);

		// run the simulation
		runner.run();


		//close loggers for report
		msgLogger.close();
		infraTransferLogger.close();
		adhocTransferLogger.close();
		bufferLogger.close();
		
	}
	
	@Override
	protected void close() throws IOException {
		super.close();
		
		orig_store.close();
		
	}
	
	
	public static void main(String[] args) throws IOException{
		App app = new PntProvaScenario();
		app.ready("", args);
		app.exec();
	}

}
