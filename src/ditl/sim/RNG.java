package ditl.sim;

import java.util.*;

public class RNG {

	private static Random rng;
	
	public static void init(long seed){
		RNG.rng = new Random(seed); 
	}
	
	public static Random getInstance(){
		return rng;
	}
	
	public static<T> T randomFromSet(Collection<T> set){
		if ( rng == null )
			RNG.init(0);
		int k = rng.nextInt(set.size());
		int j=0;
		for ( T r : set ){
			if ( j >= k )
				return r;
			j++;
		}
		return null;
	}
	
	public static<T> Set<T> randomSubSet(Collection<T> set, double p){
		if ( rng == null )
			RNG.init(0);
		Set<T> group = new HashSet<T>();
		for ( T obj : set ){
			if ( rng.nextDouble() < p ) 
				group.add(obj);
		}
		return group;
	}
}
