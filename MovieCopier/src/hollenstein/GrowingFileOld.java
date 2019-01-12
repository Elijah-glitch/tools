package hollenstein;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Logger;

public class GrowingFileOld extends Thread{

	private static Logger log = Logger.getLogger(GrowingFileOld.class);
	
	protected long waitAfterModification;
	protected File file;
	protected String destination;
	protected MovieCopier controller;
	
	
	public GrowingFileOld(File file, String destination, long waitAfterModification, MovieCopier controller){
		this.file = file;
		this.waitAfterModification = waitAfterModification;
		this.destination = destination;
		this.controller = controller;
	}
	
	
	public void run(){
		boolean modified;
		boolean sizeChanged;
		boolean fileExists;
		boolean inaccessible;
		
		do {
			modified = false;
			long fileSize = file.length();
			try {
				Thread.sleep(waitAfterModification);
			} catch (InterruptedException e) {
				log.debug("Sleep interrupted, set modified");
				modified = true;
			}
			
			sizeChanged = fileSize != file.length();
			if (sizeChanged){
				log.debug("FileSize changed");
			}
			
			fileExists = file.exists();

			inaccessible = false;
			if (fileExists && !modified && !sizeChanged){
				try {
					FileInputStream fis = new FileInputStream(file);
					fis.close();
				} catch (FileNotFoundException e) {
					inaccessible = true;
				} catch (IOException e) {
					// of fis.close()
				}
				log.debug("Have checked if file is accessible.");
			}
			log.debug("inaccessible = " + inaccessible + ",  fileExists = " + fileExists);
		} while (fileExists && (modified || sizeChanged || inaccessible));
		
		if (fileExists){
//			new Copier().copy(file, destination);
		}
		controller.cleanupFile(file);
		
		refreshMediaServer();
	}

	
	
	public void modified(){
		this.interrupt();
	}

	
	private void refreshMediaServer(){
		try {
			URL url = new URL("http://nas-mgsh:9000/rpc/rescan");
			url.openConnection();
		} catch (IOException e) {
			log.error("", e);
		}
	}
	
}
