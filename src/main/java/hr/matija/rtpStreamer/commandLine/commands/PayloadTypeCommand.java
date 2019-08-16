package hr.matija.rtpStreamer.commandLine.commands;

import java.util.Map;

import hr.matija.rtpStreamer.commandLine.Command;
import hr.matija.rtpStreamer.commandLine.CommandResult;
import hr.matija.rtpStreamer.server.Server;

public class PayloadTypeCommand implements Command {

	@Override
	public CommandResult execute(String[] command, Server server, Map<String, Command> allCommands) throws Exception {
		if(command.length!=2) {
			System.out.println("Syntax error - wrong number of arguments");
			return CommandResult.CONTINUE;
		}
		
		byte pt;
		try {
			pt = Byte.parseByte(command[1]);
		} catch (NumberFormatException ex) {
			System.out.println("Syntax error - payload type must be an 8 bit number");
			return CommandResult.CONTINUE;
		}
		
		server.setPayloadType(pt);
		return CommandResult.CONTINUE;
	}

	@Override
	public String getHelp() {
		return String.format("setpt <payloadType>%nSets the payload type for the RTP packet.%nE.g. setpt 96");
	}

}
