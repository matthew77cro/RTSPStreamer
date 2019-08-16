package hr.matija.rtpStreamer.commandLine.commands;

import java.util.Map;

import hr.matija.rtpStreamer.commandLine.Command;
import hr.matija.rtpStreamer.commandLine.CommandResult;
import hr.matija.rtpStreamer.server.Server;

public class DestCommand implements Command {

	@Override
	public CommandResult execute(String[] command, Server server, Map<String, Command> allCommands) {
		if(command.length!=3) {
			System.out.println("Syntax error - wrong number of arguments");
			return CommandResult.CONTINUE;
		}
		
		String host = command[1];
		int port;
		
		try {
			port = Integer.parseInt(command[2]);
		} catch (NumberFormatException ex) {
			System.out.println("Syntax error - port is not an integer");
			return CommandResult.CONTINUE;
		}
		
		try {
			server.setDestination(host, port);
		} catch (Exception ex) {
			System.out.println(ex.getCause() + " : " + ex.getMessage());
		}

		return CommandResult.CONTINUE;
	}

	@Override
	public String getHelp() {
		return String.format("setdest <inetHostName> <port>%nSets the destination of the stream (inetHostName + port).%nE.g. setdest 127.0.0.1 5004");
	}

}
