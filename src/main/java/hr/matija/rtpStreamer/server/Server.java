package hr.matija.rtpStreamer.server;

import java.io.IOException;
import java.net.UnknownHostException;

public interface Server {
	
	void setFPS(int fps);
	void setDestination(String inetHostName, int port) throws UnknownHostException;
	void setSource(String h264FileName);
	void stream();
	void stopStream();

	int getFPS();
	String getDestionation();
	String getSource();
	boolean isStreaming();
	
	void generateSdpFile(String fileName) throws IOException;
	byte getPayloadType();
	void setPayloadType(byte payloadType);
	
}
