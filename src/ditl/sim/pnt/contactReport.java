package ditl.sim.pnt;


import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import ditl.Bus;
import ditl.Generator;
import ditl.Listener;
import ditl.Report;
import ditl.Runner;
import ditl.StatefulReader;
import ditl.Store.LoadTraceException;
import ditl.Store.NoSuchTraceException;
import ditl.WritableStore.AlreadyExistsException;
import ditl.cli.App;
import ditl.cli.ExportApp;
import ditl.graphs.Link;
import ditl.graphs.LinkEvent;
import ditl.graphs.LinkTrace;
import ditl.graphs.cli.GraphOptions;


public class contactReport extends Report 
implements LinkTrace.Handler, Listener<Object>, Generator{

	private static ArrayList<Long>[][]intercontact = new ArrayList[62][62];
	private static long[][] meanInterContact= new long[62][62];
	private long[][] lastContact = new long[62][62];
	
	private Bus<Object> update_bus = new Bus<Object>();


	public contactReport(String out_file_name, String infile) throws IOException {
		super((out_file_name != null)? new FileOutputStream(out_file_name) : System.out);
		update_bus.addListener(this);
	}


	@Override
	public void incr(long dt) throws IOException {}

	@Override
	public void seek(long time) throws IOException {}


	@Override
	public Bus<?>[] busses() {
		return new Bus<?>[]{update_bus};
		
	}

	@Override
	public int priority() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void handle(long time, Collection<Object> events) throws IOException {			
	}

	@Override
	public Listener<Link> linkListener() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Listener<LinkEvent> linkEventListener() {
		return new Listener<LinkEvent>(){
			@Override
			public void handle(long time, Collection<LinkEvent> events) throws IOException {
				for (LinkEvent lev: events){

					if(lev.isUp()){// UP
						ArrayList<Long> inter=intercontact[lev.id1()-1][lev.id2()-1];
						if (inter==null)
							inter= new ArrayList<Long>();
						inter.add(time-lastContact[lev.id1()-1][lev.id2()-1]);
						intercontact[lev.id1()-1][lev.id2()-1]=inter;
						inter=intercontact[lev.id2()-1][lev.id1()-1];
						if (inter==null)
							inter= new ArrayList<Long>();
						inter.add(time-lastContact[lev.id2()-1][lev.id1()-1]);
						intercontact[lev.id2()-1][lev.id1()-1]=inter;
						//System.out.println(inter.toString());
					}
					else{//DOWN
						lastContact[lev.id1()-1][lev.id2()-1]=time;
						lastContact[lev.id2()-1][lev.id1()-1]=time;
						//System.out.println(time+":DOWN:"+lev.id1()+":"+lev.id2());
						
					}

				}
			}
		};


	}


	private  void calculateIntercontact(ArrayList<Long>[][]intercontact){
		for (int i=0;  i<=61; i++){
			for (int j=0;  j<=61; j++){
				meanInterContact[i][j]=calculateAverage(intercontact[i][j]);
				try {
					append(meanInterContact[i][j],"\t");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			try {
				append("");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			

		}
	}

	private  long calculateAverage(List <Long> marks) {
		long sum = 0;
		if(marks!=null) {
			marks.remove(0);
			for (long mark : marks) {
				sum += mark;
			}
			if(marks.size()!=0)
				return sum / marks.size();
			else return 0;
		}
		return sum;
	}

	private  double calculateExpectation(){
		double sum = 0;
		Set<Integer> subset= new HashSet<Integer>();
		subset.add(13);subset.add(3);subset.add(40);subset.add(45);subset.add(19);subset.add(14);
		Iterator<Integer> iter=subset.iterator();
		if(iter.hasNext()){
			int i=iter.next();
			for (int j=0; j<=61 ; j++)
			{
				if (j!=(i-1) && meanInterContact[i-1][j]>0){
					sum= (double) 1/meanInterContact[i-1][j];
				}
			}
		}
		
		System.out.println(1/sum);
		return (double)1/sum;
				
		
	}



	public static void main(String args[]) throws IOException{
		App app = new ExportApp(){

			GraphOptions graph_options = new GraphOptions(GraphOptions.LINKS);
			long min_time=1561617;
			long max_time=1780000;


			@Override
			protected void initOptions() {
				super.initOptions();
				graph_options.setOptions(options);
			}

			@Override
			protected void run() throws IOException, NoSuchTraceException,
			AlreadyExistsException, LoadTraceException {

				
				LinkTrace links = (LinkTrace)_store.getTrace(graph_options.get(GraphOptions.LINKS));
				StatefulReader<LinkEvent,Link> linkReader = links.getReader();

				
				
				contactReport r= new contactReport("intercontactMatrix","output.jar");
				
				
				// LINKS BUS
				Bus<Link> linkBus = new Bus<Link>();
				linkBus.addListener(r.linkListener());
				Bus<LinkEvent> linkEventBus = new Bus<LinkEvent>();
				linkEventBus.addListener(r.linkEventListener());
				linkReader.setBus(linkEventBus);
				linkReader.setStateBus(linkBus);

				Runner runner = new Runner(1000, min_time, max_time);
				runner.addGenerator(linkReader);
				runner.addGenerator(r);
				runner.run();
				
				
				r.calculateIntercontact(intercontact);
				r.calculateExpectation();

				r.finish();
				linkReader.close();
				
			}
			
			@Override
			protected String getUsageString(){
				return "[OPTIONS] STORE";
			}

			@Override
			protected void parseArgs(CommandLine cli, String[] args)
					throws ParseException, ArrayIndexOutOfBoundsException,
					HelpException {
				super.parseArgs(cli, args);
				graph_options.parse(cli);
			}

		};
		app.ready("", args);
		app.exec();
	}

}
