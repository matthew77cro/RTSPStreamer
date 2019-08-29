package hr.matija.rtpStreamer.main;

import java.io.IOException;

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
		server.start();
		
	}

}
