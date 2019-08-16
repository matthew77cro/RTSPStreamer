package hr.matija.rtpStreamer.main;

import java.io.IOException;
import java.util.Scanner;

import hr.matija.rtpStreamer.commandLine.ServerCmd;
import hr.matija.rtpStreamer.server.ServerImpl;

public class Main {
	
	public static void main(String[] args) {
		
		if(args.length!=1) {
			System.err.println("Expected exactly one argument! - Path to the config file");
			return;
		}
		
		Scanner sc = new Scanner(System.in);
		try {
			new ServerCmd(new ServerImpl(args[0]), sc).start();
		} catch (IOException e) {
			System.err.println(e.getCause() + " : " + e.getMessage());
		}
		sc.close();
		
	}

}
