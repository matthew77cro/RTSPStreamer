package hr.matija.rtpStreamer.server;

import hr.matija.rtpStreamer.server.H264RtspReqHandlerCollection.H264RtspReqHandler;

public interface H264RtspReqHandlerCollectionListener {

	void handlerAdded(H264RtspReqHandler handler);
	void handlerRemoved(H264RtspReqHandler handler);
	
}
