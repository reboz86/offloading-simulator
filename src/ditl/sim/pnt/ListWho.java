package ditl.sim.pnt;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import ditl.sim.Message;
import ditl.sim.RNG;

public class ListWho extends DefaultWhoToPush{
	
	static final boolean D=false; 
	
	private String filename;
	private BufferedReader buf;
	private String[] ranked_nodes= new String[62];
	
	private Random rand;

	private Integer currentMessage=-1;
	SortedMap<Integer,Integer> rank= new TreeMap<Integer,Integer>();
	ValueComparator bvc =  new ValueComparator(rank);
	TreeMap<Integer,Integer> sorted_map=new TreeMap<Integer,Integer>(bvc);
	

	// read the ordered list of nodes to whom inject the content 
	public ListWho(String list_path){
		super();
		if(D) System.out.println(list_path);
		
		filename= list_path;

		try {
			buf=new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			System.err.append("File "+filename+" not found!!");
		}
		try {
			buf.readLine();
		} catch (IOException e) {
			System.err.append("Error while reading "+filename+"!!");
		}
		//
		//		ranked_nodes=nodeList.split("\t");

	}

	@Override
	public Integer whoToPush(Message msg, Set<Integer> infected, Set<Integer> sane){
		

		if (msg.msgId()!=currentMessage&& !sane.isEmpty()){
			currentMessage=msg.msgId();
			System.out.println("message: "+currentMessage);
			rank.clear();
			sorted_map.clear();

			String nodeList = null;
			try {
				buf.readLine();
				nodeList=buf.readLine();
				//System.out.println(nodeList);
			} catch (IOException e) {
				System.err.append("Error while reading "+filename+"!!");
			}

			if (nodeList==null){
				try {
					buf.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}

			ranked_nodes=nodeList.split("\t");
			//System.out.println(ranked_nodes.length);

			//for (int i=1;i<ranked_nodes.length;i++)
			//System.out.print(ranked_nodes[i]+"\t");
			//			System.out.println("length:"+ranked_nodes.length);
			//			for (int i=1;i<ranked_nodes.length;i++)
			//			System.out.print(ranked_nodes[i]+"\t");
			//			System.out.print("\n");
			
			for (int i=1; i<ranked_nodes.length;i++){
				rank.put(i,Integer.parseInt(ranked_nodes[i]));

			}
			
			sorted_map = new TreeMap<Integer,Integer>(bvc);
			sorted_map.putAll(rank);
			//System.out.println(sorted_map);
		}

		rand= new Random();
		
		//		if(rand.nextBoolean())
		//			return RNG.randomFromSet(sane);
		//		else		
		//System.out.println("Sending...");
		//System.out.println(node.toString());
		Entry<Integer, Integer> node=sorted_map.pollFirstEntry();
		if(node!=null){
			//System.out.println(node);
			if (sane.contains(node.getKey())){
				System.out.println("Pushing to "+node.getKey());
				return node.getKey();

			}
		}
		//System.out.println("Returning null");
		return null;


	}
	
	class ValueComparator implements Comparator<Integer> {

	    Map<Integer, Integer> base;
	    public ValueComparator(Map<Integer, Integer> base) {
	        this.base = base;
	    }

	    // Note: this comparator imposes orderings that are inconsistent with equals.    
	    public int compare(Integer a, Integer b) {
	        if (base.get(a) >= base.get(b)) {
	            return -1;
	        } else {
	            return 1;
	        } // returning 0 would merge keys
	    }
	}

}
