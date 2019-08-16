package hr.matija.rtpStreamer.examples;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import hr.matija.rtpStreamer.rtp.RTPUtil;
import hr.matija.rtpStreamer.rtp.RTPUtil.RTPpacket;

public class Receiver {
	
	public static void main(String[] args) throws UnknownHostException, IOException {
		
		Socket socket = new Socket(InetAddress.getByName("127.0.0.1"), 25565);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		
		byte buf[] = new byte[65536];
		DatagramSocket dSocket = new DatagramSocket(25566);
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		
		Path newFile = Paths.get("C:\\Users\\Matija\\Desktop\\recv.h264");
		if(Files.exists(newFile)) 
			Files.delete(newFile);
		BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(newFile, StandardOpenOption.CREATE));
		
		writer.write(String.format("25566%n"));
		writer.flush();
		
		while(true) {
			dSocket.receive(packet);
			
			System.out.println("Recv!");
			RTPpacket rtp = RTPUtil.parseBasicRTPpakcet(Arrays.copyOf(packet.getData(), packet.getLength()));
			bos.write(new byte[] {0, 0, 0, 1});
			bos.write(rtp.getPayload());
			
			if(rtp.getHeader().ismarker()) break;
		}
		
		System.out.println("Done");
		bos.close();
		dSocket.close();
		writer.close();
		socket.close();
		
	}

}
