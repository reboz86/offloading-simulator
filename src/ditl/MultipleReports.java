package ditl;

import java.io.*;

/**a
 * A modified version of Report class that allows multiple reports at the same time
 * @author filippo
 *
 */
public class MultipleReports {

	public final static char commentChar = '#';

	private BufferedWriter writer;
	private OutputStream _out;

	public void finish() throws IOException {
		if (writer!=null)	writer.close();
	}

	public void append(Object line) throws IOException {
		if (writer!=null)
		writer.write(line+"\n");
	}

	public void appendComment(Object comment) throws IOException {
		if (writer!=null)
		writer.write(commentChar+" "+comment+"\n");
	}

	public void newReport(OutputStream out)throws IOException{
		finish();
		_out = out;
		writer = new BufferedWriter( new OutputStreamWriter(_out) );
	}
}



