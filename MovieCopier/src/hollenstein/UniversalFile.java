package hollenstein;

import java.io.File;
import java.net.MalformedURLException;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;


public class UniversalFile {
	
	private File file;
	private SmbFile smbFile;
	
	
	public UniversalFile(String path) throws MalformedURLException{
		if (path.startsWith("smb")){
			this.smbFile = new SmbFile(path);
		} else {
			this.file = new File(path);
		}
	}
	
	public UniversalFile(File file){
		this.file = file;
	}
	
	public UniversalFile(SmbFile smbFile){
		this.smbFile = smbFile;
	}
	
	
	public boolean isSmbFile(){
		return smbFile != null;
	}
	
	
	public String getAbsolutePath(){
		if (file != null){
			return file.getAbsolutePath();
		} else {
			return smbFile.getPath();
		}
	}
	
	
	public String getName(){
		if (file != null){
			return file.getName();
		} else {
			return smbFile.getName();
		}
	}
	
	
	public boolean setLastModified(long l){
		if (file != null){
			return file.setLastModified(l);
		} else {
			try {
				smbFile.setLastModified(l);
			} catch (SmbException e) {
				return false;
			}
			return true;
		}
	}
	
	
	public long lastModified(){
		if (file != null){
			return file.lastModified();
		} else {
			try {
				return smbFile.lastModified();
			} catch (SmbException e) {
				return 0;
			}
		}
	}
	
	
	public boolean renameTo(String fileName) throws SmbException, MalformedURLException{
		if (file != null){
			return file.renameTo(new File(fileName));
		} else {
			smbFile.renameTo(new SmbFile(fileName));
			return true;
		}
	}
	
	
	public boolean exists() throws SmbException{
		if (file != null){
			return file.exists();
		} else {
			return smbFile.exists();
		}
	}
	
	
	public long length() throws SmbException{
		if (file != null){
			return file.length();
		} else {
			return smbFile.length();
		}
	}
	
	
	public boolean delete() throws SmbException{
		if (file != null){
			return file.delete();
		} else {
			smbFile.delete();
			return true;
		}
	}
}
