package ditl.sim.pnt;

import ditl.*;
import ditl.Store.LoadTraceException;
import ditl.Store.NoSuchTraceException;
import ditl.WritableStore.AlreadyExistsException;
import ditl.cli.App;
import ditl.cli.WriteApp;
import ditl.graphs.*;
import ditl.graphs.cli.GraphOptions;
import ditl.sim.*;
import ditl.transfers.BufferTrace;
import ditl.transfers.MessageTrace;
import ditl.transfers.TransferTrace;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class MultiplePnT extends WriteApp{
	
	protected final static String bufferSizeOption = "buffer-size";
	protected final static String upBitrateOption = "up-bitrate";
	protected final static String downBitrateOption = "down-bitrate";
	protected final static String adhocBitrateOption = "adhoc-bitrate";
	protected final static String minMsgDelayOption = "min-message-delay";
	protected final static String maxMsgDelayOption = "max-message-delay";
	protected final static String minMsgPeriodOption = "min-message-period";
	protected final static String maxMsgPeriodOption = "max-message-period";
	protected final static String minTimeOption = "min-time";
	protected final static String maxTimeOption = "max-time";
	protected final static String minMsgSizeOption = "min-message-size";
	protected final static String maxMsgSizeOption = "maxmessage-size";
	protected final static String ackMsgSizeOption = "ack-size";
	protected final static String panicTimeOption = "panic-time";
	protected final static String guardTimeOption = "guard-time";
	protected final static String seedOption = "seed";
	protected final static String granularityOption ="tps";  
	protected final static String sendIncrOption = "send-incr";
	protected final static String nodePushOption= "node-push-id";
	protected final static String infraOnlyOption = "infra-only";


	protected final static String randomPushOption = "rand-push";
	protected final static String rankingListPushOption = "rank-list-push";
	protected final static String hyperbolicOption = "hyperbolic";
	protected final static String quadraticOption = "quadratic";
	protected final static String linearOption = "linear";
	protected final static String fastStartOption = "fast-start";
	protected final static String slowStartOption = "slow-start";

	protected final static String numInitCopiesOption = "n-init-copies";

	// derivative based retransmission
	protected final static String derivativeOption = "derivative";
	protected final static String windowSizeOption = "window-size";
	protected final static String clippingOption = "clipping";
	protected final static String initCopiesDerivativeOption = "init-copies-deriv";
	
	protected final static String reportFolderOption = "report-folder";
	protected final static String capInfraOption = "cap-infra";
	protected final static String sprayAndWaitOption = "spray-and-wait";
	protected final static String adhocStrategyOption = "adhoc-strategy";
	
	protected final static String dsOption = "dominating-set";
	protected final static String ccOracleOption = "cc-oracle";
	protected final static String ccOption = "cc";
	

	
	
	//report
	MessageReport msgReport;

	final static Integer root_id = -42;

	public final static String PKG_NAME = "pnt";
	public final static String CMD_NAME = "multiple-sim";
	public final static String CMD_ALIAS = null;
	protected static final boolean D = false;
	
	
	private String[] store_names;
	private Store orig_store;
	
	GraphOptions graph_options = new GraphOptions(GraphOptions.PRESENCE, GraphOptions.LINKS, GraphOptions.GROUPS, GraphOptions.MOVEMENT);
	
	private Long seed;
	private Long min_time, max_time;
	private double adhoc_bitrate;
	private double infra_up_bitrate;
	private double infra_down_bitrate;
	private int minMsgSize;
	private int maxMsgSize;
	private int ackSize;
	private long minMsgPeriod;
	private long maxMsgPeriod;
	private long minMsgDelay;
	private long maxMsgDelay;
	private int bufferSize;
	private long panic_time;
	private long guard_time;
	private Long float_interval;
	private long send_incr;
	private NumToPush num_to_push;
	private WhoToPush who_to_push;
	private int tps;
    private double windowSize;
	private double clipping;
	private String report_folder;
	private int capInfra;
	private Integer spray_and_wait=null;
	private int derivInitialCopies;
	private boolean infra_only = false;
	private double percentage;
	private String adhoc_strategy;
	private boolean ds_oracle = false;
	private boolean cc_oracle = false;
	private boolean use_cc = false;
	
	
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
		options.addOption(null,minMsgDelayOption,true,"Minimum allowed delay for messages (s)");
		options.addOption(null,maxMsgDelayOption,true,"Maximum allowed delay for messages (s)");
		options.addOption(null,minMsgPeriodOption,true,"Minimum message creation period (s)");
		options.addOption(null,maxMsgPeriodOption,true,"Maximum message creation period (s)");
		options.addOption(null,minMsgSizeOption,true,"Min Message size (bytes)");
		options.addOption(null,maxMsgSizeOption,true,"Max Message size (bytes)");
		options.addOption(null,ackMsgSizeOption,true,"Ack size (bytes)");
		options.addOption(null,adhocStrategyOption, true, "Adhoc diffusion strategy: choose between <"+AdHocRouter.ACCEPT_STRATEGY+">, <"+AdHocRouter.PRIORITY_STRATEGY+">, <"+AdHocRouter.SELFISH_STRATEGY+">");
		
		options.addOption(null,seedOption,true,"Seed for the random number generator");
		options.addOption(null,panicTimeOption,true,"Panic time");
		options.addOption(null,guardTimeOption,true,"Guard time");
		options.addOption(null,sendIncrOption,true,"Sending increment");
		options.addOption(null,minTimeOption,true,"Time at which the simulation start");
		options.addOption(null,maxTimeOption,true,"Time at which the simulation stop");
		options.addOption(null,granularityOption,true,"Time granularity for the simulation (tics per second)");
		options.addOption(null,reportFolderOption,true,"Name of the folder for reporting");
		options.addOption(null,capInfraOption,true,"Infrastructure message cap");
		options.addOption(null,sprayAndWaitOption,true,"Spray [arg] copies in ad hoc");
		options.addOption(null, infraOnlyOption, false, "Only use infra");
		options.addOption(null,dsOption,false,"Use dominating set oracle");
		options.addOption(null,ccOracleOption,false,"Use connected component oracle");
		
		
		
		OptionGroup whoGroup = new OptionGroup();
		whoGroup.setRequired(false);
		whoGroup.addOption(new Option(null,randomPushOption,false,"Push to random nodes"));
		whoGroup.addOption(new Option(null,rankingListPushOption,true,"Push to best ranked nodes, according to list <arg>"));
		whoGroup.addOption(new Option(null,ccOption,false,"Nodes also report their neighbors."));
		options.addOption(null,nodePushOption,true,"Push the content to node <arg>");
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
		numGroup.addOption(new Option(null,clippingOption,true,"Re inject at max [arg] % of missing messages "));
		numGroup.addOption(new Option(null,initCopiesDerivativeOption,true,"Initial copies sent when derivative reinjection"));
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
		minMsgSize = Integer.parseInt(cli.getOptionValue(minMsgSizeOption,"10240")); // default 10 Kbyte
		maxMsgSize = Integer.parseInt(cli.getOptionValue(maxMsgSizeOption,"102400")); // default 100 Kbyte
		ackSize = Integer.parseInt(cli.getOptionValue(ackMsgSizeOption,"256")); // default 256 Byte
		minMsgPeriod = Long.parseLong(cli.getOptionValue(minMsgPeriodOption,"30")); // default: 1 msg/30 sec
		maxMsgPeriod = Long.parseLong(cli.getOptionValue(maxMsgPeriodOption,"300")); // default: 1 msg/10 minutes
		minMsgDelay = Long.parseLong(cli.getOptionValue(minMsgDelayOption,"30")); // default: 10 minutes
		maxMsgDelay = Long.parseLong(cli.getOptionValue(maxMsgDelayOption,"300")); // default: 10 minutes

		if ( cli.hasOption(minTimeOption) )
			min_time = Long.parseLong(cli.getOptionValue(minTimeOption) );
		if ( cli.hasOption(maxTimeOption))
			max_time = Long.parseLong(cli.getOptionValue(maxTimeOption) );

		bufferSize = Integer.parseInt(cli.getOptionValue(bufferSizeOption,"104857600")); // default 100 Mbytes
		guard_time = Long.parseLong(cli.getOptionValue(guardTimeOption,"1")); // by default, pad with one second
		send_incr = Long.parseLong(cli.getOptionValue(sendIncrOption, "1")); // by default, try sending every second
		panic_time=Long.parseLong(cli.getOptionValue(panicTimeOption, "-1000")); // by default, never go in panic time;
		tps=Integer.parseInt(cli.getOptionValue(granularityOption,"1000")); // by default, ms
		
		adhoc_strategy=cli.getOptionValue(adhocStrategyOption,AdHocRouter.ACCEPT_STRATEGY);
		if (!(adhoc_strategy.equalsIgnoreCase(AdHocRouter.ACCEPT_STRATEGY) || adhoc_strategy.equalsIgnoreCase(AdHocRouter.PRIORITY_STRATEGY) 
				|| adhoc_strategy.equalsIgnoreCase(AdHocRouter.SELFISH_STRATEGY)) ){
			
			throw new IllegalArgumentException("Unknown parameter "+ adhoc_strategy);
		}
		
		report_folder=cli.getOptionValue(reportFolderOption);

		capInfra=Integer.parseInt(cli.getOptionValue(capInfraOption,"-1"));

		infra_only  = cli.hasOption(infraOnlyOption);
		if ( ! infra_only ){

			ds_oracle = cli.hasOption(dsOption);
			cc_oracle = cli.hasOption(ccOracleOption);
			if ( ! (ds_oracle  || cc_oracle)){
				
				if (cli.hasOption(sprayAndWaitOption))
					spray_and_wait=Integer.parseInt(cli.getOptionValue(sprayAndWaitOption));


				if (cli.hasOption(derivativeOption)){
					windowSize = Double.parseDouble(cli.getOptionValue(windowSizeOption, "5")); // by default, window size is 10 second
					clipping=Double.parseDouble(cli.getOptionValue(clippingOption, "0.05")); // by default, clipping at 5%;
					derivInitialCopies=Integer.parseInt(cli.getOptionValue(initCopiesDerivativeOption, "10"));
				}

				if ( cli.hasOption(randomPushOption) ){
					who_to_push = new RandomWho();
				}
				else if (cli.hasOption(nodePushOption)){
					who_to_push = new FixedWhoToPush(Integer.parseInt(cli.getOptionValue(nodePushOption, "1")));
				}else if ( cli.hasOption(ccOption) ){
					who_to_push = new ConnectedComponentWho();
					use_cc = true;
				}
				else if (cli.hasOption(rankingListPushOption)){
					who_to_push = new ListWho(cli.getOptionValue(rankingListPushOption));
				}
				else {
					who_to_push = new DefaultWhoToPush();
				}

				// parse num to push
				if ( cli.hasOption(numInitCopiesOption) ){
					num_to_push = new SinglePusher( Integer.parseInt(cli.getOptionValue(numInitCopiesOption, "1") ));
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
		//System.out.println(orig_store.);
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

		World world = new World();

		// Add AdHoc Router
		adhoc_bitrate /= 8*tps;
		AdHocRadio adhoc = new AdHocRadio(world, new ConstantBitrateGenerator(adhoc_bitrate));

		runner.addGenerator(adhoc);

		infra_up_bitrate /= 8*tps;
		infra_down_bitrate /= 8*tps;

		minMsgPeriod *= tps;
		maxMsgPeriod *= tps;
		maxMsgDelay *= tps;
		minMsgDelay *= tps;
		if ( infra_only )
			panic_time = Long.MAX_VALUE; // always panic!
		else
			panic_time*=tps;
		guard_time *= tps;
		send_incr *= tps;

		min_time = (min_time != null)? min_time * tps : presence.minTime();
		max_time = (max_time != null)? max_time * tps : presence.maxTime();


		PervasiveInfraRadio infra = new PervasiveInfraRadio(world, 
				new ConstantBitrateGenerator(infra_down_bitrate), 
				new ConstantBitrateGenerator(infra_up_bitrate), 
				root_id);
		runner.addGenerator(infra);


		 Router infra_router;
		// Infra Router
		if(ds_oracle)
			 infra_router = new OracleInfraRouter(infra, panic_time, root_id, bufferSize, bufferBus);
		else if(cc_oracle)
			 infra_router =new CcOracleInfraRouter(infra, panic_time, root_id, bufferSize, bufferBus);	  
		else
			 infra_router = new InfraRouter(infra, who_to_push, num_to_push, send_incr, panic_time, 
				guard_time, float_interval, root_id, bufferSize, bufferBus, capInfra);

		if ( ! infra_only ) {
			// link listener for ad hoc transmissions
			linkBus.addListener(adhoc.linkListener());
			linkEventBus.addListener(adhoc.linkEventListener());
			
			
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
//			else if ( use_cc ){
//				NeighborMessageQueuer queuer = new NeighborMessageQueuer(world, ctrl_incr, root_id);
//				linkBus.addListener(queuer.linkListener());
//				linkEventBus.addListener(queuer.linkEventListener());
//				presenceBus.addListener(queuer.presenceListener());
//				presenceEventBus.addListener(queuer.presenceEventListener());
//				runner.addGenerator(queuer);
//				presenceBus.addListener(((ConnectedComponentWho)who_to_push).presenceListener());
//				presenceEventBus.addListener(((ConnectedComponentWho)who_to_push).presenceEventListener());
//				infra.addListener((ConnectedComponentWho)who_to_push);
			
			if ( cc_oracle ){
				linkBus.addListener(((LinkTrace.Handler)infra_router).linkListener());
				linkEventBus.addListener(((LinkTrace.Handler)infra_router).linkEventListener());
				
			}
		}

		//add generator for infra router
		runner.addGenerator((Generator) infra_router);

		
		//world.setInterestProbability(percentage);
		
		//add a Message Generator for multicast messages it extends BUS so it behaves as a BUS
		MessageGenerator msgGenerator = new MessageGenerator(minMsgPeriod,maxMsgPeriod,minMsgDelay,maxMsgDelay, new MulticastMessage.Factory(world, minMsgSize, maxMsgSize,1));
//				new MessageFactory<MulticastMessage>(minMsgSize, maxMsgSize){
//		
//			@Override
//			public MulticastMessage getNew(long creationTime, long expirationTime) {
//				System.out.println("Multicastmessage.getNew");
//				Router from =world.getRandomRouter();
//				Set<Router> group = world.getRandomRouters(_p);
//				group.remove(from);
//				return new MulticastMessage(from, group, nextBytes(), creationTime, expirationTime);
//			}
//		});

		msgGenerator.setTimeLimits(min_time, max_time);

		//add the generator for messages
		runner.addGenerator(msgGenerator);

		PntRouterFactory routerFactory = new PntRouterFactory(infra_router, infra, adhoc, bufferSize, bufferBus,spray_and_wait,ackSize,adhoc_strategy);

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
		
//		if(cc_oracle){
//			presenceBus.addListener(((PresenceTrace.Handler)infra_router).presenceListener());
//			presenceEventBus.addListener(((PresenceTrace.Handler)infra_router).presenceEventListener());
//		}
			

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

		String numPushed="";
		if(num_to_push instanceof SinglePusher){
			numPushed="-init-copies-"+((SinglePusher)num_to_push).initCopies();
		}
		
		String derivSize="";
		if(num_to_push instanceof DerivativePusher){
			derivSize="-deriv-"+windowSize+"limit-"+clipping;
		}
		
		String SnWSize="";
		if(spray_and_wait!=null){
			SnWSize="-spray-"+spray_and_wait;
		}
		
		
		String delaySize="-delay-"+maxMsgDelay;
		
		
		String bufferS="-buffer_size-"+bufferSize;
		
		// name for the report file
		String filename=report_folder+"message-report"+delaySize+derivSize+numPushed+SnWSize+"-"+adhoc_strategy+bufferS+".txt";
		
		msgReport= new MessageReport(filename, ackSize,true, panic_time);
		//listener for reports
		msgGenerator.addListener(msgReport);
		adhoc.addListener(msgReport.transferEventListener());
		infra.addListener(msgReport.transferEventListener());
		presenceBus.addListener(msgReport.presenceListener());
		presenceEventBus.addListener(msgReport.presenceEventListener());
		
		
		// run the simulation
		runner.run();

		
		//close loggers for report
		msgLogger.close();
		infraTransferLogger.close();
		adhocTransferLogger.close();
		bufferLogger.close();
		
		msgReport.close();
		
		

		// filename for test(
		//String filename=report_folder+numPushed+"-delay-"+msgPeriod+"-msg-";

		//new InfectionDistributionReport(filename, min_time,max_time,messages, buffers, presence ).exec();

		//FileOutputStream file=new FileOutputStream(report_folder+"delay-matrix"+derivSize+numPushed+SnWSize+".txt", false);

		//new DelayReport(file, min_time,max_time,messages, buffers, presence ).exec();

		//filename=report_folder+"rx-delay-node-"+"random"+"-msg-";

		//new NodeReceptionDelayReport(filename, min_time,max_time,messages, buffers ,presence).exec();

		

		//new MessageReport(filename, presence, messages, infra_transfers, adhoc_transfers, ackSize, true).exec();
		
		//filename=report_folder+derivSize+numPushed+SnWSize+"adhoc-tx-report.txt";
		
		//new AdhocReport(filename, presence, messages, adhoc_transfers, numNodes, false).exec();
//		
//		filename=report_folder+"adhoc-rx-report.txt";
//		
//		new AdhocReport(filename, presence, messages, adhoc_transfers, numNodes, true).exec();
		
	}

	@Override
	protected void close() throws IOException {
		super.close();
		orig_store.close();
		
	}
	
	public static void main(String[] args) throws IOException{
		App app = new MultiplePnT();
		app.ready("", args);
		app.exec();
	}

}


