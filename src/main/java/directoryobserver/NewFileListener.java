package directoryobserver;

import java.io.File;

public interface NewFileListener
{	
	public void onNewFile(File newFile);
	public void onChecksumMismatch(File newFile, File doneFile);
	public void onError(File doneFile, Exception e);
}
