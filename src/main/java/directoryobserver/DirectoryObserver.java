package main.java.directoryobserver;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.log4j.Logger;

import java.io.File;

public class DirectoryObserver
{
	private FileAlterationObserver observer;
	private Logger log = Logger.getLogger(DirectoryObserver.class);
    private NewFileListener newFileListener;
    private ChecksumMismatchListener checksumMismatchListener;
    private ErrorListener errorListener;

    public DirectoryObserver(File directory)
	{
        observer = new FileAlterationObserver(directory, FileFilterUtils.suffixFileFilter(".done"));
	}
	
	public void setNewFileListener(NewFileListener listener)
	{
        newFileListener = listener;
	}

    public void setChecksumMismatchListener(ChecksumMismatchListener listener)
    {
        checksumMismatchListener = listener;
    }

    public void setErrorListener(ErrorListener listener)
    {
        errorListener = listener;
    }
	
	public void start() throws Exception
	{
        observer.addListener(new DoneFileAlterationListener(newFileListener, checksumMismatchListener, errorListener));

		FileAlterationMonitor monitor = new FileAlterationMonitor(10, observer);
		monitor.start();
		
		log.info("Watching for new files...");
	}
}
