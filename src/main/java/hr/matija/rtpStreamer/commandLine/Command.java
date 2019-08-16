package hr.matija.rtpStreamer.commandLine;

import java.util.Map;

import hr.matija.rtpStreamer.server.Server;

public interface Command {
	
	CommandResult execute(String command[], Server server, Map<String, Command> allCommands) throws Exception;
	String getHelp();

}
