package hr.matija.rtpStreamer.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import hr.matija.rtpStreamer.console.ConsoleWriter;
import hr.matija.rtpStreamer.rtsp.RTSPUtil;
import hr.matija.rtpStreamer.rtsp.UriParser;
import hr.matija.rtpStreamer.rtsp.RTSPUtil.RTSPRequest;
import hr.matija.rtpStreamer.server.H264RtpStreamWorkerCollection.H264RtpStreamWorker;
import hr.matija.rtpStreamer.server.H264RtspResourceCollection.Resource;

public class H264RtspReqHandlerCollection{
	
	private H264RtpStreamWorkerCollection streamWorkers;
	private H264RtspResourceCollection resources;
	private ConsoleWriter writer;
	
	private Map<Integer, H264RtspReqHandler> handlers = new HashMap<>();
	private Map<InetAddress, H264RtspReqHandler> handlers_inet = new HashMap<>();
	
	private Set<H264RtspReqHandlerCollectionListener> listeners = new HashSet<>();
	
	public H264RtspReqHandlerCollection(H264RtpStreamWorkerCollection streamWorkers, H264RtspResourceCollection resources, ConsoleWriter writer) {
		this.streamWorkers = Objects.requireNonNull(streamWorkers);
		this.resources = Objects.requireNonNull(resources);
		this.writer = Objects.requireNonNull(writer);
	}

	public synchronized int makeHandler(Socket s, int ssrc) {
		int id;
		do {
			id = (int) (Math.random() * Integer.MAX_VALUE);
		} while(handlers.keySet().contains(id));
		
		var handler = new H264RtspReqHandler(id, s, ssrc, streamWorkers, resources);
		handlers.put(id, handler);
		handlers_inet.put(s.getInetAddress(), handler);
		for(var l : listeners) {
			l.handlerAdded(handler);
		}
		return id;
	}
	
	public synchronized H264RtspReqHandler getHandler(int id) {
		return handlers.get(id);
	}
	
	public synchronized H264RtspReqHandler getHandler(InetAddress inet) {
		return handlers_inet.get(inet);
	}
	
	public synchronized boolean removeHandler(int id) {
		var handler =  handlers.remove(id);
		if(handler==null) return false;
		handlers_inet.remove(handler.s.getInetAddress());
		if(handler.isRunning()) handler.close();
		for(var l : listeners) {
			l.handlerRemoved(handler);
		}
		return true;
	}
	
	public synchronized void closeAllConections() {
		for(var handler : handlers.values()) {
			handler.close();
			for(var l : listeners) {
				l.handlerRemoved(handler);
			}
		}
		handlers.clear();
		handlers_inet.clear();
	}
	
	public synchronized Collection<H264RtspReqHandler> getAllHandlers() {
		return handlers.values();
	}
	
	public int getActiveConnectionCount() {
		return handlers.size();
	}
	
	public H264RtpStreamWorkerCollection getStreamWorkerCollection() {
		return streamWorkers;
	}
	
	public void addListener(H264RtspReqHandlerCollectionListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(H264RtspReqHandlerCollectionListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Rtsp connection handler for handling requests over tcp
	 * @author Matija
	 *
	 */
	public class H264RtspReqHandler implements Runnable, AutoCloseable {

		private int id;
		private Socket s;
		private int ssrc;
		private H264RtpStreamWorkerCollection streamWorkers;
		private H264RtspResourceCollection resources;
		
		private H264RtpStreamWorker currentStreamWorker;
		
		private long lastRequestTimestamp = System.currentTimeMillis(); // used for garbage collection
		private short initSeqNum = (short)(Math.random() * Short.MAX_VALUE);
		private int initTimestamp = (int) (Math.random() * Integer.MAX_VALUE);
		private int session = (int) (Math.random() * Integer.MAX_VALUE);
		
		private AtomicBoolean isRunning = new AtomicBoolean(false);
		private AtomicBoolean stopReq = new AtomicBoolean(false);
			
		public H264RtspReqHandler(int id, Socket s, int ssrc, H264RtpStreamWorkerCollection streamWorkers, H264RtspResourceCollection resources) {
			this.id = id;
			this.s = Objects.requireNonNull(s);
			this.ssrc = ssrc;
			this.streamWorkers = Objects.requireNonNull(streamWorkers);
			this.resources = resources;
		}
		
		public boolean isRunning() {
			return isRunning.get();
		}
		
		@Override
		public void close() {
			stopReq.set(true);
			while(isRunning.get());
		}
		
		@Override
		public void run() {
			if(isRunning.get()) throw new IllegalStateException("Already running!");
			isRunning.set(true);
			stopReq.set(false);
			writer.writeInfo("New connection: " + s.getInetAddress());
			
			try (BufferedInputStream bis = new BufferedInputStream(s.getInputStream());
					BufferedOutputStream bos = new BufferedOutputStream(s.getOutputStream())){
				
				while(!stopReq.get()) {
					if(bis.available()<=0) continue;
					RTSPRequest req = RTSPUtil.readNextRTSPRequest(bis);
					if(req==null) break; // END OF STREAM
					lastRequestTimestamp = System.currentTimeMillis();
					
					Map<String, String> kvpairs = new HashMap<>();
					kvpairs.put("CSeq", req.getValue("CSeq"));
					String session;
					if((session = req.getValue("Session")) != null) {
						kvpairs.put("Session", session);
					}
					
					switch(req.getMethod()) {
						case OPTIONS:
							kvpairs.put("Public", "OPTIONS, DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE");
							bos.write(RTSPUtil.createRTSPResponse(req.getVersion(), 200, "OK", kvpairs, null).getBytes());
							break;
						case DESCRIBE:
							kvpairs.put("Content-Base", req.getUri());
							kvpairs.put("Content-Type","application/sdp");
							kvpairs.put("Content-Length","67");
							String payload = "c=IN IP4 127.0.0.1\r\nm=video 5004 RTP/AVP " + streamWorkers.getPayloadType() + "\r\na=rtpmap:" + streamWorkers.getPayloadType() + " H264/" + streamWorkers.getClockHz();
							bos.write(RTSPUtil.createRTSPResponse(req.getVersion(), 200, "OK", kvpairs, payload).getBytes());
							break;
						case SETUP:
							String reqResource = UriParser.parseUri(req.getUri()).getPath().replace("/", "");
							Resource r = resources.getResourceForUri(reqResource);
							
							if(r==null) {
								writer.writeError("Not found: " + reqResource);
								bos.write(RTSPUtil.createRTSPResponse(req.getVersion(), 404, "Not Found", kvpairs, null).getBytes());
								bos.flush();
								break;
							}
							
							if(currentStreamWorker!=null) streamWorkers.removeWorker(currentStreamWorker.getId());
							currentStreamWorker = streamWorkers.getWorker(streamWorkers.makeWorker(r, new InetSocketAddress(s.getInetAddress(), 5004), initSeqNum, initTimestamp, ssrc));
							kvpairs.put("Transport", req.getValue("Transport"));
							if(!kvpairs.containsKey("Session")) kvpairs.put("Session", Integer.toString(this.session));
							bos.write(RTSPUtil.createRTSPResponse(req.getVersion(), 200, "OK", kvpairs, null).getBytes());
							bos.flush();
							break;
						case PLAY:
							new Thread(currentStreamWorker).start();
							bos.write(RTSPUtil.createRTSPResponse(req.getVersion(), 200, "OK", kvpairs, null).getBytes());
							bos.flush();
							break;
						case PAUSE:
							currentStreamWorker.pause();
							bos.write(RTSPUtil.createRTSPResponse(req.getVersion(), 200, "OK", kvpairs, null).getBytes());
							bos.flush();
							break;
						case TEARDOWN:
							streamWorkers.removeWorker(currentStreamWorker.getId());
							bos.write(RTSPUtil.createRTSPResponse(req.getVersion(), 200, "OK", kvpairs, null).getBytes());
							bos.flush();
							stopReq.set(true);
							break;
						case OTHER:
							throw new RuntimeException("Unknown RTSP request method " + req);
					}
					bos.flush();
				}
				
				s.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				if(currentStreamWorker!=null) streamWorkers.removeWorker(currentStreamWorker.getId());
				writer.writeInfo("Connection closed: " + s.getInetAddress());
				isRunning.set(false);
				removeHandler(this.id);
			}
		}
		
		public int getId() {
			return id;
		}
		
		public long getLastRequestTimestamp() {
			return lastRequestTimestamp;
		}
		
		public H264RtpStreamWorker getCurrentStreamWorker() {
			return currentStreamWorker;
		}
		
		public Socket getSocket() {
			return s;
		}
		
		@Override
		public String toString() {
			return id + " " + s.getInetAddress().getHostAddress();
		}
		
	}

}
