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
    FileAlterationMonitor monitor;
    static boolean isStarting = false;

    public DirectoryObserver(File directory)
    {
        this.directory = directory;
        doneFileFilter = FileFilterUtils.suffixFileFilter(".done");
        observer = new FileAlterationObserver(directory, doneFileFilter);
        monitor = new FileAlterationMonitor(500, observer);
    }

    public void addListener(NewFileListener listener)
    {
        observer.addListener(new DoneFileAlterationListener(listener));
    }

    public void start() throws Exception
    {

        isStarting = true;
        monitor.start();
        log.info("Watching for new files...");

        for (FileAlterationListener listener : observer.getListeners())
        {
            for (File doneFile : FileUtils.listFiles(directory, doneFileFilter, FileFilterUtils.directoryFileFilter()))
            {
                listener.onFileCreate(doneFile);
            }
        }
        isStarting = false;
    }

    public void stop() throws Exception
    {
            monitor.stop();
    }
}
