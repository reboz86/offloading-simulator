package ditl.sim;

public class ConstantBitrateGenerator implements BitrateGenerator {

	private double _bitrate;
	
	public ConstantBitrateGenerator(double bitrate){
		_bitrate = bitrate;
	}
	
	@Override
	public double getNext() {
		return _bitrate;
	}

	@Override
	public double getAverage() {
		return _bitrate;
	}
}
