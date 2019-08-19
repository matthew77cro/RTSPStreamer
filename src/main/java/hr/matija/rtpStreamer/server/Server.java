package hr.matija.rtpStreamer.server;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Models an abstract RTP Server
 * @author Matija
 *
 */
public interface Server {
	
	/**
	 * Sets the framerate of the video which is to be streamed.
	 * @param fps framerate of the video
	 */
	void setFPS(int fps);
	
	/**
	 * Sets the destination address. Whenever destination address
	 * is refreshed, a new .sdp file should be generated and used instead
	 * of the one used until that point.
	 * @param inetHostName destination IP
	 * @param port destination port
	 * @throws UnknownHostException if hostname is unknown
	 */
	void setDestination(String inetHostName, int port) throws UnknownHostException;
	
	/**
	 * Sets the source for streaming. A source is considered to be a
	 * raw h264 video file (file containing just NAL unit byte stream
	 * without any containers e.g. mp4, mkv etc.). Access units in the
	 * raw file must be separated by the AUD (access unit delimiter).
	 * NAL units must be prefixed by the NAL unit prefix as specified by
	 * the Annex B (0x 00 00 01 OR 0x 00 00 00 01). Example of the ffmpeg commad to convert
	 * mp4 into raw h264 file as specified above: <br><br>
	 * <code> ffmpeg -i input.mp4 -s 1920x1080 -c:v libx264 -x264opts slice-max-size=1024:aud=1:threads=4:keyint=300:fps=60 -bsf h264_mp4toannexb -an -f h264 output.h264 </code>
	 * @param h264FileName file name of the h264 file
	 */
	void setSource(String h264FileName);
	
	/**
	 * Sets the payload type for the RTP header. Whenever payload type
	 * is refreshed, a new .sdp file should be generated and used instead
	 * of the one used until that point.
	 * @param payloadType new payload type
	 */
	void setPayloadType(byte payloadType);

	/**
	 * Returns currently set framerate of the server.
	 * @return currently set framerate of the server.
	 */
	int getFPS();
	
	/**
	 * Returns currently set destination of the server (inetHostName:IP)
	 * @return currently set destination of the server (inetHostName:IP)
	 */
	String getDestionation();
	
	/**
	 * Returns currently set source for streaming
	 * @return currently set source for streaming
	 */
	String getSource();
	
	/**
	 * Returns currently set payload of the server
	 * @return currently set payload of the server
	 */
	byte getPayloadType();
	
	/**
	 * Starts the stream with currently set parameters.
	 */
	void stream();
	
	/**
	 * Stops the stream if it is running. This is a blocking call
	 * so by the time this method returns, stream WILL be stopped.
	 */
	void stopStream();
	
	/**
	 * Returns true iff server is currently streaming, false otherwise.
	 * @return true iff server is currently streaming, false otherwise.
	 */
	boolean isStreaming();
	
	/**
	 * Generates the .sdp file to be opened in a VLC media player, ffplay or
	 * similar players for excepting the stream OR to be sent via RTSP.
	 * The .sdp file contains information about the stream (host, port, 
	 * encoding etc.).
	 * @param fileName
	 * @throws IOException
	 */
	void generateSdpFile(String fileName) throws IOException;
	
}
