package hr.matija.rtpStreamer.rtsp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Utility for RTSP request and response headers. Contains method that can
 * crate and parse RTSP headers. <br>
 * See <a href="https://tools.ietf.org/html/rfc7826">RTSP Specification</a>
 * @author Matija
 *
 */
public class RTSPUtil {
	
	/**
	 * Reads next rtsp request from the given input stream and returns it as
	 * {@link RTSPRequest} object.
	 * @param is input stream from which the rtsp request needs to be red
	 * @return parsed rtsp request as {@link RTSPRequest} object
	 * @throws IOException if an I/O exception occurs
	 */
	public static RTSPRequest readNextRTSPRequest(InputStream is) throws IOException {
		int crlfcrlf = 0; // \r\n\r\n sequence counter
		// reading the request
		StringBuilder sb = new StringBuilder();
		while(crlfcrlf!=4) {
			int r = is.read();
			if(r==-1) return null; //END OF STREAM
			if(r=='\r' || r=='\n') crlfcrlf++;
			else crlfcrlf = 0;
			sb.append((char)r);
		}
		
		return RTSPUtil.parseRTSPRequest(sb.toString());
	}
	
	/**
	 * Parses the rtsp request nd returns it as
	 * {@link RTSPRequest} object.
	 * @param request rtsp request that needs to be parsed
	 * @return parsed rtsp request as {@link RTSPRequest} object
	 */
	public static RTSPRequest parseRTSPRequest(String request) {		
		RTSPMethod method = null;
		String uri = null;
		RTSPVersion version = null;
		Map<String, String> keyValuePairs = new HashMap<>();
		
		String[] req = request.trim().split("\r\n");
		String[] firstLine = req[0].split("\\s+");
		switch(firstLine[0]) {
			case "OPTIONS" : method = RTSPMethod.OPTIONS; break;
			case "DESCRIBE" : method = RTSPMethod.DESCRIBE; break;
			case "SETUP" : method = RTSPMethod.SETUP; break;
			case "PLAY" : method = RTSPMethod.PLAY; break;
			case "PAUSE" : method = RTSPMethod.PAUSE; break;
			case "TEARDOWN" : method = RTSPMethod.TEARDOWN; break;
			default : method = RTSPMethod.OTHER;
		}
		uri = firstLine[1];
		if(firstLine[2].equals(RTSPVersion.RTSP1_0.toString())) {
			version = RTSPVersion.RTSP1_0;
		} else if(firstLine[2].equals(RTSPVersion.RTSP2_0.toString())) {
			version = RTSPVersion.RTSP2_0;
		} else {
			version = RTSPVersion.OTHER;
		}
		
		for(int i=1; i<req.length; i++) {
			String[] next = req[i].trim().split("\\s*:\\s*");
			keyValuePairs.put(next[0], next[1]);
		}
		
		return new RTSPRequest(request, method, uri, version, keyValuePairs);
	}
	
	/**
	 * Creates and returns an RTSP response.
	 * @param version version of the RTSP protocol
	 * @param status status of the response (e.g. 200)
	 * @param statusName status name of the response (e.g. OK)
	 * @param keyValuePairs key value pairs that need to be contained within the response (e.g. "Session" -> "ABCD1234")
	 * @param payload payload of the response (e.g. sdp payload for the response to the DESCRIBE request)
	 * @return correctly formatted rtsp response as {@link String}
	 */
	public static String createRTSPResponse(RTSPVersion version, int status, String statusName, Map<String, String> keyValuePairs, String payload) {
		StringBuilder sb = new StringBuilder();
		String delimiter = "\r\n";
		if(version == RTSPVersion.OTHER) throw new IllegalArgumentException("Version cannot be " + version);
		
		sb.append(version + " " + status + " " + statusName + delimiter);
		for(var pair : keyValuePairs.entrySet()) {
			sb.append(pair.getKey() + ": " + pair.getValue() + delimiter);
		}
		sb.append(delimiter);
		
		if(payload!=null && !payload.isEmpty()) sb.append(payload);
		
		return sb.toString();
	}
	
	/**
	 * RTSP request methods (e.q. OPTIONS, PLAY, TEARDOWN...)
	 * @author Matija
	 */
	public enum RTSPMethod {
		
		OPTIONS("OPTIONS"),
		DESCRIBE("DESCRIBE"),
		SETUP("SETUP"),
		PLAY("PLAY"),
		PAUSE("PAUSE"),
		TEARDOWN("TEARDOWN"),
		OTHER("OTHER");
		
		private String name;
		
		private RTSPMethod(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			return name;
		}
		
	}
	
	/**
	 * RTSP versions
	 * @author Matija
	 */
	public enum RTSPVersion {
		
		RTSP1_0("RTSP/1.0"),
		RTSP2_0("RTSP/2.0"),
		OTHER("OTHER");
		
		private String name;
		
		private RTSPVersion(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			return name;
		}
		
	}
	
	/**
	 * Represents an RTSP request. <br>
	 * See <a href="https://tools.ietf.org/html/rfc7826">RTSP Specification</a>
	 * @author Matija
	 */
	public static class RTSPRequest {
		
		private RTSPMethod method;
		private String uri;
		private RTSPVersion version;
		private Map<String, String> keyValuePairs;
		
		private String req;
		
		public RTSPRequest(String req, RTSPMethod method, String uri, RTSPVersion version, Map<String, String> keyValuePairs) {
			this.req = req;
			this.method = Objects.requireNonNull(method);
			this.uri = uri;
			this.version = version;
			this.keyValuePairs = Collections.unmodifiableMap(keyValuePairs);
		}
		
		public RTSPMethod getMethod() {
			return method;
		}
		
		public String getUri() {
			return uri;
		}
		
		public RTSPVersion getVersion() {
			return version;
		}
		
		public String getValue(String key) {
			return keyValuePairs.get(key);
		}
		
		public boolean isEmpty() {
			return keyValuePairs.isEmpty();
		}
		
		@Override
		public String toString() {
			return req;
		}
		
	}

}
