package hr.matija.rtpStreamer.h264;

import java.util.Arrays;
import java.util.Objects;

/**
 * High level representation of Network abstraction layer (NAL) units.
 * @author Matija
 *
 */
public class NalUnit {
	
	/**
	 * Type of the nal unit.
	 * @author Matija
	 *
	 */
	public enum NalUnitType {
		CODED_SLICE_NON_IDR ("Coded slice of a non-IDR picture"),
		CODED_SLICE_IDR ("Coded slice of an IDR picture"),
		SUPPLEMENTAL_ENANCEMENT_INFORMATION ("Supplemental enhancement information"),
		SEQUENCE_PARAMETER_SET ("Sequence parameter set"),
		PICTURE_PARAMETER_SET ("Picture parameter set"),
		ACCESS_UNIT_DELIMITER ("Access unit delimiter"),
		END_OF_SEQUENCE ("End of sequence"),
		END_OF_STREAM ("End of stream"),
		OTHER ("Other");
		
		private String longName;
		
		private NalUnitType(String longName) {
			this.longName = Objects.requireNonNull(longName);
		}
		
		@Override
		public String toString() {
			return longName;
		}
	}
	
	private byte[] data;
	
	private byte nalUnitHeader;
	private boolean forbiddenZeroBit;
	private byte nalRefIdc;
	private byte nalUnitType;
	
	private NalUnitType type;
	
	/**
	 * Creates and initializes the nal unit
	 * @param nalData nal unit in the raw byte array form
	 * @param nalDataLen length of the nal unit in bytes
	 */
	public NalUnit(byte[] nalData, int nalDataLen) {
		if(nalData==null) throw new NullPointerException();
		
		this.data = Arrays.copyOf(nalData, nalDataLen);
		init();
	}

	/**
	 * Initializes the nal unit
	 */
	private void init() {
		nalUnitHeader = data[0];
		forbiddenZeroBit = (nalUnitHeader & 0x80) != 0;
		nalRefIdc = (byte) ((nalUnitHeader >> 5) & 0x03);
		nalUnitType = (byte) (nalUnitHeader & 0x1F);
		
		switch(nalUnitType) {
			case 1:
				type = NalUnitType.CODED_SLICE_NON_IDR;
				break;
			case 5:
				type = NalUnitType.CODED_SLICE_IDR;
				break;
			case 6:
				type = NalUnitType.SUPPLEMENTAL_ENANCEMENT_INFORMATION;
				break;
			case 7:
				type = NalUnitType.SEQUENCE_PARAMETER_SET;
				break;
			case 8:
				type = NalUnitType.PICTURE_PARAMETER_SET;
				break;
			case 9:
				type = NalUnitType.ACCESS_UNIT_DELIMITER;
				break;
			case 10:
				type = NalUnitType.END_OF_SEQUENCE;
				break;
			case 11:
				type = NalUnitType.END_OF_STREAM;
				break;
			default :
				type = NalUnitType.OTHER;
		}
	}
	
	/**
	 * Returns the data of the nal unit (raw nal unit in the byte array form)
	 * @return nal unit as byte array
	 */
	public byte[] getData() {
		return Arrays.copyOf(data, data.length);
	}
	
	/**
	 * Returns the nal unit header
	 * @return the nal unit header
	 */
	public byte getNalUnitHeader() {
		return nalUnitHeader;
	}
	
	/**
	 * Returns true iff forbiddenZeroBit is set to 1, false otherwise
	 * @return true iff forbiddenZeroBit is set to 1, false otherwise
	 */
	public boolean isForbiddenZeroBit() {
		return forbiddenZeroBit;
	}
	
	/**
	 * Returns the nal unit reference indicator
	 * @return the nal unit reference indicator
	 */
	public byte getNalRefIdc() {
		return nalRefIdc;
	}
	
	/**
	 * Returns the nal unit type
	 * @return the nal unit type
	 */
	public byte getNalUnitType() {
		return nalUnitType;
	}
	
	/**
	 * Returns the nal unit type
	 * @return the nal unit type
	 */
	public NalUnitType getType() {
		return type;
	}
	
	@Override
	public String toString() {
		return String.format("Forbidden Zero Bit: " + forbiddenZeroBit + "%n" +
							 "NalRefIdc : " + nalRefIdc + "%n" +
							 "NalUnitType : " + nalUnitType + "%n" +
							 "NalUnitType Name : " + type);
	}

}
