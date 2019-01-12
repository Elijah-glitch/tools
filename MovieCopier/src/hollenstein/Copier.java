package hollenstein;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFileOutputStream;

import org.apache.log4j.Logger;



public class Copier {

	private static final Logger log = Logger.getLogger(Copier.class);
	
	private ConcurrentLinkedQueue<byte[]> queue;
	private ArrayBlockingQueue<byte[]> pool;
	private final int POOL_SIZE = 100;
	private Reader reader;
	private Writer writer;
	private final int BUFFER_SIZE = 32768;
	private boolean finishedReading;
	private long startTimer;
	
	private boolean canceled;
	private boolean error;
	
	
	
	public boolean copy(File file, String destination, long waitForDataAvailable){
		try {
			log.info("Copy started: " + file.getAbsolutePath());
			startTimer = System.currentTimeMillis();
			
			String destinationFileString = Util.getPathWithSeparator(destination, file.getName());
			UniversalFile destinationFile = new UniversalFile(destinationFileString);
			
			queue = new ConcurrentLinkedQueue<byte[]>();
			
			Collection<byte[]> emptyByteArrays = new ArrayList<byte[]>(POOL_SIZE);
			for (int i = 0; i < POOL_SIZE; i++){
				emptyByteArrays.add(new byte[BUFFER_SIZE]);
			}
			pool = new ArrayBlockingQueue<byte[]>(POOL_SIZE, false, emptyByteArrays);
			
			reader = new Reader(file, waitForDataAvailable);
			writer = new Writer(destinationFile, file);
			reader.start();
			writer.start();
			
			reader.join();
			writer.join();
			
			return true;
		
		} catch (MalformedURLException e) {
			log.error("", e);
			return false;
		} catch (InterruptedException e) {
			log.error("", e);
			return false;
		}
	}
	
	
	
	
	public class Reader extends Thread{
		
		private File file;
		private long waitForDataAvailable;
		
		public Reader(File file, long waitForDataAvailable){
			this.file = file;
			this.waitForDataAvailable = waitForDataAvailable;
		}
		
		public void run(){
			byte[] bytes = pool.poll();
			int nrBytes;
			InputStream is = null;
			
			try {
				is = new FileInputStream(file);
				
				while(!canceled && !error){
					nrBytes = is.read(bytes);
					if (nrBytes > -1){
						if (nrBytes < BUFFER_SIZE){
							byte[] bytesCopy = Arrays.copyOf(bytes, nrBytes);
							queue.add(bytesCopy);
						} else {
							queue.add(bytes);
						}
						synchronized(queue){
							queue.notify();
						}
						bytes = pool.take();
					} else {
						Thread.sleep(waitForDataAvailable);
						if (is.available() == 0){
							break;
						}
					}
				}
			} catch (IOException e) {
				log.error("", e);
				error = true;
			} catch (InterruptedException e) {
				// Copying is either canceled or an error has occurred
			} finally {
				finishedReading = true;
				if (is != null){
					try {
						is.close();
					} catch (IOException e) {
						log.error("InputStream could not be closed!", e);
					}
				}
			}
		}
	}
	
	
	public class Writer extends Thread{
		
		private UniversalFile file;
		private File originalFile;
		
		public Writer(UniversalFile file, File originalFile){
			this.file = file;
			this.originalFile = originalFile;
		}
		
		public void run(){
			byte[] bytes;
			long nrBytesWritten = 0;
			long fileSize = 0;
			OutputStream os = null;
			DecimalFormat filesizeFormat = new DecimalFormat("#,##0");
			filesizeFormat.setRoundingMode(RoundingMode.DOWN);
			
			try {
				if (file.isSmbFile()){
					os = new SmbFileOutputStream(file.getAbsolutePath());
				} else {
					os = new FileOutputStream(file.getAbsolutePath());
				}
				fileSize = originalFile.length();
				
				ProgressMonitor progressMonitor = new ProgressMonitor(null, "'" + file.getName() + "' wird kopiert...", " ", 0, 100);

				while ((!finishedReading || !queue.isEmpty()) && !canceled && !error){
					if (queue.isEmpty()){
						synchronized(queue){
							queue.wait(1000);
						}
					}
					while((bytes = queue.poll()) != null){
						os.write(bytes);
						pool.put(bytes);
						nrBytesWritten += bytes.length;
						Float progress = new Float(nrBytesWritten * 100.0 / fileSize);
						if (progress > 100){
							progress = 50f;
						}
						progressMonitor.setProgress(progress.intValue());
						progressMonitor.setNote(filesizeFormat.format(nrBytesWritten / (1024.0 * 1024.0)) + " MB / " +
										filesizeFormat.format(fileSize / (1024.0 * 1024.0)) + " MB");
						if (progressMonitor.isCanceled()){
							if (JOptionPane.showConfirmDialog(null, "Wottsch würkli abbräche?", "Bisch sicher?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
								canceled = true;
								reader.interrupt();
								log.info("Copy canceled");
								break;
							} else {
								progressMonitor = new ProgressMonitor(null, "'" + file.getName() + "' wird kopiert...", " ", 0, 100);
							}
						}
					}
				}
				progressMonitor.close();
				if (!canceled && !error){
					double duration = (System.currentTimeMillis() - startTimer) / 1000.0;
					log.info(("Copy finished (duration: " + duration + "s, filesize: " + nrBytesWritten + ") " + file.getName()));
					JOptionPane.showMessageDialog(null, "'" + file.getName() + "' isch ufs NAS kopiert worde.");
				}
				if (!error)
					return;
				
			} catch (SmbException e) {
				log.error("", e);
			} catch (MalformedURLException e) {
				log.error("", e);
			} catch (UnknownHostException e) {
				log.error("", e);
			} catch (FileNotFoundException e) {
				log.error("", e);
			} catch (IOException e) {
				log.error("", e);
			} catch (InterruptedException e) {
				log.error("", e);
			} finally {
				try {
					if (os != null){
						os.close();
					}
					if (file != null)
						file.setLastModified(originalFile.lastModified());
					if (canceled)
						file.delete();
				} catch (IOException e) {
					log.error("", e);
				}
			}
			error = true;
			reader.interrupt();
			JOptionPane.showMessageDialog(null, "Bim Kopiere isch en Fehler passiert!\n" +
					"Lauft s'NAS no? Wenn nöd, iischalte, und nachher s'File umbenänne,\n" +
					"denn wird's au kopiert.");
		}
	}
	
	
}
