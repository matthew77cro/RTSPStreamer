package hr.matija.rtpStreamer.server;

public interface H264RtpStreamBandwidthSupplier {
	
	double getMaximumBandwidth();
	double getMinimumBandwidth();
	double getAverageBandwidth();
	double getMomentBandwidth();

}
