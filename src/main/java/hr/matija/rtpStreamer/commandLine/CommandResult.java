package hr.matija.rtpStreamer.commandLine;

/**
 * Models a result of a console command.
 * @author Matija
 *
 */
public enum CommandResult {
	
	/**
	 * Continue asking user for the next input.
	 */
	CONTINUE,
	
	/**
	 * Exit the command line
	 */
	EXIT;

}
