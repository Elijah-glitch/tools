package hollenstein;

import java.io.File;
import java.net.MalformedURLException;

import javax.swing.JOptionPane;

import jcifs.smb.SmbException;

import org.apache.log4j.Logger;

public class Util {

	private static Logger log = Logger.getLogger(Util.class);
	
	
	
	public static void checkDestinationExists(String path){
		boolean showWarning = false;
		try {
			if (!new UniversalFile(path).exists()){
				showWarning = true;
			}
		} catch (SmbException e) {
			showWarning = true;
		} catch (MalformedURLException e) {
			log.error("", e);
			JOptionPane.showMessageDialog(null, "Evtl. falschi Aagabe im ini-File!");
		}
		if (showWarning){
			JOptionPane.showMessageDialog(null, "Bitte prüef nah, ob s'NAS lauft.\n" +
					"Wenn's lauft, uf OK drucke.\n" + 
					"(Warte, bis es piepst.)", "NAS nöd erreichbar!", JOptionPane.WARNING_MESSAGE, null);
		}
	}
	
	
	public static boolean filesAreEqual(UniversalFile file1, UniversalFile file2){
		try {
			if (!file1.exists() || !file2.exists()){
				return false;
			} else if (file1.length() != file2.length()){
				return false;
			} else {
				// Differences of less or equal 2 seconds are treated equal.
				long difference = Math.abs(file1.lastModified() - file2.lastModified());
				if ((difference / 1000.0) > 2.0){
					return false;
				}
			}
		} catch (SmbException e){
			log.error("", e);
			return false;
		}
		return true;
	}

	
	/**
	 * @param path if no separator is found at the end, one will be added
	 * @param fileName
	 * @return
	 */
	public static String getPathWithSeparator(String path, String fileName){
		if (path.endsWith("\\") || path.endsWith("/")){
			 return path + fileName;
		} else {
			if (path.startsWith("smb")) {
				return path + "/" + fileName;
			} else {
				return path + File.separator + fileName;
			}
		}
	}
	
}
