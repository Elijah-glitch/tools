package hollenstein;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SplashScreen;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import jcifs.smb.SmbException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import restartServer.TwonkyMediaServer;



public class MovieCopier implements ActionListener{
	
	private static Logger log = Logger.getLogger(MovieCopier.class);
	
	public static final String INIFILE = ".\\conf\\moviecopier.ini";
	private static final String LOG4J_PROPERTIES = ".\\conf\\log4j.properties";
	private static final String TRAYICON = ".\\resource\\nemo_small.png";

	private File watchingDirectory;
	private String copyDestination;
	private long waitInputStreamAvailable;
	private long waitMaxInputStreamAvailable;
	private long waitForDataAvailable = 10000; // in ms
	private boolean includeSubDirectories;
	
	private Map<File, GrowingFile> files = new HashMap<File, GrowingFile>();
	
	
	private MovieCopier(){
	}
	
	
	private void init(){
		readProperties();
		createSystemTray();
		DirectoryWatcher watcher = new DirectoryWatcher(watchingDirectory.getAbsolutePath(), includeSubDirectories, this);
		
		SplashScreen splash = SplashScreen.getSplashScreen();
		if (splash != null) splash.close();
		
		watcher.startWatch();
	}
	
	private void readProperties(){
		Properties prop = new Properties();
		Reader reader;
		try {
			reader = new FileReader(INIFILE);
			prop.load(reader);
			watchingDirectory = new File(prop.getProperty("watchingDir"));
			copyDestination = prop.getProperty("copyDestinationDir");
			waitInputStreamAvailable = Long.valueOf(prop.getProperty("waitInputStreamAvailable"));
			waitMaxInputStreamAvailable = Long.valueOf(prop.getProperty("waitMaxInputStreamAvailable"));
			waitForDataAvailable = Long.valueOf(prop.getProperty("waitForDataAvailable"));
			includeSubDirectories = Boolean.valueOf(prop.getProperty("includeSubDirectories"));
			log.info("Properties successfully read from: " + INIFILE);
			
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(null, "filmcopier.ini could not be found: " + INIFILE);
			System.exit(0);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "error reading filmcopier.ini");
			System.exit(0);
		}
	}
	
	
	private void createSystemTray(){
		SystemTray tray = SystemTray.getSystemTray();
		File file = new File(TRAYICON);
		try {
			Image img = ImageIO.read(file);
			TrayIcon icon = new TrayIcon(img);
			tray.add(icon);
			icon.setToolTip("MovieCopier");
			PopupMenu popupmenu = new PopupMenu();
			popupmenu.addActionListener(this);
			MenuItem menuitem = new MenuItem("Exit MovieCopier");
			popupmenu.add(menuitem);
			menuitem = new MenuItem("Restart Twonkymedia Server");
			popupmenu.add(menuitem);
			icon.setPopupMenu(popupmenu);
			
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "icon for tray could not be found: " + TRAYICON);
		} catch (AWTException e) {
			log.error("", e);
		}
		
	}
	
	
	public void fileCreated(File file){
		log.info("File created: " + file.getAbsolutePath());
		Util.checkDestinationExists(copyDestination);
		createGrowingFile(file);
	}
	
	
	public void fileModified(File file){
		GrowingFile t = files.get(file);
		if (t == null){
			log.info("File modified: " + file.getName() + ", no GrowingFile yet, creating a new one.");
			Util.checkDestinationExists(copyDestination);
			createGrowingFile(file);
		}
	}
	
	public void fileDeleted(File file){
		log.info("File deleted: " + file.getAbsolutePath());
		files.remove(file);
	}
	
	
	public void fileRenamed(File from, File to){
		log.info("File Renamed: " + from.getAbsolutePath() + " --> " + to.getName());
		
		Util.checkDestinationExists(copyDestination);
		
		UniversalFile sourceFile = new UniversalFile(to);
		
		String fileAtDestinationString = Util.getPathWithSeparator(copyDestination, from.getName());
		try {
			UniversalFile fileAtDestination = new UniversalFile(fileAtDestinationString);
			if (Util.filesAreEqual(sourceFile, fileAtDestination)){
				if (fileAtDestination.renameTo(Util.getPathWithSeparator(copyDestination, to.getName()))){
					JOptionPane.showMessageDialog(null, "S'File isch au im Ordner " + copyDestination + " umbenännt worde.");
				}
			} else {
				log.info("   File '" + from.getName() + "' doesn't exist in directory " + copyDestination + ".");
				createGrowingFile(to);
			}
		} catch (MalformedURLException e) {
			log.error("Something's wrong about the file at the destination!", e);
		} catch (SmbException e) {
			log.error("Renaming file at destination failed!", e);
		}	
	}
	
	
	private void createGrowingFile(File file){
		GrowingFile growingFile = new GrowingFile(file, copyDestination, waitInputStreamAvailable,
				  								  waitMaxInputStreamAvailable, waitForDataAvailable, this);
		growingFile.start();
		files.put(file, growingFile);
	}
	
	
	/**
	 * Call this when the GrowingFile has ceased to live.
	 * @param file
	 */
	public void cleanupFile(File file){
		log.debug("Cleaned up file '" + file);
		files.remove(file);
	}
	
	
		@Override
	public void actionPerformed(ActionEvent actionevent) {
		String cmd = actionevent.getActionCommand();
		if (cmd.equalsIgnoreCase("Exit FilmCopier")){
			System.exit(0);
		} else if (cmd.equalsIgnoreCase("Restart Twonkymedia Server")) {
			TwonkyMediaServer.restart();
		}
	}
		
		
		
	public static void main(String[] args) {
		PropertyConfigurator.configure(LOG4J_PROPERTIES);
		if (args.length > 0 && args[0].equalsIgnoreCase("-restartTMServer")){
			TwonkyMediaServer.restart();
		} else {
			log.info("************** FilmCopier started ***************");
			new MovieCopier().init();
		}
	}



}
