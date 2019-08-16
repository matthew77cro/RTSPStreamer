package hr.matija.rtpStreamer.h264;

public interface H264Loader {
	
	boolean nextNalUnit();
	NalUnit getNalUnit();

}
