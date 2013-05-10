directory-observer
==================

A library for observing a directory and getting notified when a new file is added

Usage
-----

### On the file system side...
Write a file in your observed directory
```bash
~/observable $ echo hello > new_file
~/observable $ ls
new_file
```
Touch the *.done file
```bash
~/observable $ md5sum new_file 
b1946ac92492d2347c6235b4d2611184  new_file
touch new_file.b1946ac92492d2347c6235b4d2611184.done
```
### In the Java code...
The _onNewFile()_ callback will be invoked if _new\_file_ exists and the checksum match with the one in the _*.done_ file name

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
