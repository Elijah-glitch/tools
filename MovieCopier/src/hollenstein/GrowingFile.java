package hollenstein;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.log4j.Logger;



public class GrowingFile extends Thread {

	private static final Logger log = Logger.getLogger(GrowingFile.class);
	
	private File file;
	private String destination;
	private long waitInputStreamAvailable;
	private long waitMaxInputStreamAvailable;
	private long waitForDataAvailable;
	private MovieCopier controller;
	
	public GrowingFile(File file, String destination, long waitInputStreamAvailable, long waitMaxInputStreamAvailable,
					   long waitForDataAvailable, MovieCopier controller){
		this.file = file;
		this.waitInputStreamAvailable = waitInputStreamAvailable;
		this.waitMaxInputStreamAvailable = waitMaxInputStreamAvailable;
		this.waitForDataAvailable = waitForDataAvailable;
		this.destination = destination;
		this.controller = controller;
	}
	
	public void run(){
		if (isInputStreamAvailable()){
			new Copier().copy(file, destination, waitForDataAvailable);
			refreshMediaServer();
		} else {
			log.info("Could not get InputStream: timeout!");
		}
		controller.cleanupFile(file);
	}
	
	
	private boolean isInputStreamAvailable(){
		long timerStart = System.currentTimeMillis();
		long timer = timerStart;
		while ((timer - timerStart) < waitMaxInputStreamAvailable){
			try {
				InputStream is = new FileInputStream(file);
				is.close();
				log.debug("InputStream available.");
				return true;
			} catch (FileNotFoundException e) {
				try {
					Thread.sleep(waitInputStreamAvailable);
					timer = System.currentTimeMillis();
				} catch (InterruptedException e1) {
				}
			} catch (IOException e) {
			}
		}
		return false;
	}

	
	private void refreshMediaServer(){
		try {
			URL url = new URL("http://nas-mgsh:9000/rpc/rescan");
			url.openConnection();
			log.info("Rescan of Twonkymedia Server initiated.");
		} catch (IOException e) {
			log.error("", e);
		}
	}
	
}
