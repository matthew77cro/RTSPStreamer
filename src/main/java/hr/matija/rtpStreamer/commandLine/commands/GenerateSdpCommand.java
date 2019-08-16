package hr.matija.rtpStreamer.commandLine.commands;

import java.io.IOException;
import java.util.Map;

import hr.matija.rtpStreamer.commandLine.Command;
import hr.matija.rtpStreamer.commandLine.CommandResult;
import hr.matija.rtpStreamer.server.Server;

public class GenerateSdpCommand implements Command {

	@Override
	public CommandResult execute(String[] command, Server server, Map<String, Command> allCommands) throws IOException {
		if(command.length!=2) {
			System.out.println("Syntax error - wrong number of arguments");
			return CommandResult.CONTINUE;
		}
		
		server.generateSdpFile(command[1]);
		return CommandResult.CONTINUE;
	}

	@Override
	public String getHelp() {
		return String.format("generate-sdo <fileName>%nGenerates the .sdp file to be opened in a VLC media player or similar players for excepting the stream.%nThe .sdp file contains information about the stream (host, port, encoding etc.).%nE.g. generate-sdp stream.sdp (will generate stream.sdp file in the server work directory)");
	}

}
