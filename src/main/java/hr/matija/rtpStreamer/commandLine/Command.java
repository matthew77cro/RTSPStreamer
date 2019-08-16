package hr.matija.rtpStreamer.commandLine;

import java.util.Map;

import hr.matija.rtpStreamer.server.Server;

/**
 * Command that can be executed from the server command line
 * @author Matija
 *
 */
public interface Command {
	
	/**
	 * Executes the command
	 * @param command command which user typed in trimmed and split by spaces
	 * @param server server on which the command is beeing executed
	 * @param allCommands map containing all available commands - map : command_name -> command_object
	 * @return command result
	 * @throws Exception
	 */
	CommandResult execute(String command[], Server server, Map<String, Command> allCommands) throws Exception;
	
	/**
	 * Returns the man page for the command
	 * @return the man page for the command
	 */
	String getHelp();

}
