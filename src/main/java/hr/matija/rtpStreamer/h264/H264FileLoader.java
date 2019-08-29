package hr.matija.rtpStreamer.h264;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Loader of the h264 nal units from a file on disk.
 * @author Matija
 *
 */
public class H264FileLoader implements H264Loader{
	
	private static int kibi64 = 64*1024;
	
	private Path h264File;
	private BufferedInputStream bis;
	
	private NalUnit nextNal;
	
	private boolean firstNal = true;
	
	/**
	 * Initializes the h264 file loader
	 * @param h264File path to the raw h264 file in the annex B format (byte stream of nal units
	 *                 separated by 0x00 00 01, and each access unit separated by the AUD - access unit
	 *                 delimiter; AUD must not be at the start of the file!)
	 * @throws IOException if an I/O error occurs
	 */
	public H264FileLoader(Path h264File)  throws IOException {
		this.h264File = Objects.requireNonNull(h264File);
		
		if(!Files.isRegularFile(h264File) || !Files.isReadable(h264File)) throw new RuntimeException("File is not readable or is not a regular file!");
		bis = new BufferedInputStream(Files.newInputStream(this.h264File));
	}
	

	@Override
	public boolean nextNalUnit() throws IOException {
		
		byte[] data = new byte[kibi64];
		int dataLen = 0;
		
		int state = 0;
outer:	while(true) {
			int next;
			next = bis.read();

			if(next==-1) break;
			
			byte nextByte = (byte) next;
			data[dataLen++] = nextByte;
			switch(state) {
				case 0: if(nextByte==0) state = 1; break;
				case 1: if(nextByte==0) state = 2;
						else state = 0;
						break;
				case 2: if(nextByte!=0 && nextByte!=1) {state = 0; break;}
						if(nextByte==0) {
							next = bis.read(); //eat the next 0x1
							if(next!=1) throw new IllegalStateException("Unexpected byte sequence 0x00 00 00 00");
						}
						dataLen-=3;
						break outer;
			}
		}
		
		if(dataLen==0 && firstNal) {
			firstNal = false;
			return nextNalUnit();
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
	
	@Override
	public void close() throws IOException {
		bis.close();
	}

}
