package hr.matija.rtpStreamer.commandLine.commands;

import java.util.Map;

import hr.matija.rtpStreamer.commandLine.Command;
import hr.matija.rtpStreamer.commandLine.CommandResult;
import hr.matija.rtpStreamer.server.Server;

public class InfoCommand implements Command {

	@Override
	public CommandResult execute(String[] command, Server server, Map<String, Command> allCommands) {
		if(command.length!=1) {
			System.out.println("Syntax error - wrong number of arguments");
			return CommandResult.CONTINUE;
		}
		
		System.out.println("Payload type : " + server.getPayloadType());
		System.out.println("FPS : " + server.getFPS());
		System.out.println("Source : " + server.getSource());
		System.out.println("Destination : " + server.getDestionation());
		System.out.println("Running : " + server.isStreaming());
		
		return CommandResult.CONTINUE;
	}

	@Override
	public String getHelp() {
		return String.format("Prints out information about the current state of the server.");
	}

}
