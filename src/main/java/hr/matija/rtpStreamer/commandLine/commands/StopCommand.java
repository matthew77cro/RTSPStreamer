package hr.matija.rtpStreamer.commandLine.commands;

import java.util.Map;

import hr.matija.rtpStreamer.commandLine.Command;
import hr.matija.rtpStreamer.commandLine.CommandResult;
import hr.matija.rtpStreamer.server.Server;

public class StopCommand implements Command {

	@Override
	public CommandResult execute(String command[], Server server, Map<String, Command> allCommands) {
		server.stopStream();
		return CommandResult.CONTINUE;
	}

	@Override
	public String getHelp() {
		return String.format("Stops currently running stream if it is running. Does nothing otherwise.");
	}

}
