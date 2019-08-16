package hr.matija.rtpStreamer.h264;

/**
 * Abstract loader of the h264 nal units. Acts as a one-direction-only cursor.
 * @author Matija
 *
 */
public interface H264Loader {
	
	/**
	 * Loads the next nal unit and returns true iff loader has not reached the end.
	 * @return true if there is a nal unit to get (loader has not reached the end), false otherwise
	 */
	boolean nextNalUnit();
	
	/**
	 * Returns the nal unit loaded by the nextNalUnit() method
	 * @return nal unit on which the cursor is currently poining.
	 */
	NalUnit getNalUnit();

}
