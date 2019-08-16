package hr.matija.rtpStreamer.commandLine.commands;

import java.util.Map;

import hr.matija.rtpStreamer.commandLine.Command;
import hr.matija.rtpStreamer.commandLine.CommandResult;
import hr.matija.rtpStreamer.server.Server;

public class ManCommand implements Command {

	@Override
	public CommandResult execute(String[] command, Server server, Map<String, Command> allCommands) {
		if(command.length!=2) {
			System.out.println("Syntax error - wrong number of arguments");
			return CommandResult.CONTINUE;
		}
		
		Command c = allCommands.get(command[1]);
		if(c==null) {
			System.out.println("Command " + command[1] + "does not exist!");
			return CommandResult.CONTINUE;
		}
		
		System.out.println(c.getHelp());
		
		return CommandResult.CONTINUE;
	}

	@Override
	public String getHelp() {
		return String.format("man <commanName>%nPrints out help page for specified command%nE.g.man setsource");
	}

}
