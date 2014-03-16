package ditl.sim.pnt;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ditl.sim.Message;

public class DerivativePusher extends DefaultNumToPush{

	private static final boolean D=false;
	
	private Set<Integer> _sent = new HashSet<Integer>();
	private int _initial;
	
	private long windowSize;
    private double clipping;
	
	private static Map<Long,Double> infectionMap= new TreeMap<Long,Double>(); 

	public DerivativePusher(long windowSize,  double clipping, int initCopies){
		System.out.println("Derivative Pusher:"+windowSize);
		this.windowSize=windowSize;
		
		//this.dLimit=dLimit;
		this._initial=initCopies;
		this.clipping=clipping;
	}

//	@Override
//	public int numToPush(Message msg, double curTime, int nInfected, int nTotal){
//		
//		if ( ! _sent.contains(msg.msgId()) ){ // not yet sent this message
//			_sent.add(msg.msgId());
//			if(D)System.out.println("numToPush:1");
//			return _initial;
//		}
//
//		if ( nTotal - nInfected < 1)
//			return 0;
//
//		// compute the  time elapsed for every message
//		long elapsed=(long) (curTime-msg.creationTime());
//
//		infection=(double)nInfected/nTotal;
//		// add the information on time and fraction of infected
//		infectionMap.put( elapsed, infection);
//
//		if(D) System.out.println("1curTime:"+curTime);
//		if(elapsed< delay && elapsed> windowSize+1){
//			 //System.out.println("elapsed="+elapsed);
//			derivative=computeWindowedDerivative(elapsed);
//			//if(D && msg.msgId()==0) System.out.println("derivative="+derivative);
//			// if the derivative is less than a certain value resend a packet
//			if (derivative <= dLimit && infection < 0.9 ){
//				
//				if(D)System.out.println("numToPush:1");
//				return 1;
//			}
//		}
//		else
//			return 0;
//		return 0;
//
//	}
	
	
	@Override
	public int numToPush(Message msg, double curTime, int nInfected, int nTotal){
		
		if ( ! _sent.contains(msg.msgId()) ){ // not yet sent this message
			_sent.add(msg.msgId());
			return nTotal/10; //first transmission 1/10 of total requests
		}

		if ( nTotal - nInfected < 1)
			return 0;
		
		// compute the  time elapsed for every message
		//long elapsed=(long) (curTime-msg.creationTime());

        double infection = (double) nInfected / nTotal;
//		// add the information on time and fraction of infected
		infectionMap.put( (long) (curTime-msg.creationTime()), infection);

		if(D) System.out.println("curTime:"+curTime);
		if(curTime< msg.expirationTime()){

            double derivative = computeWindowedDerivative(msg, curTime);
			
			// algorithm to re inject a number of msg
			
			double dLimit= (1- infection) /(msg.expirationTime()-curTime);
			
			//double clipping= 0.05; // 10% of missing
			
			return numberToReInject(dLimit, derivative, (nTotal-nInfected), clipping);
//			if (derivative <= dLimit && infection < 0.95 ){
//				
//				if(D)System.out.println("numToPush:1");
//				return 1;
//			}
		}
		
		
		return 0;
	}
	
	/**
	 * Compute the number of copies to re inject, depending on the distance from the target derivative and the number of missing infected nodes
	 * @param dLimit
	 * @param deriv
	 * @param missing
	 * @param clipping
	 * @return
	 */
	private int numberToReInject(double dLimit, double deriv, int missing, double clipping){
		int inject;
		if(deriv>=dLimit) 
			inject = 0;
		else if(deriv>0) 
			inject= (int) Math.ceil((clipping+deriv*(-clipping/dLimit))*missing);
		else 
			inject = (int) Math.ceil(clipping*missing);
		
		//System.out.println("deriv: "+deriv+" limit: "+dLimit+" missing: "+missing+" INJECTED: "+inject);
		return inject; 
	}

    private Double computeWindowedDerivative(Message msg, double curTime){

		if(D)System.out.println("elapsed = "+curTime);
		// only if current time is greater than the windowSize and is less than delay
		if(curTime< msg.expirationTime()){
            double finalValue;
            double startValue;
            if((curTime-msg.creationTime()) > windowSize){
				if(infectionMap.containsKey((long)(curTime-msg.creationTime())-windowSize))
					startValue =infectionMap.get((long)(curTime-msg.creationTime())-windowSize);
				else startValue =0;
				finalValue =infectionMap.get((long)(curTime-msg.creationTime()));


                //if(D ) System.out.println(curTime+"\t"+deriv);
				// compute the window derivative as the inclination of the segment connecting the startingpoint and the final point 
				return ((finalValue - startValue)/ windowSize);
			}
			else {
				startValue =0;
				finalValue =infectionMap.get((long)(curTime-msg.creationTime()));
                return ((finalValue - startValue)/ (curTime-msg.creationTime()));
			}
		}
		else return (double) 0;

	}


	

}
