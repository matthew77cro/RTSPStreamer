package hr.matija.rtpStreamer.server;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import hr.matija.rtpStreamer.h264.H264FileLoader;
import hr.matija.rtpStreamer.h264.NalUnit;
import hr.matija.rtpStreamer.h264.NalUnit.NalUnitType;
import hr.matija.rtpStreamer.rtp.RTPUtil;
import hr.matija.rtpStreamer.rtp.RTPUtil.RTPheader;
import hr.matija.rtpStreamer.rtp.RTPUtil.RTPpacket;

public class ServerImpl implements Server {
	
	private Path sdpDir;
	private Path sourceDir;
	
	private int fps;
	private String inetHostName;
	private int port;
	private InetSocketAddress address;
	private Path source;
	
	private byte payloadType = 96;
	
	private AtomicBoolean isStreaming;
	private AtomicBoolean stopReq;
	
	public ServerImpl(String configFilePath) throws IOException {
		isStreaming = new AtomicBoolean(false);
		stopReq = new AtomicBoolean(false);
		
		Properties prop = new Properties();
		InputStream in = Files.newInputStream(Paths.get(configFilePath));
		prop.load(in);
		
		sdpDir = Paths.get(prop.getProperty("server.sdpDirectory"));
		if(!Files.isDirectory(sdpDir))
			throw new RuntimeException("server.sdpDirectory must be a path to a directory!");
		
		sourceDir = Paths.get(prop.getProperty("server.sourceDirectory"));
		if(!Files.isDirectory(sourceDir))
			throw new RuntimeException("server.sourceDirectory must be a path to a directory!");
		
		payloadType = Byte.parseByte(prop.getProperty("server.initialPt"));
		
		setFPS(Integer.parseInt(prop.getProperty("server.initialFPS")));

		String src = prop.getProperty("server.initialSource").trim();
		setSource(src);
		
		String dst[] = prop.getProperty("server.initialDestination").trim().split(":");
		setDestination(dst[0], Integer.parseInt(dst[1]));
	}

	@Override
	public void setFPS(int fps) {
		if(isStreaming.get()) throw new RuntimeException("Cannot change parameters while the stream is running.");
		if(fps<=0) throw new IllegalArgumentException("fps must not be <= 0");
		this.fps = fps;
	}

	@Override
	public void setDestination(String inetHostName, int port) throws UnknownHostException {
		if(isStreaming.get()) throw new RuntimeException("Cannot change parameters while the stream is running.");
		if(port<0) throw new IllegalArgumentException("port must not be < 0");

		this.address = new InetSocketAddress(InetAddress.getByName(inetHostName), port);
		this.inetHostName = Objects.requireNonNull(inetHostName);
		this.port = port;
	}

	@Override
	public void generateSdpFile(String fileName) throws IOException {
		Path file = Paths.get(sdpDir.toString() + "/" + fileName);
		if(Files.exists(file)) {
			Files.delete(file);
		}
		
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream( Files.newOutputStream(file, StandardOpenOption.CREATE))));
		
		writer.write("c=IN IP4 " + inetHostName + "\n" +
		             "m=video " + port + " RTP/AVP " + payloadType + "\n" + 
				     "a=rtpmap:" + payloadType + " H264/90000"); 
		
		writer.close();
	}

	@Override
	public void setSource(String h264FileName) {
		if(isStreaming.get()) throw new RuntimeException("Cannot change parameters while the stream is running.");
		Path p = Paths.get(sourceDir.toString() + "/" + h264FileName);
		if(!Files.isRegularFile(p) || !Files.isReadable(p))
			throw new RuntimeException("Wrong file name (not a regular file or not readable) : " + p.toString());
		this.source = p;
	}

	@Override
	public synchronized void stream() {
		if(isStreaming.get()) throw new RuntimeException("Stream already running");
		if(source == null) throw new NullPointerException("Souce not set!");
		if(inetHostName==null) throw new NullPointerException("Destination not set!");
		if(fps==0) throw new IllegalArgumentException("FPS not set!");
		stopReq.set(false);
		isStreaming.set(true);
		
		new Thread(() -> {
			try {
				DatagramSocket dSocket = new DatagramSocket();
				
				H264FileLoader loader = new H264FileLoader(source);
				loader.nextNalUnit();                          // skip the first 0x 00 00 01 (nalu separator)
				loader.nextNalUnit();                          // load the first nalu
				
				short seqNum = (short) (Math.random()*10_000); // initial sequence number
				int timestamp = (int) (Math.random()*100_000); // initial timestamp
				int ssrc = (int) (Math.random()*100_000);      // ssrc
				
				boolean aud = false;                           // true if next nal unit is AUD nal unit; used for timestamp increment and setting the marker to 1
				
				long start = System.currentTimeMillis();       // indicates start of one access unit; used for synchronizing sending of one frame
				long timestampIncrement = 90_000/fps;          // 90kHz clock as required by the standard
				while(!stopReq.get()) {
					
					if(aud) {
						timestamp+=timestampIncrement;
						aud = false;
						if(!loader.nextNalUnit()) break;
						start = System.currentTimeMillis();
						continue;
					}
					
					NalUnit nalu = loader.getNalUnit();
					byte naluB[] = nalu.getData();
					boolean end = !loader.nextNalUnit();
					aud = end ? end : loader.getNalUnit().getType() == NalUnitType.ACCESS_UNIT_DELIMITER; // checking if next nalu is aud; 
					 																					  // loader.getNalUnit() is now the next nalu after one that is currently sending
					
					RTPheader header = new RTPheader((byte) 2, false, false, (byte) 0, aud, payloadType, seqNum, timestamp, ssrc);
					RTPpacket rtp = new RTPpacket(header, naluB);
					byte UDPpayload[] = RTPUtil.createRTPpacket(rtp);
					
					DatagramPacket packet = new DatagramPacket(UDPpayload, UDPpayload.length, address);
					dSocket.send(packet);
					
					if(end) break;
					seqNum++;
					if(aud) {
						long sleep = (1000/fps) - (System.currentTimeMillis() - start); //time between two frames - time passed for sending the frame
						Thread.sleep(sleep < 0 ? 0 : sleep); //if time passed for sending the frame is greater than time between two frames, then don't sleep
					}
				}
				
				loader.close();
				dSocket.close();
			} catch (Exception ex) {
				System.err.println("Unexpected exception " + ex.getCause() + " : " + ex.getMessage());
			} finally {
				isStreaming.set(false);
			}
		}).start();
	}

	@Override
	public boolean isStreaming() {
		return isStreaming.get();
	}

	@Override
	public synchronized void stopStream() {
		stopReq.set(true);
		while(isStreaming.get());
		System.out.println("Stream terminated!");
	}

	@Override
	public int getFPS() {
		return fps;
	}

	@Override
	public String getDestionation() {
		return inetHostName + ":" + port;
	}

	@Override
	public String getSource() {
		if(source==null) return null;
		return source.toString();
	}
	
	@Override
	public byte getPayloadType() {
		return payloadType;
	}
	
	@Override
	public void setPayloadType(byte payloadType) {
		if(isStreaming.get()) throw new RuntimeException("Cannot change parameters while the stream is running.");
		this.payloadType = payloadType;
	}

}
