package restartServer;

import hollenstein.MovieCopier;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.Properties;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;



public class TwonkyMediaServer extends Thread {

	private static Logger log = Logger.getLogger(TwonkyMediaServer.class);
	
	private static String ip;
	private static String login;
	private static String password;
	
	private static ProgressWindow progressWindow;
	
	
	/**
	 * Creating a TwonkyMediaServer is not allowed
	 */
	private TwonkyMediaServer(){
	}
	
	
	public static void restart(){
		new TwonkyMediaServer().start();
	}
	
	
	public void run(){
		main();
	}
	
	
	/**
	 * Only one thread is allowed within the whole restart-procedure
	 */
	private static synchronized void main(){
		log.info("----- Restarting Twonkymediaserver -----");
		progressWindow = new ProgressWindow();

		readProperties();
		Connection connection = new Connection(ip);
		Session session = null;
		try {
			progressWindow.setNote("connecting...");
			connection.connect();
			connection.authenticateWithPassword(login, password);
			session = connection.openSession();
			
			progressWindow.setNote("Stopping server...");
			log.info("invoke twonkymedia.sh stop");
			session.execCommand("sh /etc/init.d/twonkymedia.sh stop");
			showOutput(session);
			session.close();
			
			session = connection.openSession();
			progressWindow.setNote("Starting server...");
			log.info("invoke twonkymedia.sh start");
			session.execCommand("sh /etc/init.d/twonkymedia.sh start");
			showOutput(session);
			
			progressWindow.setNote("Finished");
			
		} catch (IOException e) {
			log.error("", e);
			progressWindow.appendText(e.getMessage());
			progressWindow.setNote("Es isch en Fehler passiert!");
		} finally {
			progressWindow.stopProgressBar();
			if (session != null)
				session.close();
			connection.close();
		}
	}
	
	
	private static void readProperties(){
		Properties prop = new Properties();
		Reader reader;
		try {
			reader = new FileReader(MovieCopier.INIFILE);
			prop.load(reader);
			ip = prop.getProperty("NASIP");
			login = prop.getProperty("NASLogin");
			password = prop.getProperty("NASPassword");
			
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(null, "filmcopier.ini could not be found: " + MovieCopier.INIFILE);
			System.exit(0);
		} catch (IOException e) {
			log.error("", e);
		}
	}
	
	
	private static void showOutput(Session session) throws IOException{
		InputStream stdout = session.getStdout();
		InputStream stderr = session.getStderr();
		
		byte[] buffer = new byte[8192];
		
		while (true){
			if ((stdout.available() == 0) && (stderr.available() == 0)){
				/* Even though currently there is no data available, it may be that new data arrives
				 * and the session's underlying channel is closed before we call waitForCondition().
				 * This means that EOF and STDOUT_DATA (or STDERR_DATA, or both) may
				 * be set together.
				 */
				int conditions = session.waitForCondition(ChannelCondition.STDOUT_DATA |
						ChannelCondition.STDERR_DATA | ChannelCondition.EOF | ChannelCondition.EXIT_STATUS, 10000);
				
				if ((conditions & ChannelCondition.TIMEOUT) != 0){
					/* A timeout occured. */
//					throw new IOException("Timeout while waiting for data from peer.");
				}
				if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0){
					/* ... and we have consumed all data in the local arrival window. */
					break;
				}
				/* OK, either STDOUT_DATA or STDERR_DATA (or both) is set. */
			}

			StringBuilder strBuilder = new StringBuilder();
			
			while (stdout.available() > 0){
				int len = stdout.read(buffer);
				buffer = Arrays.copyOf(buffer, len);
				strBuilder.append(new String(buffer));
			}
			if (strBuilder.length() > 0){
				log.info(strBuilder);
				progressWindow.appendText(strBuilder.toString());
			}

			strBuilder = new StringBuilder();
			while (stderr.available() > 0){
				int len = stderr.read(buffer);
				buffer = Arrays.copyOf(buffer, len);
				strBuilder.append(new String(buffer));
			}
			if (strBuilder.length() > 0){
				log.error(strBuilder);
				progressWindow.appendText(strBuilder.toString());
			}
			
		}
		stdout.close();
		stderr.close();
	}

}
