directory-observer
==================

A library for observing a directory and getting notified when a new file is added

Usage
-----

Write a file _myFile_ in _myDir_ then write the file _myFile.md5sumOfMyFile.done_ in the same directory.

The _onNewFile_ callback will be invoked if _myFile_ exists and the checksum match with the one in the _*.done_ file

```java
DirectoryObserver observer = new DirectoryObserver(new File("myDir"));

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
