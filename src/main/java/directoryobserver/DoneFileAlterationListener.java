package main.java.directoryobserver;

import java.io.File;
import java.io.FileInputStream;
import java.util.function.Consumer;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.log4j.*;

final class DoneFileAlterationListener implements FileAlterationListener
{
	private final Consumer<File> newFileListener;
    private ChecksumMismatchListener checksumMismatchListener;
    private ErrorListener errorListener;
    private Logger log = Logger.getLogger(DoneFileAlterationListener.class);
	
	public DoneFileAlterationListener(Consumer<File> newFileListener, ChecksumMismatchListener checksumMismatchListener, ErrorListener errorListener)
	{
		this.newFileListener = newFileListener;
        this.checksumMismatchListener = checksumMismatchListener;
        this.errorListener = errorListener;
    }
	
	@Override
	public void onStop(FileAlterationObserver observer)
	{
	}

	@Override
	public void onStart(FileAlterationObserver observer)
	{
	}

	@Override
	public void onFileDelete(File file)
	{
	}

	@Override
	public void onFileCreate(File doneFile)
	{
		try
		{
			log.info("Received done file " + doneFile.getName());
			
			String[] tokens = doneFile.getName().split("\\.");
			if (tokens.length != 3)
			{
                if (errorListener != null)
                    errorListener.onError(doneFile, new WrongDoneFileName());

				return;
			}
			
			String name = tokens[0];
			String md5 = tokens[1];
			
			File newFile = new File(doneFile.getParent() + File.separator + name);
			
			String calculatedMd5 = DigestUtils.md5Hex(new FileInputStream(newFile));
			
			if (calculatedMd5.equals(md5))
			{
                if (newFileListener != null)
				    newFileListener.accept(newFile);
			}
			else
			{
                if (checksumMismatchListener != null)
                    checksumMismatchListener.onChecksumMismatch(newFile, doneFile);
			}
		}
		catch (Exception e)
		{
            if (errorListener != null)
                errorListener.onError(doneFile, e);
		}
		finally
		{
			doneFile.delete();
		}
	}

	@Override
	public void onFileChange(File file)
	{
	}

	@Override
	public void onDirectoryDelete(File directory)
	{
	}

	@Override
	public void onDirectoryCreate(File directory)
	{
	}

	@Override
	public void onDirectoryChange(File directory)
	{
	}
}