package hr.matija.rtpStreamer.rtsp;

import java.util.Arrays;
import java.util.Objects;

/**
 * URI (Uniform Resource Identifier) parser for rtsp uri-s. Can also be used for other schema uri-s, BUT: only supports
 * uri-s of this pattern : [schema]://[host]:[port]/[path] (port is optional).
 * Does not yet support userinfo, query or fragment parts of uri specification.
 * @author Matija
 *
 */
public class UriParser {
	
	/**
	 * Parses rtsp uri-s. For supported uri patterns, please read javadoc of
	 * this class.
	 * @param uri uri which needs to be parsed
	 * @return parsed uri
	 */
	public static Uri parseUri(String uri) {
		String protocol, serverAddress, port = null, resource = null;
		char[] uriChar = uri.toCharArray();
		
		StringBuilder sb = new StringBuilder();
		int ptr = 0; // uriChar current character pointer
		
		while(ptr<uriChar.length && uriChar[ptr] != ':') {
			sb.append(uriChar[ptr++]);
		}
		protocol = sb.toString();
		sb.setLength(0); // clear the stringbuilder
		ptr+=3; // skip double forward slash
		
		while(ptr<uriChar.length && uriChar[ptr]!=':' && uriChar[ptr]!='/') {
			sb.append(uriChar[ptr++]);
		}
		serverAddress = sb.toString();
		sb.setLength(0); // clear the stringbuilder
		
		if(ptr<uriChar.length && uriChar[ptr++]==':') {
			while(ptr<uriChar.length && uriChar[ptr]!='/') {
				sb.append(uriChar[ptr++]);
			}
			port = sb.toString();
			sb.setLength(0); // clear the stringbuilder
		}
		
		resource = new String(Arrays.copyOfRange(uriChar, ptr, uriChar.length));
		
		return new Uri(protocol, serverAddress, port, resource);
	}
	
	/**
	 * Models a uri of pattern : [schema]://[host]:[port]/[path]. Does
	 * not support userinfo, query or fragments.
	 * @author Matija
	 *
	 */
	public static class Uri {
		
		//example : rtsp://192.168.1.2:25565/video/sample1
		private String scheme; // rtsp
		private String host; // 192.168.1.2
		private String port; //25565
		private String path; // /video/sample1

		/**
		 * Creates and initialises new uri.
		 * @param scheme scheme of the uri
		 * @param host host of the uri
		 * @param port port of the uri, can be <code>null</code>
		 * @param path path of the uri
		 * @throws NullPointerException if scheme, host or path are null
		 */
		public Uri(String scheme, String host, String port, String path) {
			this.scheme = Objects.requireNonNull(scheme);
			this.host = Objects.requireNonNull(host);
			this.port = port;
			this.path = Objects.requireNonNull(path);
		}
		
		public String getScheme() {
			return scheme;
		}

		public String getHost() {
			return host;
		}

		public String getPort() {
			return port;
		}

		public String getPath() {
			return path;
		}

		@Override
		public int hashCode() {
			return Objects.hash(host, path, port, scheme);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Uri))
				return false;
			Uri other = (Uri) obj;
			return Objects.equals(host, other.host) && Objects.equals(path, other.path)
					&& Objects.equals(port, other.port) && Objects.equals(scheme, other.scheme);
		}

		@Override
		public String toString() {
			if(port!=null)
				return scheme + "://" + host + ":" + port + path;
			else
				return scheme + "://" + host + path;
		}
		
	}

}
