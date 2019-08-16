package hr.matija.rtpStreamer.rtp;

import java.util.Arrays;
import java.util.Objects;

/**
 * Utility for creating and parsing RTP packets and RTP headers.
 * <a href="https://tools.ietf.org/html/rfc3550">RTP Specification</a>
 * @author Matija
 *
 */
public class RTPUtil {

	/**
	 * Creates a basic RTP header and returns it as a byte array.
	 * @param version version of the RTP standard
	 * @param padding true if RTP payload has padding, false otherwise
	 * @param extension true if RTP header has extension, false otherwise
	 * @param cc CSRC count => contains the number of CSRC identifiers that follow the fixed header
	 * @param marker the interpretation of the marker is defined by a profile
	 * @param payloadType identifies the format of the RTP payload and determines its interpretation by the application
	 * @param seqNum increments by one for each RTP data packet sent, and may be used by the receiver to detect packet loss and to
      		         restore packet sequence.  The initial value of the sequence number
      				 SHOULD be random (unpredictable) to make known-plaintext attacks
     				 on encryption more difficult
	 * @param timestamp The timestamp reflects the sampling instant of the first octet in
      					the RTP data packet. The sampling instant MUST be derived from a
      					clock that increments monotonically and linearly in time to allow
      					synchronization and jitter calculations. The initial value of the timestamp SHOULD be random.
	 * @param SSRC The SSRC field identifies the synchronization source.  This
      			   identifier SHOULD be chosen randomly, with the intent that no two
      			   synchronization sources within the same RTP session will have the
      			   same SSRC identifier.
	 * @return RTP header constructed from the given parameters as byte array
	 */
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
	
	/**
	 * Creates a basic RTP header and returns it as a byte array.
	 * @param header object containing all information about an RTP header
	 * @return RTP header constructed from the given RTPheader object as byte array
	 */
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
	
	/**
	 * Parses an RTP header and returns it as an RTPheader object containing all parsed information.
	 * @param data raw RTP header
	 * @return object populated with information parsed from the raw RTP header
	 */
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
	
	/**
	 * Crates an RTP packet as an array of bytes from RTPpacket object
	 * @param packet RTPpacket object which needs to be converted into byte array
	 * @return RTP packet as an array of bytes
	 */
	public static byte[] createRTPpacket(RTPpacket packet) {
		byte[] pack = constructBasicHeaderRTP(packet.header);
		int headerSize = pack.length;
		pack = Arrays.copyOf(pack, pack.length + packet.payload.length);
		
		for(int i=headerSize; i<pack.length; i++) {
			pack[i] = packet.payload[i-headerSize];
		}
		
		return pack;
	}
	
	/**
	 * Parses an RTP packet and returns it a an RTPpacket object
	 * @param packet raw packet which needs to be parsed
	 * @return RTPpacket object parsed from given raw RTP packet data
	 */
	public static RTPpacket parseBasicRTPpakcet(byte[] packet) {
		return new RTPpacket(parseBasicHeaderRTP(packet), Arrays.copyOfRange(packet, 12, packet.length));
	}
	
	/**
	 * Class RTPpacket represents an RTP packet with header and payload
	 * @author Matija
	 *
	 */
	public static class RTPpacket {
		
		private RTPheader header;
		private byte[] payload;
		
		/**
		 * Initializes an RTPpacket object
		 * @param header rtp header
		 * @param payload rtp payload
		 */
		public RTPpacket(RTPheader header, byte[] payload) {
			this.header = header;
			this.payload = payload;
		}

		/**
		 * Returns RTP header
		 * @return RTP header
		 */
		public RTPheader getHeader() {
			return header;
		}

		/**
		 * Returns RTP payload
		 * @return RTP payload
		 */
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
	
	/**
	 * Class RTPheader represents an RTP header with basic information (no extension is supported)
	 * @author Matija
	 *
	 */
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
		
		/**
		 * Initializes RTPheader object
		 * @param version version of the RTP standard
		 * @param padding true if RTP payload has padding, false otherwise
		 * @param extension true if RTP header has extension, false otherwise
		 * @param cc CSRC count => contains the number of CSRC identifiers that follow the fixed header
		 * @param marker the interpretation of the marker is defined by a profile
		 * @param payloadType identifies the format of the RTP payload and determines its interpretation by the application
		 * @param seqNum increments by one for each RTP data packet sent, and may be used by the receiver to detect packet loss and to
	      		         restore packet sequence.  The initial value of the sequence number
	      				 SHOULD be random (unpredictable) to make known-plaintext attacks
	     				 on encryption more difficult
		 * @param timestamp The timestamp reflects the sampling instant of the first octet in
	      					the RTP data packet. The sampling instant MUST be derived from a
	      					clock that increments monotonically and linearly in time to allow
	      					synchronization and jitter calculations. The initial value of the timestamp SHOULD be random.
		 * @param SSRC The SSRC field identifies the synchronization source.  This
	      			   identifier SHOULD be chosen randomly, with the intent that no two
	      			   synchronization sources within the same RTP session will have the
	      			   same SSRC identifier.
		 */
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

		/**
		 * Returns the version of the RTP standard
		 * @return the version of the RTP standard
		 */
		public byte getVersion() {
			return version;
		}

		/**
		 * Returns true if RTP payload has padding, false otherwise
		 * @return true if RTP payload has padding, false otherwise
		 */
		public boolean isPadding() {
			return padding;
		}

		/**
		 * Returns true if RTP header has extension, false otherwise
		 * @return true if RTP header has extension, false otherwise
		 */
		public boolean isextension() {
			return extension;
		}

		/**
		 * Returns the number of CSRC identifiers that follow the fixed header
		 * @return the number of CSRC identifiers that follow the fixed header
		 */
		public byte getCc() {
			return cc;
		}

		/**
		 * Returns true if marker bit is set to 1, false otherwise
		 * @return true if marker bit is set to 1, false otherwise
		 */
		public boolean ismarker() {
			return marker;
		}

		/**
		 * Returns payload type (identifier for the format of the RTP payload)
		 * @return payload type (identifier for the format of the RTP payload)
		 */
		public byte getPayloadType() {
			return payloadType;
		}

		/**
		 * Returns sequence number of the RTP packet
		 * @return sequence number of the RTP packet
		 */
		public short getSeqNum() {
			return seqNum;
		}

		/**
		 * Returns timestamp of the RTP packet
		 * @return timestamp of the RTP packet
		 */
		public int getTimestamp() {
			return timestamp;
		}

		/**
		 * Returns the synchronization source identifier.
		 * @return the synchronization source identifier.
		 */
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
