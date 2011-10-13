package ditl.sim;

import java.util.Random;

public class RNG {

	private static Random rng;
	
	public static void init(long seed){
		RNG.rng = new Random(seed); 
	}
	
	public static Random getInstance(){
		return rng;
	}
}
