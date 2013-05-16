package test.java;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;


import main.java.directoryobserver.DirectoryObserver;
import main.java.directoryobserver.NewFileListener;
import main.java.directoryobserver.WrongDoneFileName;
import org.apache.commons.codec.digest.*;
import org.apache.commons.io.*;
import org.apache.log4j.*;
import org.junit.*;

public class DirectoryObserverTest
{
	private Path tempDir;
	private Object lock = new Object();
	private String myFileName = "myfile";
	private DirectoryObserver observer;
	private AtomicBoolean callbackExecuted;
	
	@Before
	public void before() throws IOException
	{
		BasicConfigurator.configure();
		
		tempDir = Files.createTempDirectory("observable");
		observer = new DirectoryObserver(tempDir.toFile());
		callbackExecuted = new AtomicBoolean(false);
	}
	
	@After
	public void after() throws IOException
	{
		FileUtils.deleteDirectory(tempDir.toFile());
		assertTrue(callbackExecuted.get());
	}
	
	private void notifyFromCallback()
	{
		callbackExecuted.set(true);
		
		synchronized (lock)
		{
			lock.notify();
		}
	}
	
	private void waitForCallback() throws InterruptedException
	{
		synchronized (lock)
		{
			lock.wait(1000);
		}
	}
	
	@Test
	public void testMultipleNewFilesAllIsOk() throws Exception
	{
		final ConcurrentLinkedDeque<String> newFiles = new ConcurrentLinkedDeque<String>();

        observer.setNewFileListener((newFile) -> {newFiles.add(newFile.getName()); notifyFromCallback();});

        observer.setChecksumMismatchListener((newFile, doneFile) -> fail());
        observer.setErrorListener((doneFile, e) -> fail());
		
		observer.start();
		
		String[] newFileNames = new String[]{"f1", "f2", "f3"};
		List<Path> doneFiles = new ArrayList<Path>();
		
		for (String newFileName : newFileNames)
		{
			Path newFile = Files.createFile(tempDir.resolve(newFileName));
			IOUtils.write(newFileName, new FileWriter(newFile.toFile()));
			
			String md5 = DigestUtils.md5Hex(new FileInputStream(newFile.toFile()));
			Path doneFile = tempDir.resolve(newFileName + "." + md5 + ".done");
			
			doneFiles.add(doneFile);
			Files.createFile(doneFile);
		}
		
		synchronized (lock)
		{
			while (newFiles.size() < 3)
			{
				lock.wait(1000);
			}
		}
		
		for (String newFileName : newFileNames)
		{
			assertTrue(newFiles.contains(newFileName));
		}
		
		for (Path doneFile : doneFiles)
		{
			assertFalse(doneFile.toFile().exists());
		}
	}
	
	@Test
	public void testChecksumError() throws Exception
	{
        observer.setNewFileListener(f -> fail());
        observer.setChecksumMismatchListener((newFile, doneFile) -> notifyFromCallback());
        observer.setErrorListener((doneFile, e) -> fail());
		
		observer.start();
		
		Path newFile = Files.createFile(tempDir.resolve(myFileName));
		IOUtils.write("ciao", new FileWriter(newFile.toFile()));
		
		Files.createFile(tempDir.resolve(myFileName + ".fakemd5.done"));
		
		waitForCallback();
	}
	
	@Test
	public void testErrorBecauseDoneFileDoesntHaveNewFile() throws Exception
	{
		testForWrongDoneFile(myFileName + ".fakemd5", FileNotFoundException.class);
	}
	
	@Test
	public void testErrorDoneFileWithWrongFileName() throws Exception
	{
		testForWrongDoneFile("mywrong", WrongDoneFileName.class);
	}
	
	private void testForWrongDoneFile(String doneFileName, final Class<?> expectedException) throws Exception
	{
        observer.setNewFileListener(f -> fail());
        observer.setChecksumMismatchListener((newFile, doneFile) -> fail());
        observer.setErrorListener((doneFile, e) -> {assertEquals(expectedException, e.getClass()); notifyFromCallback();});
		
		observer.start();
		
		Files.createFile(tempDir.resolve(doneFileName + ".done"));
		
		waitForCallback();
	}

}
