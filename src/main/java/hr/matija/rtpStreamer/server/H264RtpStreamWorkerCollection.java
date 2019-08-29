package hr.matija.rtpStreamer.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import hr.matija.rtpStreamer.h264.H264FileLoader;
import hr.matija.rtpStreamer.h264.NalUnit;
import hr.matija.rtpStreamer.h264.NalUnit.NalUnitType;
import hr.matija.rtpStreamer.rtp.RTPUtil;
import hr.matija.rtpStreamer.rtp.RTPUtil.RTPheader;
import hr.matija.rtpStreamer.rtp.RTPUtil.RTPpacket;
import hr.matija.rtpStreamer.server.H264RtspResources.Resource;

public class H264RtpStreamWorkerCollection {
	
	private long clockHz = 90_000;
	private byte payloadType = 96;
	
	private Map<Integer, H264RtpStreamWorker> workers = new HashMap<>();
	
	public H264RtpStreamWorkerCollection() {
	}

	public H264RtpStreamWorkerCollection(long clockHz, byte payloadType) {
		this.clockHz = clockHz;
		this.payloadType = payloadType;
	}
	
	public long getClockHz() {
		return clockHz;
	}
	
	public byte getPayloadType() {
		return payloadType;
	}
	
	public synchronized int makeWorker(Resource resource, SocketAddress address, short initSeqNum, int initTimestamp, int ssrc) {
		int id;
		do {
			id = (int) (Math.random() * Integer.MAX_VALUE);
		} while(workers.keySet().contains(id));
		
		var worker = new H264RtpStreamWorker(id, resource, address, initSeqNum, initTimestamp, ssrc, clockHz, payloadType);
		workers.put(id, worker);
		return id;
	}
	
	public synchronized H264RtpStreamWorker getWorker(int id) {
		return workers.get(id);
	}
	
	public synchronized boolean removeWorker(int id) {
		var worker =  workers.remove(id);
		if(worker==null) return false;
		worker.close();
		return true;
	}
	
	public int getActiveWorkerCount() {
		return workers.size();
	}
	
 	public class H264RtpStreamWorker implements Runnable, AutoCloseable {
		
		private int id;

		private Resource resource;
		private SocketAddress address;
		private short initSeqNum;
		private int initTimestamp;
		private int ssrc;
		private byte payloadType;
		
		private AtomicBoolean isStreaming = new AtomicBoolean(false);
		private AtomicBoolean stopReq = new AtomicBoolean(false);
		private AtomicBoolean pause = new AtomicBoolean(false);
		
		private long timestampIncrement;
		private int frameTime;
		
		private H264RtpStreamWorker(int id, Resource resource, SocketAddress address, short initSeqNum, int initTimestamp, int ssrc, long clockHz, byte payloadType) {
			this.id = id;
			this.resource = resource;
			this.address = address;
			this.initSeqNum = initSeqNum;
			this.initTimestamp = initTimestamp;
			this.ssrc = ssrc;
			this.payloadType = payloadType;
			
			this.timestampIncrement = clockHz/resource.getFps();
			this.frameTime = 1000/resource.getFps();
		}
		
		public boolean isStreaming() {
			return isStreaming.get();
		}
		
		public boolean isPaused() {
			return pause.get();
		}
		
		@Override
		public void close() {
			stopReq.set(true);
			while(isStreaming.get());
		}
		
		public void pause() {
			if(!isStreaming.get()) throw new IllegalStateException("Stream is not running!");
			if(pause.get()) throw new IllegalStateException("Stream already paused!");
			pause.set(true);
		}
		
		public void unpause() {
			if(!pause.get()) throw new IllegalStateException("Stream is not paused!");
			pause.set(false);
		}

		@Override
		public void run() {
			System.out.println("New stream: " + resource.getName() + " " + address.toString());
			try (DatagramSocket dSocket = new DatagramSocket();
					H264FileLoader loader = new H264FileLoader(resource.getPath())){
				
				loader.nextNalUnit();                          // load the first nalu
				
				short seqNum = initSeqNum;
				int timestamp = initTimestamp;
				boolean aud = loader.getNalUnit().getType() == NalUnitType.ACCESS_UNIT_DELIMITER; // true if next nal unit is AUD nal unit; used for timestamp increment and setting the marker to 1
				
				long start = System.currentTimeMillis();       // indicates start of one access unit; used for synchronizing sending of one frame
				while(!stopReq.get()) {
					
					NalUnit nalu = loader.getNalUnit();
					byte naluData[] = nalu.getData();
					boolean end = !loader.nextNalUnit();
					aud = end ? end : loader.getNalUnit().getType() == NalUnitType.ACCESS_UNIT_DELIMITER; // checking if next nalu is aud; 
					 																					  // loader.getNalUnit() is now the next nalu after one that is currently sending
					RTPheader header = new RTPheader((byte) 2, false, false, (byte) 0, aud, payloadType, seqNum, timestamp, ssrc);
					byte UDPpayload[] = RTPUtil.createRTPpacket(new RTPpacket(header, naluData));
					
					DatagramPacket packet = new DatagramPacket(UDPpayload, UDPpayload.length, address);
					dSocket.send(packet);
					
					if(end) break;
					seqNum++;
					
					if(aud) { // skip access unit delimiter and refresh variables that need to be refreshed
						timestamp+=timestampIncrement;
						if(!loader.nextNalUnit()) break;
						aud = loader.getNalUnit().getType() == NalUnitType.ACCESS_UNIT_DELIMITER;
						
						long sleep = this.frameTime - (System.currentTimeMillis() - start); //time between two frames - time passed for sending the frame
						Thread.sleep(sleep < 0 ? 0 : sleep); //if time passed for sending the frame is greater than time between two frames, then don't sleep
						
						while(pause.get() && !stopReq.get()); // if the stream is paused
						
						start = System.currentTimeMillis();
					}
					
				}
				
			} catch (Exception ex) {
				System.err.println("Unexpected exception " + ex.getClass().getName() + " : " + ex.getMessage());
			} finally {
				isStreaming.set(false);
				removeWorker(this.id);
				System.out.println("Closed stream: " + resource.getName() + " " + address.toString());
			}
		}
		
		public int getId() {
			return id;
		}

		public Resource getResource() {
			return resource;
		}

		public SocketAddress getAddress() {
			return address;
		}

		public short getInitSeqNum() {
			return initSeqNum;
		}

		public int getInitTimestamp() {
			return initTimestamp;
		}

		public int getSsrc() {
			return ssrc;
		}

		public byte getPayloadType() {
			return payloadType;
		}

		public long getTimestampIncrement() {
			return timestampIncrement;
		}

		@Override
		public int hashCode() {
			return Objects.hash(id);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof H264RtpStreamWorker))
				return false;
			H264RtpStreamWorker other = (H264RtpStreamWorker) obj;
			return id == other.id;
		}
		
	}

}
