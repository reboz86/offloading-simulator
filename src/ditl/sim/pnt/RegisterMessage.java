package ditl.sim.pnt;

import ditl.sim.*;

public class RegisterMessage extends ControlMessage {
	
	public static final int ENTER = 0;
	public static final int LEAVE = 1;
	
	private final int _type;
	
	public RegisterMessage(Router from, Router to, long creationTime, int type){
		super(from, to, creationTime);
		_type = type;
	}
	
	public int type(){
		return _type;
	}
	
	@Override
	public String toString(){
		switch(_type){
		case ENTER: return _from+" ENTERS";
		default: return _from+" LEAVES";
		}
	}
}
