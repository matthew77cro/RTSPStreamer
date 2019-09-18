package hr.matija.rtpStreamer.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import hr.matija.rtpStreamer.console.ConsoleWriter;
import hr.matija.rtpStreamer.h264.H264FileLoader;
import hr.matija.rtpStreamer.h264.H264LiveCameraLoader;
import hr.matija.rtpStreamer.h264.H264Loader;
import hr.matija.rtpStreamer.h264.NalUnit.NalUnitType;
import hr.matija.rtpStreamer.rtp.RTPUtil;
import hr.matija.rtpStreamer.rtp.RTPUtil.RTPheader;
import hr.matija.rtpStreamer.rtp.RTPUtil.RTPpacket;
import hr.matija.rtpStreamer.server.H264RtspResourceCollection.Resource;

public class H264RtpStreamWorkerCollection {
	
	private long clockHz = 90_000;
	private byte payloadType = 96;
	private ConsoleWriter writer;
	
	private double packageDropRate = 0;
	private int packageMultiplier = 1;
	private boolean allowDropAll = true;
	private boolean onlyRetransmitImportant = false;
	
	private Map<Integer, H264RtpStreamWorker> workers = new HashMap<>();
	
	public H264RtpStreamWorkerCollection() {
	}

	public H264RtpStreamWorkerCollection(long clockHz, byte payloadType, ConsoleWriter writer) {
		this.clockHz = clockHz;
		this.payloadType = payloadType;
		this.writer = Objects.requireNonNull(writer);
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
		if(worker.isStreaming()) worker.close();
		return true;
	}
	
	public int getActiveWorkerCount() {
		return workers.size();
	}
	
	public long getClockHz() {
		return clockHz;
	}
	
	public byte getPayloadType() {
		return payloadType;
	}
	
	public double getPackageDropRate() {
		return packageDropRate;
	}

	public void setPackageDropRate(double packageDropRate) {
		this.packageDropRate = packageDropRate/100;
	}

	public int getPackageMultiplier() {
		return packageMultiplier;
	}

	public void setPackageMultiplier(int packageMultiplier) {
		this.packageMultiplier = packageMultiplier;
	}

	public boolean isDropAllAllowed() {
		return allowDropAll;
	}
	
	public void setAllowDropAll(boolean allowDropAll) {
		this.allowDropAll = allowDropAll;
	}
	
	public boolean isOnlyRetransmitImportant() {
		return onlyRetransmitImportant;
	}
	
	public void setOnlyRetransmitImportant(boolean onlyRetransmitImportant) {
		this.onlyRetransmitImportant = onlyRetransmitImportant;
	}

 	public class H264RtpStreamWorker implements Runnable, AutoCloseable, H264RtpStreamBandwidthSupplier {
		
		private int id;

		private Resource resource;
		private SocketAddress address;
		private short initSeqNum;
		private int initTimestamp;
		private int ssrc;
		private long clockHz;
		private byte payloadType;
		
		private AtomicBoolean isStreaming = new AtomicBoolean(false);
		private AtomicBoolean stopReq = new AtomicBoolean(false);
		private AtomicBoolean pause = new AtomicBoolean(false);
		
		private long timestampIncrement;
		private int frameTime;
		
		private double maxBandwidth;
		private double minBandwidth = Double.MAX_VALUE;
		private double momentBandwidth;
		private double momentBandwidthSum = 0;
		private long momentBandwithCount = 0;
		
		private H264RtpStreamWorker(int id, Resource resource, SocketAddress address, short initSeqNum, int initTimestamp, int ssrc, long clockHz, byte payloadType) {
			this.id = id;
			this.resource = resource;
			this.address = address;
			this.initSeqNum = initSeqNum;
			this.initTimestamp = initTimestamp;
			this.ssrc = ssrc;
			this.clockHz = clockHz;
			this.payloadType = payloadType;
			
			this.timestampIncrement = clockHz/resource.getFps();
			this.frameTime = 1000/resource.getFps();
		}

		@Override
		public void run() {
			if(isStreaming.get()) throw new IllegalStateException("Already streaming!");
			isStreaming.set(true);
			stopReq.set(false);
			writer.writeInfo("New stream: " + resource.getName() + " " + address.toString());
			
			try (DatagramSocket dSocket = new DatagramSocket()){
				
				H264Loader loader;
				if(resource.getName().startsWith("live://")) loader = new H264LiveCameraLoader(resource.getName().substring(7));
				else loader = new H264FileLoader(resource.getPath());
				sendFrames(dSocket, loader);
				loader.close();
				
			} catch (Exception ex) {
				ex.printStackTrace();
				writer.writeError("Unexpected exception " + ex.getClass().getName() + " : " + ex.getMessage());
			} finally {
				writer.writeInfo("Closed stream: " + resource.getName() + " " + address.toString());
				isStreaming.set(false);
				removeWorker(this.id);
			}
		}
		
		private void sendFrames(DatagramSocket dSocket, H264Loader loader) throws IOException, InterruptedException {
			short seqNum = initSeqNum;
			int timestamp = initTimestamp;
			double bytesToMbitsMultiplier = 8.0/1_000_000;
			
			Thread.sleep(50);
			long start = System.currentTimeMillis();       // indicates start of one access unit; used for synchronizing sending of one frame

			long tic = System.currentTimeMillis();
			long toc = System.currentTimeMillis();
			double transferedSizeMb = 0;
			while(!stopReq.get()) {
				if(!loader.nextNalUnit()) break;
				
				if(loader.getNalUnit().getType()==NalUnitType.ACCESS_UNIT_DELIMITER) {
					timestamp+=timestampIncrement;
					
					long delay = System.currentTimeMillis() - start;
					long sleep = this.frameTime - delay;
					if(sleep>0) Thread.sleep(sleep);
					
					while(pause.get() && !stopReq.get());
					
					start = System.currentTimeMillis();
					continue;
				}
				
				byte nalu[] = loader.getNalUnit().getData(); 
				RTPheader header = new RTPheader((byte) 2, false, false, (byte) 0, false, payloadType, seqNum, timestamp, ssrc);
				byte UDPpayload[] = RTPUtil.createRTPpacket(new RTPpacket(header, nalu));
				DatagramPacket packet = new DatagramPacket(UDPpayload, UDPpayload.length, address);
				
				double packageSize = UDPpayload.length*bytesToMbitsMultiplier;
				if(onlyRetransmitImportant && loader.getNalUnit().getType() == NalUnitType.CODED_SLICE_NON_IDR) {
					if(Math.random()>=packageDropRate ||
							!allowDropAll && loader.getNalUnit().getType()!=NalUnitType.CODED_SLICE_NON_IDR) {
							dSocket.send(packet);
							transferedSizeMb+=packageSize;
					}
				} else {
					for(int cnt=0; cnt<packageMultiplier; cnt++) {
						if(Math.random()<packageDropRate &&
							(allowDropAll || loader.getNalUnit().getType()==NalUnitType.CODED_SLICE_NON_IDR)) continue;
							dSocket.send(packet);
							transferedSizeMb+=packageSize;
					}
				}
				
				toc = System.currentTimeMillis();
				
				if(toc-tic>=1000) {
					this.momentBandwidth = transferedSizeMb / ((toc-tic) / 1000.0);
					if (Double.MAX_VALUE - this.momentBandwidthSum > this.momentBandwidth) //TODO overflow
					this.momentBandwidthSum += this.momentBandwidth;
					this.momentBandwithCount++;
					if(momentBandwidth>maxBandwidth) maxBandwidth = momentBandwidth;
					if(momentBandwidth<minBandwidth) minBandwidth = momentBandwidth;
					transferedSizeMb = 0;
					tic = System.currentTimeMillis();
				}
				
				seqNum++;
				
			}
			writer.writeInfo("Maximum bandwidth: " + maxBandwidth);
			writer.writeInfo("Minimum bandwidth: " + minBandwidth);
			writer.writeInfo("Average bandwidth: " + momentBandwidthSum/momentBandwithCount);
			
		}
		
		@Override
		public double getMaximumBandwidth() {
			return maxBandwidth;
		}

		@Override
		public double getMinimumBandwidth() {
			return minBandwidth;
		}

		@Override
		public double getAverageBandwidth() {
			return this.momentBandwidthSum/this.momentBandwithCount;
		}

		@Override
		public double getMomentBandwidth() {
			return momentBandwidth;
		}

		public boolean isStreaming() {
			return isStreaming.get();
		}
		
		@Override
		public void close() {
			stopReq.set(true);
			while(isStreaming.get());
		}
		
		public boolean isPaused() {
			return pause.get();
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
		
		public long getClockHz() {
			return clockHz;
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
