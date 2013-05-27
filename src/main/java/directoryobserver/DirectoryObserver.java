package directoryobserver;

import org.apache.commons.io.*;
import org.apache.commons.io.filefilter.*;
import org.apache.commons.io.monitor.*;
import org.apache.log4j.*;

import java.io.*;

public class DirectoryObserver
{
    private final IOFileFilter doneFileFilter;
    private FileAlterationObserver observer;
	private Logger log = Logger.getLogger(DirectoryObserver.class);
    private File directory;

    public DirectoryObserver(File directory)
	{
        this.directory = directory;
        doneFileFilter = FileFilterUtils.suffixFileFilter(".done");
        observer = new FileAlterationObserver(directory, doneFileFilter);
	}
	
	public void addListener(NewFileListener listener)
	{
		observer.addListener(new DoneFileAlterationListener(listener));
	}
	
	public void start() throws Exception
	{
		FileAlterationMonitor monitor = new FileAlterationMonitor(500, observer);

        for (FileAlterationListener listener : observer.getListeners())
        {
            for (File doneFile : FileUtils.listFiles(directory, doneFileFilter, FileFilterUtils.directoryFileFilter()))
            {
                listener.onFileCreate(doneFile);
            }
        }

		monitor.start();
		
		log.info("Watching for new files...");
	}
}
