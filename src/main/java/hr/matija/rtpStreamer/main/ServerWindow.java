package hr.matija.rtpStreamer.main;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import hr.matija.rtpStreamer.server.H264RtspServer;

@SuppressWarnings("serial") 
public class ServerWindow extends JFrame {

		public static final double screenWidth = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		public static final double screenHeight = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
		public static final int ppi = Toolkit.getDefaultToolkit().getScreenResolution();
		public static final double ratio = screenWidth/screenHeight;
		
		public static final double widthInch = screenWidth/ppi;
		public static final double heightInch = screenHeight/ppi;
		public static final double diagonal = Math.sqrt(widthInch*widthInch + heightInch*heightInch);

		private int width;
		private int height;
		private String title;
		
		private H264RtspServer server;
		
		private JButton start;
		private JButton stop;
		private JButton reload;
		private JSpinner packageMultiplier;
		private JSpinner packageDrop;
		private JCheckBox allowDropAll;
		private JButton confirmPackageChange;
		
		public ServerWindow(int width, int height, String title, H264RtspServer server) throws HeadlessException {
			this.width = width;
			this.height = height;
			this.title = Objects.requireNonNull(title);
			this.server = Objects.requireNonNull(server);
			
			this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			this.setSize(this.width, this.height);
			this.setTitle(this.title);
			this.setLayout(new BorderLayout());
			
			initGui();
			initListeners();
		}

		private void initGui() {
			
			JLabel banner = new JLabel("RTSP Streamer 1.0");

			// Set the label's font size to the newly determined size.
			banner.setFont(new Font("Times", Font.PLAIN, 30));
			
			this.add(banner, BorderLayout.PAGE_START);
			
			JPanel center = new JPanel(new GridLayout(0, 2));
			
	        packageMultiplier = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
	        packageDrop = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.1));
	        allowDropAll = new JCheckBox("AllowDropAll");
	        allowDropAll.setToolTipText("If unchecked, IDR, SPS, PPS or SEE packages will not be dropped");
	        allowDropAll.setSelected(true);
	        confirmPackageChange = new JButton("Enter");
	        
	        JLabel l1 = new JLabel("Package multiplier: ");
	        l1.setToolTipText("How many times to send each RTP packet. Helps in package dropping");
	        center.add(l1);
	        center.add(packageMultiplier);
	        JLabel l2 = new JLabel("Package drop rate: ");
	        l2.setToolTipText("Percentage of packets to be dropped");
	        center.add(l2);
	        center.add(packageDrop);
	        center.add(new JLabel());
	        center.add(allowDropAll);
	        center.add(new JLabel());
	        center.add(confirmPackageChange);
			
			this.add(center, BorderLayout.CENTER);
			
			JPanel bottom = new JPanel(new GridLayout(1, 0));
			
			start = new JButton("Start");
			stop = new JButton("Stop");
			stop.setEnabled(false);
			reload = new JButton("Reload");
			
			bottom.add(start);
			bottom.add(stop);
			bottom.add(reload);
			
			this.add(bottom, BorderLayout.PAGE_END);
			
		}
		
		private void initListeners() {
			
			start.addActionListener((e) -> {
				start.setEnabled(false);
				reload.setEnabled(false);
				new Thread(() -> {
					server.start();
					stop.setEnabled(true);
				}).start();;
				
			});
			
			stop.addActionListener((e) -> {
				stop.setEnabled(false);
				new Thread(() -> {
					server.stop();
					start.setEnabled(true);
					reload.setEnabled(true);
				}).start();
			});

			reload.addActionListener((e) -> {
				start.setEnabled(false);
				reload.setEnabled(false);
				new Thread(() -> {
					try {
						server.reloadResources();
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(ServerWindow.this, e1.getClass().getName() + " : " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					}
					start.setEnabled(true);
					reload.setEnabled(true);
				}).start();
			});
			
			confirmPackageChange.addActionListener((e) -> {
				server.setPackageDropRate(((SpinnerNumberModel)packageDrop.getModel()).getNumber().doubleValue());
				server.setPackageMultiplier(((SpinnerNumberModel)packageMultiplier.getModel()).getNumber().intValue());
				server.setAllowDropAll(allowDropAll.isSelected());
			});
			
			this.addWindowListener(new WindowAdapter() {				
				@Override
				public void windowClosing(WindowEvent e) {
					start.setEnabled(false);
					stop.setEnabled(false);
					reload.setEnabled(false);
					new Thread(() -> {
						server.stop();
						dispose();
					}).start();
				}
			});
			
		}
		
	}