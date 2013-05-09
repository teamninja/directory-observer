package alecava;

import java.io.File;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

public class DirectoryObserver
{
	private FileAlterationObserver observer;

	public DirectoryObserver(File directory)
	{
		observer = new FileAlterationObserver(directory, FileFilterUtils.suffixFileFilter(".done"));
	}
	
	public void addListener(NewFileListener listener)
	{
		observer.addListener(new DoneFileAlterationListener(listener));
	}
	
	public void start() throws Exception
	{
		FileAlterationMonitor monitor = new FileAlterationMonitor(10, observer);
		monitor.start();
		
		System.out.println("Watching for new files...");
	}
}
