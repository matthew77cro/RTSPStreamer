package hr.matija.rtpStreamer.commandLine.commands;

import java.util.Map;

import hr.matija.rtpStreamer.commandLine.Command;
import hr.matija.rtpStreamer.commandLine.CommandResult;
import hr.matija.rtpStreamer.server.Server;

public class SetFPSCommand implements Command {

	@Override
	public CommandResult execute(String command[], Server server, Map<String, Command> allCommands) {
		if(command.length!=2) {
			System.out.println("Syntax error - wrong number of arguments");
			return CommandResult.CONTINUE;
		}
		
		int fps;
		
		try {
			fps = Integer.parseInt(command[1]);
		} catch (NumberFormatException ex) {
			System.out.println("Syntax error - fps is not an integer");
			return CommandResult.CONTINUE;
		}
		
		server.setFPS(fps);

		return CommandResult.CONTINUE;
	}

	@Override
	public String getHelp() {
		return String.format("setfps <FPS>%nSets the fps of the stream. Should be the same as encoded h264 video.%nE.g. setfps 60");
	}

}
