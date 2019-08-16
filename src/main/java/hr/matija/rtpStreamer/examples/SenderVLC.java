package hr.matija.rtpStreamer.examples;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Paths;

import hr.matija.rtpStreamer.h264.H264FileLoader;
import hr.matija.rtpStreamer.h264.NalUnit;
import hr.matija.rtpStreamer.h264.NalUnit.NalUnitType;
import hr.matija.rtpStreamer.rtp.RTPUtil;
import hr.matija.rtpStreamer.rtp.RTPUtil.RTPheader;
import hr.matija.rtpStreamer.rtp.RTPUtil.RTPpacket;

public class SenderVLC {
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		DatagramSocket dSocket = new DatagramSocket();
		
		H264FileLoader loader = new H264FileLoader(Paths.get("C:\\Users\\Matija\\Desktop\\fpv.h264"));
		loader.nextNalUnit();
		loader.nextNalUnit();
		
		short seqNum = (short) (Math.random()*10_000); // initial sequence number
		int timestamp = (int) (Math.random()*100_000); // initial timestamp
		int ssrc = (int) (Math.random()*100_000);      // ssrc
		
		boolean aud = false;
		
		long start = System.currentTimeMillis();
		while(true) {
			if(aud) {
				timestamp+=1500; //for 60 fps
				aud = false;
				if(!loader.nextNalUnit()) break;
				start = System.currentTimeMillis();
				continue;
			}
			NalUnit nalu = loader.getNalUnit();
			byte naluB[] = nalu.getData();
			boolean end = !loader.nextNalUnit();
			aud = end ? end : loader.getNalUnit().getType() == NalUnitType.ACCESS_UNIT_DELIMITER;
			
			RTPheader header = new RTPheader((byte) 2, false, false, (byte) 0, aud, (byte) 96, seqNum, timestamp, ssrc);
			RTPpacket rtp = new RTPpacket(header, naluB);
			byte UDPpayload[] = RTPUtil.createRTPpacket(rtp);
			
			DatagramPacket packet = new DatagramPacket(UDPpayload, UDPpayload.length, new InetSocketAddress(InetAddress.getByName("10.220.27.19"), 5004));
			dSocket.send(packet);
			
			System.out.println(naluB.length);
			
			if(end) break;
			seqNum++;
			
			if(aud) {
				long sleep = 16 - (System.currentTimeMillis() - start);
				Thread.sleep(sleep < 0 ? 0 : sleep);
			}
		}
		
		loader.close();
		dSocket.close();
		
	}

}
