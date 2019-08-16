package hr.matija.rtpStreamer.commandLine.commands;

import java.util.Map;

import hr.matija.rtpStreamer.commandLine.Command;
import hr.matija.rtpStreamer.commandLine.CommandResult;
import hr.matija.rtpStreamer.server.Server;

public class StreamCommand implements Command {

	@Override
	public CommandResult execute(String command[], Server server, Map<String, Command> allCommands) {
		server.stream();
		return CommandResult.CONTINUE;
	}

	@Override
	public String getHelp() {
		return String.format("Starts a stream to a destination specified by the setdest command.");
	}

}
