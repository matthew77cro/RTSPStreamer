package hr.matija.rtpStreamer.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import hr.matija.rtpStreamer.console.ConsoleWriter;
import hr.matija.rtpStreamer.server.H264RtspReqHandlerCollection.H264RtspReqHandler;

/**
 * Implementation of the rtsp server. This implementation expects that
 * .properties file is ready and path to that file is given in the
 * parameter of the constructor. Webroot directory contains all
 * resources that need to be available for streaming. Also, in the 
 * webroot directory there must exist a resource descriptor which 
 * is a txt file of the following format: <br><br> 
 * <code>
 * 
 * # This is a resource descriptor <br>
 * # It maps resources that server will be serving to the uri paths <br>
 * # <br>
 * # <br>
 * # RESOURCE_UNIQUE_ID	PATH_TO_THE_RESOURCE	URI_MAPPING	FPS <br>
 * # Columns are separated by tabs ('\t') <br>
 * # <br>
 * # <br>
 * # Example:
 * # 1000 sample_1080p_60fps.h264	sampleFULLHD	60
 * # => video named "sample_1080p_60fps.h264" will be mapped to rtsp://serverIP:port/sampleFULLHD
 * #    and resource is a video that needs to be played at 60 fps; id
 * #	 of this resource is 1000
 * # <br>
 * # Also, lines that start with '#' are comments (they will be ignored) <br>
 * 1001 sample_720_60fps.h264	sampleHD60	60 <br>
 * 1002 sample_1080_60fps.h264	sampleFULLHD60	60 <br>
 *  <br>
 * </code>
 * 
 * Also, here is the example of the server config file 
 * (server.properties file) : <br> <br> <code>
 * #Port on which server listens for incoming RTSP requests <br>
 * server.port = <em>port</em> <br>
 * #Path to the webroot <br>
 * server.webroot = <em>path_to_the_webroot_directory</em> <br>
 * #Resource descriptor <br>
 * server.resourceDescriptor = <em>path_to_the_resource_descriptor</em> <br> </code>
 * @author Matija
 *
 */
public class H264RtspServer {
	
	private ConsoleWriter writer;
	
	private static byte payloadType = 96;
	private static int clockHz = 90_000;
	private static int ssrc = (int) (Math.random() * Integer.MAX_VALUE);
	
	private Path configFile;
	private int serverPort;
	private Path webroot;
	private Path resourceDescriptor;
	private H264RtspResourceCollection resources;
	private H264RtspReqHandlerCollection reqHandlers;
	private H264RtpStreamWorkerCollection workers;
	
	private AtomicBoolean isRunning = new AtomicBoolean(false);
	private AtomicBoolean stopReq = new AtomicBoolean(false);
	
	/**
	 * Creates and initializes the rtsp server from the config file. 
	 * See the javadoc of this class for more information.	
	 * @param configFilePath
	 * @throws IOException
	 */
	public H264RtspServer(String configFilePath, ConsoleWriter writer) throws IOException {
		this.configFile = Paths.get(configFilePath);
		this.writer = Objects.requireNonNull(writer);
		
		Properties prop = new Properties();
		InputStream in = Files.newInputStream(configFile);
		prop.load(in);
		
		try {
			serverPort = Integer.parseInt(prop.getProperty("server.port"));
		} finally {
			in.close();
		}
			
		webroot = Paths.get(prop.getProperty("server.webroot"));
		if(!Files.isDirectory(webroot)) {
			in.close();
			throw new RuntimeException("server.webroot must be a path to a directory!");
		}
		
		resourceDescriptor = Paths.get(prop.getProperty("server.resourceDescriptor"));
		if(!Files.isRegularFile(resourceDescriptor) || !Files.isReadable(resourceDescriptor)) {
			in.close();
			throw new RuntimeException("server.resourceDescriptor must be a regular file and must be readable!");
		}
		
		in.close();
		
		this.resources = new H264RtspResourceCollection(resourceDescriptor, webroot);
		this.workers = new H264RtpStreamWorkerCollection(clockHz, payloadType, writer);
		this.reqHandlers = new H264RtspReqHandlerCollection(workers, resources, writer);
	}

	public void printInfo() throws SocketException {
		
		writer.writeInfo("RTSP SERVER");
		writer.writeInfo("Config file: " + configFile.toAbsolutePath());
		writer.writeln("");
		
		writer.writeInfo("Server addresses: ");
		var en = NetworkInterface.getNetworkInterfaces();
		while(en.hasMoreElements()) {
			NetworkInterface n = (NetworkInterface) en.nextElement();
			var ee = n.getInetAddresses();
			while (ee.hasMoreElements()) {
				InetAddress i = (InetAddress) ee.nextElement();
				writer.writeInfo(i.getHostAddress());
			}
		}
		writer.writeln("");
		
		writer.writeInfo("Server port: " + serverPort);
		writer.writeInfo("Webroot: " + webroot.toAbsolutePath());
		writer.writeInfo("Resource descriptor: " + resourceDescriptor.toAbsolutePath());
		writer.writeln("");
		writer.writeInfo("RESOURCES");
		writer.writeInfo("--------------------------");
		for(var res : resources.getResources()) {
			writer.writeInfo(res.toString());
		}
		writer.writeInfo("--------------------------");
		writer.writeln("");
	}
	
	public synchronized void start() {
		if(isRunning.get()) throw new IllegalStateException("Stream is already running!");
		isRunning.set(true);
		stopReq.set(false);
		
		new Thread(() -> {
			
			Timer cleaner = new Timer("cleaner", true);
			cleaner.schedule(new TimerTask() {
				@Override
				public void run() {
					clean.accept(60_000);
				}
			}, 0, 10_000);
			
			try (ServerSocket socket = new ServerSocket(serverPort)){
				
				writer.writeInfo("Server run");
				socket.setSoTimeout(5000);
				while(!stopReq.get()) {
					Socket s;
					try {
						s = socket.accept();
					} catch (SocketTimeoutException ex) { continue; }
					H264RtspReqHandler old = this.reqHandlers.getHandler(s.getInetAddress());
					if(old!=null) old.close();
					H264RtspReqHandler handler = this.reqHandlers.getHandler(this.reqHandlers.makeHandler(s, ssrc));
					new Thread(handler).start();
				}
				this.reqHandlers.closeAllConections();
				
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				cleaner.cancel();
				clean.accept(0);
				isRunning.set(false);
				writer.writeInfo("Server stop");
			}
			
		}).start();
	}

	public synchronized void stop() {
		stopReq.set(true);
		while(isRunning.get());
	}

	public boolean isRunning() {
		return isRunning.get();
	}
	
	public synchronized void reloadResources() throws IOException {
		if(isRunning.get()) throw new RuntimeException("Cannot reload resources while the server is running!");
		resources.loadResources();
		writer.writeInfo("RELOADED RESOURCES");
		writer.writeInfo("--------RESOURCES---------");
		for(var res : resources.getResources()) {
			writer.writeInfo(res.toString());
		}
		writer.writeInfo("--------------------------");
	}
	
	private Consumer<Integer> clean = (d) -> {
		int connectionC = reqHandlers.getActiveConnectionCount();
		int workerC = workers.getActiveWorkerCount();
		if(connectionC==0 && workerC==0) return;
		
		writer.writeInfo("Running connections: " + connectionC + "\tRunning workers:" + workerC);
		List<H264RtspReqHandler> cons = new ArrayList<>(reqHandlers.getAllHandlers());
		for(var connection : cons) {
			if(System.currentTimeMillis() - connection.getLastRequestTimestamp() > d) {
				connection.close();
				reqHandlers.removeHandler(connection.getId());
				writer.writeInfo("Cleaner removed: " + connection.getId() + " " + connection.getSocket().getInetAddress());
			}
		}
	};

	public void setPackageDropRate(double dropRate) {
		reqHandlers.getStreamWorkerCollection().setPackageDropRate(dropRate);
	}

	public void setPackageMultiplier(int multiplier) {
		reqHandlers.getStreamWorkerCollection().setPackageMultiplier(multiplier);
	}

	public void setAllowDropAll(boolean value) {
		reqHandlers.getStreamWorkerCollection().setAllowDropAll(value);
	}
	
	public void setOnlyRetransmitImportant(boolean value) {
		reqHandlers.getStreamWorkerCollection().setOnlyRetransmitImportant(value);
	}
	
	public H264RtspReqHandlerCollection getReqHandlers() {
		return reqHandlers;
	}
	
	public H264RtspResourceCollection getResources() {
		return resources;
	}
	
}
