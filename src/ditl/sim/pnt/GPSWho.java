package ditl.sim.pnt;

import java.util.*;

import ditl.Listener;
import ditl.graphs.*;
import ditl.sim.*;

public class GPSWho extends DefaultWhoToPush 
	implements Listener<TransferEvent>, PresenceTrace.Handler {
	
	public static double G = 1.0;
	public static int borderWeight = 1;
	
	protected Map<Integer,Point> _lastPositions = new HashMap<Integer,Point>();
	protected Map<Integer,Zone> _zoneMap = new HashMap<Integer,Zone>();
	protected Zone _mainZone;
	private int _thresh = 2; // do not create mainZone before we have _thresh number of nodes in system
	private double _padding = 10.0; // padding around max values for main zone 
	private double _minX = Double.MAX_VALUE;
	private double _maxX = Double.MIN_VALUE;
	private double _minY = Double.MAX_VALUE;
	private double _maxY = Double.MIN_VALUE;
	
	public static int MAX_RECURSION = 10;
	
	protected double dist2(Point c1, Point c2){
		return (c1.x-c2.x)*(c1.x-c2.x)+(c1.y-c2.y)*(c1.y-c2.y);
	}
	
	private void updateBounds(Point c){
		boolean updated = false;
		if ( c.x < _minX ){
			_minX = c.x;
			updated = true;
		}
		if ( c.x > _maxX ){
			_maxX = c.x;
			updated = true;
		}
		if ( c.y < _minY ){
			_minY = c.y;
			updated = true;
		}
		if ( c.y > _maxY ){
			_maxY = c.y;
			updated = true;
		}
		if ( updated ){
			if ( _minX != _maxX && _minY != _maxY ){
				reCalcBHTree();
			}
		}
	}
	
	protected void reCalcBHTree(){
		_mainZone = new Zone(_minX - _padding, _maxX + _padding, 
				_minY - _padding, _maxY + _padding, null, 0);
		
		if ( _lastPositions.size() >= _thresh ){
			for ( Map.Entry<Integer, Point> e : _lastPositions.entrySet() ){
				_mainZone.addNode(e.getKey(), e.getValue());
			}
		}
	}
	
	public Map<Integer,Zone> getZoneMap(){
		return _zoneMap;
	}
	
	protected void addNode(Integer i){
		// do nothing
	}
	
	protected void removeNode(Integer i){
		_lastPositions.remove(i);
		if ( _mainZone != null && _zoneMap.containsKey(i) ){
			Zone zone = _zoneMap.get(i);
			if ( zone._coords != null ){ // not a singleton
				zone._coords.remove(i);
				if ( zone._coords.size() == 1 ){ // make the set leaf into a singleton leaf
					for( Map.Entry<Integer, Point> e : zone._coords.entrySet() ){
						zone._node = e.getKey();
						zone._coord = e.getValue();
					}
					zone._coords = null;
				}
			} else { // we are a singleton leaf => remove it
				removeZone( _zoneMap.get(i) );
			}	
		}
	}
	
	
	protected void updateNode(Integer i, Point c){
		_lastPositions.put(i, c);
		updateBounds(c);
		if ( _mainZone != null ){
			if ( _zoneMap.containsKey(i) ){ // we are already in GPS data 
				Zone z = _zoneMap.get(i); // get previous zone
				if( ! z.inZone(c) ){ // we have changed zones!
					removeZone(z);
					_mainZone.addNode(i, c);
				}
			} else {
				_mainZone.addNode(i, c);
			}
		}
	}	
	
	protected void removeZone(Zone z){
		_zoneMap.remove(z._node);
		Zone parent = z._parent;
		Zone child = z;
		if ( parent == null)
			return;
		parent.removeChild(child);
		while ( parent != null && parent.shouldMerge() ){
			parent.absorbChild();
			parent = parent._parent;
		}
	}
	
	protected Point getLastPosition(Integer i){
		return _lastPositions.get(i);
	}
	
	public class Zone {
		protected Zone _NW, _NE, _SE, _SW;
		protected Map<Integer,Point> _coords;
		protected int _nInfected;
		protected Integer _node;
		protected Point _coord;
		protected double _area;
		protected double _minX, _maxX;
		protected double _minY, _maxY;
		protected Zone _parent;
		protected boolean _isLeaf;
		protected int _depth;
		
		public Zone(double minX, double maxX, double minY, double maxY, Zone parent, int depth ){
			_minX = minX; _maxX = maxX; _minY = minY; _maxY = maxY;
			_area = (_maxX-_minX) * (_maxY - _minY);
			_coords = null;
			_coord = null;
			_parent = parent;
			_isLeaf = true;
			_depth = depth;
		}
		
		public void addNode(Integer i, Point c){
			if ( _isLeaf && _node == null && _coords == null){ // we are an empty leaf -> ok
				_coord = c;
				_node = i;
				_zoneMap.put(i, this);
			} else if ( _isLeaf ){ // we are a leaf. Become an intermediate node  if our depth is less then MAX_RECURSION
				if ( _depth < MAX_RECURSION ){
					getZone(c).addNode(i,c);
					getZone(_coord).addNode(_node,_coord);
					_isLeaf = false;
					_node = null;
					_coord = null;
					assert( _coords == null );
				} else {
					if ( _coords == null ){ // singleton, become a "set leaf"
						_coords = new HashMap<Integer,Point>();
						_coords.put(_node, _coord);
						_node = null;
						_coord = null;
					}
					_coords.put(i, c);
					_zoneMap.put(i, this);
				}
			} else {
				getZone(c).addNode(i,c);
			}
		}
		
		public void removeChild(Zone zone){
			if ( _NW == zone ){
				_NW = null;
			} else if ( _NE == zone ){
				_NE = null;
			} else if ( _SE == zone ){
				_SE = null;
			} else {
				_SW = null;
			}
		}
		
		public boolean shouldMerge(){ 
			int c = 0;
			if ( _NW != null)
				++c;
			if ( _NE != null )
				++c;
			if ( _SE != null )
				++c;
			if ( _SW != null )
				++c;
			if ( c == 1)
				return true;
			return false;
		}
		
		public void absorbChild(){
			if ( _NW != null ){
				_node = _NW._node;
				_coord = _NW._coord;
				_NW = null;
			} else if ( _NE != null ){
				_node = _NE._node;
				_coord = _NE._coord;
				_NE = null;
			} else if ( _SE != null ){
				_node = _SE._node;
				_coord = _SE._coord;
				_SE = null;
			} else {
				_node = _SW._node;
				_coord = _SW._coord;
				_SW = null;
			}
			_zoneMap.put(_node, this);
			_isLeaf = true;
			assert(_coords == null);
		}
		
		public boolean inZone(Point c){ // included borders are maxX in minY
			return (( _minX < c.x && c.x <= _maxX ) 
					&& ( _minY <= c.y && c.y < _maxY ));
		}
		
		public Zone getZone(Point c){
			double mx = (_maxX + _minX)/2;
			double my = (_maxY + _minY)/2;
			if ( c.x <= mx ){ // we are west
				if ( c.y >= my ){ // we are north
					if ( _NW == null )
						_NW = new Zone( _minX, mx, my, _maxY, this, _depth+1 );
					return _NW;
				} else { // south
					if ( _SW == null)
						_SW = new Zone( _minX, mx , _minY, my, this, _depth+1);
					return _SW;
				}
			} else { // we are east
				if ( c.y >= my ){ // we are north
					if ( _NE == null )
						_NE = new Zone( mx, _maxX, my, _maxY, this, _depth+1 );
					return _NE;
				} else { // south
					if ( _SE == null )
						_SE = new Zone( mx, _maxX, _minY, my, this, _depth+1);
					return _SE;
				}
			}
		}
		
		public Integer node(){
			return _node;
		}
		
		public Point coord(){
			return _coord;
		}
		
		public Zone parent(){
			return _parent;
		}
		
		public double area(){
			return _area;
		}
		
		public void checkSanity(){
			if ( _isLeaf ){
				if ( ! ( (_node == null && _coord == null && _coords != null )
						|| (_node != null && _coord != null && _coords == null )) )
							throw new Error( "Invalid Leaf");
			} else { 
				if ( ! (_node == null && _coord == null && _coords == null ) )
					throw new Error("Invalid Intermediate node");
			}
		}
		
		public Point center(){
			return new Point( (_maxX-_minX)/2.0 , (_maxY-_minY)/2.0);
		}
		
	}
	
	protected Point barycenter(List<Point> coords, List<Integer> weights){
		double x = 0;
		double y = 0;
		int total = 0;
		for (int i=0; i<coords.size(); ++i){
			x += coords.get(i).x*weights.get(i);
			y += coords.get(i).y*weights.get(i);
			total += weights.get(i);
		}
		return new Point(x/(double)total, y/(double)total);
	}
	
	protected void setBarycenter(Zone zone, Set<Integer> infected){
		if ( zone._isLeaf ){
			if ( zone._coords == null ){ // singleton leaf
				if ( zone._coord == null ){
					// System.err.println("Invalid Leaf during barycenter update");
					zone._nInfected = 0;
				} else {
					assert( zone._coord != null );
					if ( infected.contains(zone._node) ){
						zone._nInfected = 1;
					} else {
						zone._nInfected = 0;
					}
				}
			} else { // just take center of zone instead of its proper barycenter
				zone._coord = zone.center();
				zone._nInfected = 0;
				List<Integer> infectedInThisZone = new LinkedList<Integer>();
				for ( Integer i : infected ){
					if  ( zone._coords.containsKey(i) ){
						infectedInThisZone.add(i);
					}
				}
				zone._nInfected = infectedInThisZone.size();
				infected.removeAll(infectedInThisZone);
			}
		} else { // not a leaf, recurse over subzones
			List<Point> subCoords = new LinkedList<Point>();
			List<Integer> weights = new LinkedList<Integer>();
			zone._nInfected = 0;
			if ( zone._NW != null ){
				setBarycenter(zone._NW, infected);
				if ( zone._NW._nInfected > 0 ){
					assert(zone._NW._coord != null);
					subCoords.add(zone._NW._coord);
					weights.add(zone._NW._nInfected);
					zone._nInfected += zone._NW._nInfected;
				}
			}
			if ( zone._NE != null ){
				setBarycenter(zone._NE, infected);
				if ( zone._NE._nInfected > 0 ){
					assert(zone._NE._coord != null);
					subCoords.add(zone._NE._coord);
					weights.add(zone._NE._nInfected);
					zone._nInfected += zone._NE._nInfected;
				}
			}
			if ( zone._SE != null ){
				setBarycenter(zone._SE, infected);
				if ( zone._SE._nInfected > 0 ){
					assert(zone._SE._coord != null);
					subCoords.add(zone._SE._coord);
					weights.add(zone._SE._nInfected);
					zone._nInfected += zone._SE._nInfected;
				}
			}
			if ( zone._SW != null ){
				setBarycenter(zone._SW, infected);
				if ( zone._SW._nInfected > 0 ){
					assert(zone._SW._coord != null);
					subCoords.add(zone._SW._coord);
					weights.add(zone._SW._nInfected);
					zone._nInfected += zone._SW._nInfected;
				}
			}
			if ( zone._nInfected > 0 ){
				zone._coord = barycenter(subCoords, weights);
			}
		}
	}
	
	protected double getGravity(Point target, Point source, int weight){
		assert ( weight > 0);
		double d = Math.sqrt(dist2(target,source));
		assert( ! Double.isNaN(d) );
		return (G*(double)weight)/(d+0.1);
	}
	
	protected double getInvGravityPotential(Point c){
		assert(c != null);
		double V = 0.0;
		// mainzone 
		V += recGetInvGravityPotential(_mainZone, c);
		assert( ! Double.isNaN(V) );
		assert ( V >= 0);
		// borders
		V += getGravity(c, new Point(c.x,_mainZone._maxY), borderWeight ); // north
		V += getGravity(c, new Point(c.x,_mainZone._minY), borderWeight ); // south
		V += getGravity(c, new Point(_mainZone._minX,c.y), borderWeight ); // west
		V += getGravity(c, new Point(_mainZone._maxX,c.y), borderWeight ); // east
		return 1.0/V;
	}
	
	private double recGetInvGravityPotential(Zone zone, Point c){
		double V = 0.0;
		if ( zone._isLeaf && zone._nInfected > 0){
			V += getGravity(c,zone._coord,zone._nInfected);
		} else {
			if ( zone._NW != null && zone._NW._nInfected > 0){
				if ( zone._NW.inZone(c) )
					V += recGetInvGravityPotential(zone._NW, c);
				else
					V += getGravity(c,zone._NW._coord,zone._NW._nInfected);
			}
			if ( zone._NE != null && zone._NE._nInfected > 0){
				if ( zone._NE.inZone(c) )
					V += recGetInvGravityPotential(zone._NE, c);
				else
					V += getGravity(c,zone._NE._coord,zone._NE._nInfected);
			}
			if ( zone._SE != null && zone._SE._nInfected > 0 ){
				if ( zone._SE.inZone(c) )
					V += recGetInvGravityPotential(zone._SE, c);
				else
					V += getGravity(c,zone._SE._coord,zone._SE._nInfected);
			}
			if ( zone._SW != null && zone._SW._nInfected > 0){
				if ( zone._SW.inZone(c) )
					V += recGetInvGravityPotential(zone._SW, c);
				else
					V += getGravity(c,zone._SW._coord,zone._SW._nInfected);
			}
		}
		return V;
	}

	@Override
	public void handle(long time, Collection<TransferEvent> events) {
		for ( TransferEvent tev : events ){
			Transfer transfer = tev.transfer();
			Message msg = transfer.message();
			if ( msg instanceof GPSMessage ){
				Integer from_id = msg.from().id();
				Point cur_pos = ((GPSMessage)msg).position();
				updateNode(from_id, cur_pos);
			}
		}
	}

	@Override
	public Listener<PresenceEvent> presenceEventListener() {
		return new Listener<PresenceEvent>(){
			@Override
			public void handle(long time, Collection<PresenceEvent> events) {
				for ( PresenceEvent pev : events ){
					if ( pev.isIn() )
						addNode(pev.id());
					else
						removeNode(pev.id());
				}
			}
		};
	}

	@Override
	public Listener<Presence> presenceListener() {
		return new Listener<Presence>(){
			@Override
			public void handle(long time, Collection<Presence> events){
				for ( Presence p : events )
					addNode(p.id());
			}
		};
	}
}
