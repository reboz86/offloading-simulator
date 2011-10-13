package ditl.sim.pnt;

import ditl.graphs.Point;
import ditl.sim.Router;

public class GPSMessage extends ControlMessage {
	
	private final Point _pos;
	
	public GPSMessage(Router from, Router to, long creationTime, Point pos){
		super(from, to, creationTime);
		_pos = pos;
	}
	
	public Point position(){
		return _pos;
	}
	
	@Override
	public String toString(){
		return "GPS from "+_from+" ("+_pos.x+","+_pos.y+")";
	}
}
