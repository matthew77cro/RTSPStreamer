package hr.matija.rtpStreamer.main;

import java.io.IOException;

import javax.swing.SwingUtilities;

import hr.matija.rtpStreamer.server.H264RtspServer;

/**
 * Main class - the start point of the RTSP streamer application
 * @author Matija
 *
 */
public class Main {
	
	public static void main(String[] args) throws IOException {
		
		if(args.length!=1) {
			System.err.println("Expected exactly one argument! - Path to the config file");
			return;
		}

		H264RtspServer server = new H264RtspServer(args[0]);
		
		SwingUtilities.invokeLater(() -> {
			new ServerWindow((int)(ServerWindow.screenWidth/4), 
						     (int)(ServerWindow.screenHeight/4), 
						     "RtspServer", 
						     server).setVisible(true);
		});
	}

}
