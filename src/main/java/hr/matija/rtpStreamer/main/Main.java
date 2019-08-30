package hr.matija.rtpStreamer.main;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import hr.matija.rtpStreamer.console.ConsoleWriter;
import hr.matija.rtpStreamer.server.H264RtspServer;

/**
 * Main class - the start point of the RTSP streamer application
 * @author Matija
 *
 */
public class Main {
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		if(args.length!=1) {
			System.err.println("Expected exactly one argument! - Path to the config file");
			return;
		}

		List<OutputStream> oss = new ArrayList<OutputStream>();
		ConsoleWriter writer = new ConsoleWriter(oss);
		H264RtspServer server = new H264RtspServer(args[0], writer);
		ServerWindow window = new ServerWindow((int)(ServerWindow.screenWidth/2), 
			     (int)(ServerWindow.screenHeight/2), 
			     "RtspServer", 
			     server);		
		oss.add(window.new JTextAreaOutputStream());		
		window.setVisible(true);
		Thread.sleep(1000);
		server.printInfo();
	}

}
