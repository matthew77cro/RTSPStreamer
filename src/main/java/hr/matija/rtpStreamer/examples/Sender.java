package hr.matija.rtpStreamer.examples;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;

import hr.matija.rtpStreamer.h264.H264FileLoader;
import hr.matija.rtpStreamer.rtp.RTPUtil;
import hr.matija.rtpStreamer.rtp.RTPUtil.RTPheader;
import hr.matija.rtpStreamer.rtp.RTPUtil.RTPpacket;

public class Sender {
	
	public static void main(String[] args) throws IOException {
		
		ServerSocket socket = new ServerSocket(25565);
		
		for(int i=0; i<10; i++) {
			Socket s = socket.accept();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(s.getInputStream())));
			String portS = reader.readLine();
			int port = Integer.parseInt(portS);
			
			DatagramSocket dSocket = new DatagramSocket();
			
			H264FileLoader loader = new H264FileLoader(Paths.get("C:\\Users\\Matija\\Desktop\\fpv.h264"));
			loader.nextNalUnit();
			loader.nextNalUnit();
			
			short seqNum = (short) (Math.random()*10_000);
			int timestamp = 0;
			while(true) {
				byte nalu[] = loader.getNalUnit().getData();
				boolean marker = !loader.nextNalUnit();
				
				RTPheader header = new RTPheader((byte) 2, false, false, (byte) 0, marker, (byte) 67, seqNum, timestamp, 0);
				RTPpacket rtp = new RTPpacket(header, nalu);
				byte UDPpayload[] = RTPUtil.createRTPpacket(rtp);
				
				DatagramPacket packet = new DatagramPacket(UDPpayload, UDPpayload.length, new InetSocketAddress(s.getInetAddress(), port));
				dSocket.send(packet);
				
				if(marker) break;
			}
			
			dSocket.close();
			reader.close();
			s.close();
		}
		
		socket.close();
	}

}
