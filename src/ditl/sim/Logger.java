package ditl.sim;

import java.io.IOException;

import ditl.Listener;

public interface Logger<T> extends Listener<T> {
	public void close() throws IOException;
}
