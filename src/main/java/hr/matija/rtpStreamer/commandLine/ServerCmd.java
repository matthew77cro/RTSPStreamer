package hr.matija.rtpStreamer.commandLine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import hr.matija.rtpStreamer.commandLine.commands.DestCommand;
import hr.matija.rtpStreamer.commandLine.commands.ExitCommand;
import hr.matija.rtpStreamer.commandLine.commands.GenerateSdpCommand;
import hr.matija.rtpStreamer.commandLine.commands.HelpCommand;
import hr.matija.rtpStreamer.commandLine.commands.InfoCommand;
import hr.matija.rtpStreamer.commandLine.commands.ManCommand;
import hr.matija.rtpStreamer.commandLine.commands.PayloadTypeCommand;
import hr.matija.rtpStreamer.commandLine.commands.RunningCommand;
import hr.matija.rtpStreamer.commandLine.commands.SetFPSCommand;
import hr.matija.rtpStreamer.commandLine.commands.SourceCommand;
import hr.matija.rtpStreamer.commandLine.commands.StopCommand;
import hr.matija.rtpStreamer.commandLine.commands.StreamCommand;
import hr.matija.rtpStreamer.server.Server;

public class ServerCmd {
	
	private Scanner sc;
	private Map<String, Command> commands;
	
	private Server server;
	
	public ServerCmd(Server server, Scanner sc) {
		this.server = server;
		
		this.sc = sc;
		commands = new HashMap<String, Command>();
		
		initCommands();
	}
	
	private void initCommands() {
		commands.put("exit", new ExitCommand());
		commands.put("stop", new StopCommand());
		commands.put("stream", new StreamCommand());
		commands.put("setfps", new SetFPSCommand());
		commands.put("setdest", new DestCommand());
		commands.put("generate-sdp", new GenerateSdpCommand());
		commands.put("setsource", new SourceCommand());
		commands.put("running", new RunningCommand());
		commands.put("info", new InfoCommand());
		commands.put("setpt", new PayloadTypeCommand());
		commands.put("help", new HelpCommand());
		commands.put("man", new ManCommand());
		
		commands = Collections.unmodifiableMap(commands);
	}

	public void start() {
		
		System.out.println("Welcome to h264 rtp server 1.0!");
		
		String input = null;
		CommandResult result = null;
		do {
			System.out.print("> ");
			input = sc.nextLine().trim();
			String splited[] = input.split("\\s+");
			
			Command nextCommand = commands.get(splited[0]);
			if(nextCommand==null) {
				System.out.println("Unknown command.");
				continue;
			}
			
			try {
				result = nextCommand.execute(splited, server, commands);
			} catch (Exception ex) {
				System.out.println("[EXCEPTION] " + ex.getCause() + " : " + ex.getMessage());
				continue;
			}
		} while(result!=CommandResult.EXIT);
		
		System.out.println("Goodbye!");
		
	}

}
