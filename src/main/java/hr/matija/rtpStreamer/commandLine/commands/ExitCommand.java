package hr.matija.rtpStreamer.commandLine.commands;

import java.util.Map;

import hr.matija.rtpStreamer.commandLine.Command;
import hr.matija.rtpStreamer.commandLine.CommandResult;
import hr.matija.rtpStreamer.server.Server;

public class ExitCommand implements Command {

	@Override
	public CommandResult execute(String command[], Server server, Map<String, Command> allCommands) {
		server.stopStream();
		return CommandResult.EXIT;
	}

	@Override
	public String getHelp() {
		return String.format("Stops the stream an exits the application.");
	}

}
