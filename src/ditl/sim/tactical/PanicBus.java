package ditl.sim.tactical;

import java.io.IOException;

import ditl.*;

public class PanicBus extends Bus<PanicEvent> implements Generator {

	private long cur_time;
	
	@Override
	public Bus<?>[] busses() {
		return new Bus<?>[]{this};
	}

	@Override
	public int priority() {
		return Trace.defaultPriority;
	}

	@Override
	public void incr(long time) throws IOException {
		flush(cur_time);
		cur_time += time;
	}

	@Override
	public void seek(long time) throws IOException {
		cur_time = time;
	}

}
