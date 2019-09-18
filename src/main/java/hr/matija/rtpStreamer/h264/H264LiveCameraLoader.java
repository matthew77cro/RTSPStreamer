package hr.matija.rtpStreamer.h264;

import java.io.IOException;
import java.io.InputStream;

import hr.matija.rtpStreamer.h264.NalUnit.NalUnitType;

public class H264LiveCameraLoader implements H264Loader {
	
	private static int kibi64 = 64*1024;
	
	private NalUnit nextNal;
	private boolean firstNal = true;
	
	private ProcessBuilder pb;
	private Process currentProcess;
	private InputStream is;
	
	public H264LiveCameraLoader(String cameraName) throws IOException {
		pb = new ProcessBuilder(("ffmpeg -f dshow -i video=\"" + cameraName + "\" "
				+ "-c:v libx264 -preset ultrafast -tune zerolatency -x264opts aud=1 -bsf "
				+ "h264_mp4toannexb -an -f h264 pipe:1").split(" "));
		currentProcess = pb.start();
		is = currentProcess.getInputStream();
	}

	@Override
	public void close() throws Exception {
		if(is!=null) is.close();
		if(currentProcess!=null) currentProcess.destroy();
	}

	@Override
	public boolean nextNalUnit() throws IOException {
		
		byte[] data = new byte[kibi64];
		int dataLen = 0;
		
		int state = 0;
outer:	while(true) {
	
			int next;
			next = is.read();
	
			if(is.available()==0) {
				System.out.println("change");
				is.close();
				currentProcess.destroy();
				try {
					currentProcess.waitFor();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				currentProcess = pb.start();
				is = currentProcess.getInputStream();
			}

			if(next==-1) break;
			
			byte nextByte = (byte) next;
			data[dataLen++] = nextByte;
			switch(state) {
				case 0: if(nextByte==0) state = 1; break;
				case 1: if(nextByte==0) state = 2;
						else state = 0;
						break;
				case 2: if(nextByte!=0 && nextByte!=1) {state = 0; break;}
						if(nextByte==0) next = is.read(); //eat the next 0x1
						dataLen-=3;
						break outer;
			}
		}
		
		if(dataLen==0 && firstNal) {
			firstNal = false;
			boolean ret = nextNalUnit();
			if(ret==false) return false;
			if(getNalUnit().getType() == NalUnitType.ACCESS_UNIT_DELIMITER) return nextNalUnit();
			return true;
		}
		firstNal = false;
		
		if(dataLen!=0) {
			nextNal = new NalUnit(data, dataLen);
			return true;
		} else {
			nextNal = null;
			return false;
		}
		
	}

	@Override
	public NalUnit getNalUnit() {
		if(nextNal==null) throw new IllegalStateException("No NAL units to return!");
		return nextNal;
	}

}
