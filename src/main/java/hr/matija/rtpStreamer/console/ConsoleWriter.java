package hr.matija.rtpStreamer.console;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

public class ConsoleWriter {
	
	private List<OutputStream> outputStreams;
	
	public ConsoleWriter(List<OutputStream> outputStreams) {
		this.outputStreams = Objects.requireNonNull(outputStreams);
	}
	
	public void writeInfo(String info) {
		writeln("[INFO] " + info);
	}
	
	public void writeWarning(String warning) {
		writeln("[WARNING] " + warning);
	}

	public void writeError(String error) {
		writeln("[ERROR] " + error);
	}
	
	public List<OutputStream> getOutputStreams() {
		return outputStreams;
	}
		
	public void write(String msg) {
		byte[] m = msg.getBytes();
		for(var os : outputStreams) {
			try {
				os.write(m);
				os.flush();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}
	
	public void writeln(String msg) {
		msg = String.format("%s%n", msg);
		write(msg);
	}

}
