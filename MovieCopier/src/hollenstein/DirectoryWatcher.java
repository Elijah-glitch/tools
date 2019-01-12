package hollenstein;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import name.pachler.nio.file.ClosedWatchServiceException;
import name.pachler.nio.file.FileSystems;
import name.pachler.nio.file.Path;
import name.pachler.nio.file.Paths;
import name.pachler.nio.file.StandardWatchEventKind;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.WatchService;
import name.pachler.nio.file.WatchEvent.Modifier;
import name.pachler.nio.file.ext.ExtendedWatchEventKind;
import name.pachler.nio.file.ext.ExtendedWatchEventModifier;

public class DirectoryWatcher {
	
	private static Logger log = Logger.getLogger(DirectoryWatcher.class);

	private MovieCopier controller;
	private String path;
	private boolean recursive;
	
	
	public DirectoryWatcher(String path, boolean recursive, MovieCopier controller){
		this.controller = controller;
		this.path = path;
		this.recursive = recursive;;
	}
	
	
	
	public void startWatch(){
		log.info("WatchService started");
		Map<WatchKey, Path> mapKeyPath = new HashMap<WatchKey, Path>();
		WatchService watchService  = FileSystems.getDefault().newWatchService();
		Path watchedPath = Paths.get(path);
		Modifier<?>[] modRecursive = new Modifier[0];
		if (recursive){
			modRecursive = new Modifier[1];
			modRecursive[0] = ExtendedWatchEventModifier.FILE_TREE;
		}
		WatchEvent.Kind<?>[] watchEvents = {StandardWatchEventKind.ENTRY_CREATE,
											StandardWatchEventKind.ENTRY_MODIFY,
											StandardWatchEventKind.ENTRY_DELETE,
											ExtendedWatchEventKind.ENTRY_RENAME_FROM,
											ExtendedWatchEventKind.ENTRY_RENAME_TO};
		try {
			WatchKey watchKey = watchedPath.register(watchService, watchEvents, modRecursive);
			mapKeyPath.put(watchKey, watchedPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		while (true){
			WatchKey signalledKey = null;
			try {
				signalledKey = watchService.take();
			} catch (ClosedWatchServiceException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "WatchService has been closed unexpectedly.");
			} catch (InterruptedException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "WatchService has been interrupted unexpectedly.");
			}
			if (signalledKey != null){
				List<WatchEvent<?>> events = signalledKey.pollEvents();
				signalledKey.reset();
				File renameFrom = null;
				File renameTo = null;
				
				for (WatchEvent<?> we: events){
					Path filename = (Path) we.context();
					String fullPath = mapKeyPath.get(signalledKey).toString() + File.separator +  filename.toString();
					File file = new File(fullPath);
					
					if (we.kind() == StandardWatchEventKind.ENTRY_CREATE){
						if (file.isFile()){
							controller.fileCreated(file);
						}
					} else if (we.kind() == StandardWatchEventKind.ENTRY_MODIFY){
						if (file.isFile()){
							controller.fileModified(file);
						}
					} else if (we.kind() == StandardWatchEventKind.ENTRY_DELETE) {
						controller.fileDeleted(file);
					} else if (we.kind() == ExtendedWatchEventKind.ENTRY_RENAME_FROM){
						renameFrom = file;
					} else if (we.kind() == ExtendedWatchEventKind.ENTRY_RENAME_TO){
						if (file.isFile()){
							renameTo = file;
						}
					}
				}
				if (renameFrom != null && renameTo != null){
					controller.fileRenamed(renameFrom, renameTo);
				}
			}
		}
	}
	
}
