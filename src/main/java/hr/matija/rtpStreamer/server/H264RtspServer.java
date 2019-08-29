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
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Timer;

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
	
	private static byte payloadType = 96;
	private static int clockHz = 90_000;
	private static int ssrc = (int) (Math.random() * Integer.MAX_VALUE);
	
	private Path configFile;
	private int serverPort;
	private Path webroot;
	private Path resourceDescriptor;
	private H264RtspResources resources;
	private H264RtspReqHandlerCollection reqHandlers;
	private H264RtpStreamWorkerCollection workers;
	
	private AtomicBoolean isRunning = new AtomicBoolean(false);
	private AtomicBoolean stopReq = new AtomicBoolean(false);
	
	private Timer cleaner;
	
	/**
	 * Creates and initializes the rtsp server from the config file. 
	 * See the javadoc of this class for more information.	
	 * @param configFilePath
	 * @throws IOException
	 */
	public H264RtspServer(String configFilePath) throws IOException {
		configFile = Paths.get(configFilePath);
		
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
		
		this.resources = new H264RtspResources(resourceDescriptor, webroot);
		this.workers = new H264RtpStreamWorkerCollection(clockHz, payloadType);
		this.reqHandlers = new H264RtspReqHandlerCollection(workers, resources);
		
		cleanerStart();
		printInfo();
	}

	private void printInfo() throws SocketException {
		
		System.out.println("RTSP SERVER");
		System.out.println("Config file: " + configFile.toAbsolutePath());
		
		System.out.println("----------------------------------");
		System.out.println("Server addresses: ");
		var en = NetworkInterface.getNetworkInterfaces();
		while(en.hasMoreElements()) {
			NetworkInterface n = (NetworkInterface) en.nextElement();
			var ee = n.getInetAddresses();
			while (ee.hasMoreElements()) {
				InetAddress i = (InetAddress) ee.nextElement();
				System.out.println(i.getHostAddress());
			}
		}
		System.out.println("----------------------------------");
		
		System.out.println("Server port: " + serverPort);
		System.out.println("Webroot: " + webroot.toAbsolutePath());
		System.out.println("Resource descriptor: " + resourceDescriptor.toAbsolutePath());
		System.out.println();
		System.out.println("--------RESOURCES---------");
		for(var res : resources.getResources()) {
			System.out.println(res);
		}
		System.out.println("--------------------------");
		System.out.println();
	}
	
	public synchronized void start() {
		if(isRunning.get()) throw new IllegalStateException("Stream is already running!");
		isRunning.set(true);
		stopReq.set(false);
		
		new Thread(() -> {
			
			try (ServerSocket socket = new ServerSocket(serverPort)){
				
				System.out.println("Server run");
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
				isRunning.set(false);
				System.out.println("Server stop");
			}
			
		}).start();
	}

	public synchronized void stop() {
		stopReq.set(true);
		while(isRunning.get());
		cleaner.stop();
	}

	public boolean isRunning() {
		return isRunning.get();
	}
	
	public synchronized void reloadResources() throws IOException {
		if(isRunning.get()) throw new RuntimeException("Cannot reload resources while the server is running!");
		resources.loadResources();
	}
	
	public void cleanerStart() {
		cleaner = new Timer(10_000, (e) -> {
			System.out.println("Running connections: " + reqHandlers.getActiveConnectionCount() + "\tRunning workers:" + workers.getActiveWorkerCount());
			List<H264RtspReqHandler> cons = new ArrayList<>(reqHandlers.getAllHandlers());
			for(var connection : cons) {
				if(System.currentTimeMillis() - connection.getLastRequestTimestamp() > 60_000) {
					connection.close();
					reqHandlers.removeHandler(connection.getId());
					System.out.println("Cleaner removed: " + connection.getId() + " " + connection.getSocket().getInetAddress());
				}
			}
		});
		cleaner.start();
	}
	
}
