package hr.matija.rtpStreamer.commandLine.commands;

import java.util.Map;

import hr.matija.rtpStreamer.commandLine.Command;
import hr.matija.rtpStreamer.commandLine.CommandResult;
import hr.matija.rtpStreamer.server.Server;

public class HelpCommand implements Command {

	@Override
	public CommandResult execute(String[] command, Server server, Map<String, Command> allCommands) {
		if(command.length!=1) {
			System.out.println("Syntax error - wrong number of arguments");
			return CommandResult.CONTINUE;
		}
		
		for(String c : allCommands.keySet()) {
			System.out.println(c);
		}
		
		return CommandResult.CONTINUE;
	}

	@Override
	public String getHelp() {
		return String.format("Prints out all available commands.");
	}

}
