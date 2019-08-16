package hr.matija.rtpStreamer.commandLine.commands;

import java.util.Map;

import hr.matija.rtpStreamer.commandLine.Command;
import hr.matija.rtpStreamer.commandLine.CommandResult;
import hr.matija.rtpStreamer.server.Server;

public class SourceCommand implements Command {

	@Override
	public CommandResult execute(String[] command, Server server, Map<String, Command> allCommands) {
		if(command.length!=2) {
			System.out.println("Syntax error - wrong number of arguments");
			return CommandResult.CONTINUE;
		}
		
		server.setSource(command[1]);
		
		return CommandResult.CONTINUE;
	}

	@Override
	public String getHelp() {
		return String.format("setsource <h264FileName>%nSets the source file of the stream (must be a raw h264 file in Annex B format!).%nE.g. setsource video.h264 (Sets the source to the video.h264 file from the server sources directory)");
	}

}
