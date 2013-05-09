directory-observer
==================

A library for observing a directory and getting notified when a new file is added

Usage
-----

Write a file myFile in /tmp/xxx then write the file myFile.<md5sumOfMyFile>.done in the same directory 

```java
DirectoryObserver observer = new DirectoryObserver(new File("/tmp/xxx"));

observer.addListener(new NewFileListener()
{
	@Override
	public void onNewFile(File newFile)
	{
		System.out.println("a new file!");
	}

	@Override
	public void onError(File doneFile, Exception e)
	{
		System.out.println("a new error!");
	}

	@Override
	public void onChecksumMismatch(File newFile, File doneFile)
	{
		System.out.println("a new corrupted file!");
	}
});
		
observer.start();
```
