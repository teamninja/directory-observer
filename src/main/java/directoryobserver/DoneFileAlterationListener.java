package directoryobserver;

import org.apache.commons.codec.digest.*;
import org.apache.commons.io.monitor.*;
import org.apache.log4j.*;

import java.io.*;

final class DoneFileAlterationListener implements FileAlterationListener
{
	private final NewFileListener newFileListener;
	private Logger log = Logger.getLogger(DoneFileAlterationListener.class);
	
	public DoneFileAlterationListener(NewFileListener newFileListener)
	{
		this.newFileListener = newFileListener;
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

            if(doneFile.exists())
            {
                String[] tokens = doneFile.getName().split("\\.");
                if (tokens.length != 3)
                {
                    newFileListener.onError(doneFile, new WrongDoneFileName());
                    return;
                }

                String name = tokens[0];
                String md5 = tokens[1];

                File newFile = new File(doneFile.getParent() + File.separator + name);

                String calculatedMd5 = DigestUtils.md5Hex(new FileInputStream(newFile));

                if (calculatedMd5.equals(md5))
                {
                    newFileListener.onNewFile(newFile);
                }
                else
                {
                    newFileListener.onChecksumMismatch(newFile, doneFile);
                }
            }
            else if(!DirectoryObserver.isStarting)
            {
                newFileListener.onError(doneFile, new DoneFileException());
            }
		}
		catch (Exception e)
		{
			newFileListener.onError(doneFile, e);
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