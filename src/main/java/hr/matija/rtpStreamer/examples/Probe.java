package hr.matija.rtpStreamer.examples;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;

import hr.matija.rtpStreamer.rtp.RTPUtil;

public class Probe {
	
	public static void main(String[] args) throws IOException {
		
		byte[] buffer = new byte[65536];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		DatagramSocket socket = new DatagramSocket(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 5004));
		
		for(int i=0; i<5; i++){
			socket.receive(packet);
			
			RTPUtil.RTPpacket rtpPacket = RTPUtil.parseBasicRTPpakcet(Arrays.copyOfRange(buffer, 0, packet.getLength()));
			RTPUtil.RTPheader header = rtpPacket.getHeader();
			
			System.out.println(header);
			
			for(int j=0; j<rtpPacket.getPayload().length; j++)
				System.out.print(Integer.toHexString(rtpPacket.getPayload()[j] & 0x000000FF) + " ");
			System.out.println();
			System.out.println("--------------------------" + rtpPacket.getPayload().length + "-----------------------------");
		}
		
		System.out.println("Finished");
		
		socket.close();
		
	}

}
