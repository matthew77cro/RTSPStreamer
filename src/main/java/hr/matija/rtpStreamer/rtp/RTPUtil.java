package hr.matija.rtpStreamer.rtp;

import java.util.Arrays;
import java.util.Objects;

public class RTPUtil {

	public static byte[] constructBasicHeaderRTP(byte version, boolean padding, boolean extension,
												byte cc, boolean marker, byte payloadType, short seqNum,
												int timestamp, int SSRC) {

		byte[] header = new byte[12];
		header[0] = (byte) ((version << 6 & 0xC0) |
							(padding ? 0x20 : 0) |
							(extension ? 0x10 : 0) |
							(cc & 0x0F)
							);
		header[1] = (byte) ((marker ? 0x80 : 0) |
							(payloadType & 0x7F)
							);
		header[2] = (byte) (seqNum >> 8 & 0x00FF);
		header[3] = (byte) (seqNum & 0x00FF);
		header[4] = (byte) (timestamp >> 24 & 0x000000FF);
		header[5] = (byte) (timestamp >> 16 & 0x000000FF);
		header[6] = (byte) (timestamp >> 8 & 0x000000FF);
		header[7] = (byte) (timestamp & 0x000000FF);
		header[8] = (byte) (SSRC >> 24 & 0x000000FF);
		header[9] = (byte) (SSRC >> 16 & 0x000000FF);
		header[10] = (byte) (SSRC >> 8 & 0x000000FF);
		header[11] = (byte) (SSRC & 0x000000FF);
		
		return header;
	}
	
	public static byte[] constructBasicHeaderRTP(RTPheader header) {
		return constructBasicHeaderRTP(header.version, 
										header.padding, 
										header.extension, 
										header.cc, 
										header.marker, 
										header.payloadType, 
										header.seqNum, 
										header.timestamp, 
										header.SSRC);
	}
	
	public static RTPheader parseBasicHeaderRTP(byte[] data) {
		RTPheader header = new RTPheader();
		
		header.version = (byte) (data[0] >> 6 & 0x03);
		header.padding = (data[0] & 0x20) > 0 ? true : false;
		header.extension = (data[0] & 0x10) > 0 ? true : false;
		header.cc = (byte) (data[0] & 0x0F);
		header.marker = (data[1] & 0x80) > 0 ? true : false;
		header.payloadType = (byte) (data[1] & 0x7F);
		header.seqNum = (short) ((data[2] << 8) & 0xFF00 | (data[3] & 0x00FF));
		header.timestamp = (data[4] << 24) & 0xFF000000 | (data[5] << 16) & 0x00FF0000 | (data[6] << 8) & 0x0000FF00 | data[7] & 0x000000FF;
		header.SSRC = (data[8] << 24) & 0xFF000000 | (data[9] << 16) & 0x00FF0000 | (data[10] << 8) & 0x0000FF00 | data[11] & 0x000000FF;
		
		return header;
	}
	
	public static byte[] createRTPpacket(RTPpacket packet) {
		byte[] pack = constructBasicHeaderRTP(packet.header);
		int headerSize = pack.length;
		pack = Arrays.copyOf(pack, pack.length + packet.payload.length);
		
		for(int i=headerSize; i<pack.length; i++) {
			pack[i] = packet.payload[i-headerSize];
		}
		
		return pack;
	}
	
	public static RTPpacket parseBasicRTPpakcet(byte[] packet) {
		return new RTPpacket(parseBasicHeaderRTP(packet), Arrays.copyOfRange(packet, 12, packet.length));
	}
	
	public static class RTPpacket {
		
		private RTPheader header;
		private byte[] payload;
		
		public RTPpacket(RTPheader header, byte[] payload) {
			this.header = header;
			this.payload = payload;
		}

		public RTPheader getHeader() {
			return header;
		}

		public byte[] getPayload() {
			return payload;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(payload);
			result = prime * result + Objects.hash(header);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof RTPpacket))
				return false;
			RTPpacket other = (RTPpacket) obj;
			return Objects.equals(header, other.header) && Arrays.equals(payload, other.payload);
		}
		
	}
	
	public static class RTPheader {
		
		private byte version;
		private boolean padding;
		private boolean extension;
		private byte cc;
		private boolean marker;
		private byte payloadType;
		private short seqNum;
		int timestamp;
		private int SSRC;
		
		public RTPheader(byte version, boolean padding, boolean extension, byte cc, boolean marker, byte payloadType,
				short seqNum, int timestamp, int sSRC) {
			this.version = version;
			this.padding = padding;
			this.extension = extension;
			this.cc = cc;
			this.marker = marker;
			this.payloadType = payloadType;
			this.seqNum = seqNum;
			this.timestamp = timestamp;
			SSRC = sSRC;
		}
		
		private RTPheader() {
			
		}

		public byte getVersion() {
			return version;
		}

		public boolean isPadding() {
			return padding;
		}

		public boolean isextension() {
			return extension;
		}

		public byte getCc() {
			return cc;
		}

		public boolean ismarker() {
			return marker;
		}

		public byte getPayloadType() {
			return payloadType;
		}

		public short getSeqNum() {
			return seqNum;
		}

		public int getTimestamp() {
			return timestamp;
		}

		public int getSSRC() {
			return SSRC;
		}

		@Override
		public int hashCode() {
			return Objects.hash(SSRC, cc, extension, marker, padding, payloadType, seqNum, timestamp, version);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof RTPheader))
				return false;
			RTPheader other = (RTPheader) obj;
			return SSRC == other.SSRC && cc == other.cc && extension == other.extension && marker == other.marker
					&& padding == other.padding && payloadType == other.payloadType && seqNum == other.seqNum
					&& timestamp == other.timestamp && version == other.version;
		}
		
		@Override
		public String toString() {
			return "V " + Integer.toHexString(version & 0x000000FF) + String.format("%n") +
					"P " + padding + String.format("%n") +
					"X " + extension + String.format("%n") +
					"CC " + Integer.toHexString(cc & 0x000000FF) + String.format("%n") +
					"M " + marker + String.format("%n") +
					"PT " + Integer.toHexString(payloadType & 0x000000FF) + String.format("%n") +
					"SN " + Integer.toHexString(seqNum & 0x0000FFFF) + String.format("%n") +
					"TS " + Integer.toHexString(timestamp) + String.format("%n") +
					"SSRC " + Integer.toHexString(SSRC);
		}
		
	}
	
}
