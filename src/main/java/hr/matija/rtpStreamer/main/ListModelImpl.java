package hr.matija.rtpStreamer.main;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import hr.matija.rtpStreamer.server.H264RtspReqHandlerCollection.H264RtspReqHandler;
import hr.matija.rtpStreamer.server.H264RtspReqHandlerCollectionListener;
import hr.matija.rtpStreamer.server.H264RtspServer;

public class ListModelImpl implements ListModel<H264RtspReqHandler>, H264RtspReqHandlerCollectionListener {
	
	List<H264RtspReqHandler> list = new ArrayList<>();
	Set<ListDataListener> listeners = new HashSet<>();
	
	public ListModelImpl(H264RtspServer server) {
		server.getReqHandlers().addListener(this);
	}

	@Override
	public int getSize() {
		return list.size();
	}

	@Override
	public H264RtspReqHandler getElementAt(int index) {
		return list.get(index);
	}

	@Override
	public void addListDataListener(ListDataListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListDataListener(ListDataListener l) {
		listeners.remove(l);
	}

	@Override
	public void handlerAdded(H264RtspReqHandler elem) {
		int position = list.size();
		list.add(elem);
		for(var l : listeners) {
			l.intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, position, position));
		}
	}

	@Override
	public void handlerRemoved(H264RtspReqHandler elem) {
		list.remove(elem);
		int position = list.size();
		for(var l : listeners) {
			l.intervalRemoved(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, position, position));
		}
	}

}
