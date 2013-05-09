package test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import main.DirectoryObserver;
import main.NewFileListener;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;



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
		
		observer.addListener(new NewFileListener()
		{
			@Override
			public void onNewFile(File newFile)
			{
				newFiles.add(newFile.getName());
				
				notifyFromCallback();
			}
			
			@Override
			public void onError(File doneFile, Exception e)
			{
			}
			
			@Override
			public void onChecksumMismatch(File newFile, File doneFile)
			{
			}
		});
		
		observer.start();
		
		String[] newFileNames = new String[]{"f1", "f2", "f3"};
		for (String newFileName : newFileNames)
		{
			Path newFile = Files.createFile(tempDir.resolve(newFileName));
			IOUtils.write(newFileName, new FileWriter(newFile.toFile()));
			
			String md5 = DigestUtils.md5Hex(new FileInputStream(newFile.toFile()));
			Files.createFile(tempDir.resolve(newFileName + "." + md5 + ".done"));
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
	}
	
	@Test
	public void testChecksumError() throws Exception
	{
		observer.addListener(new NewFileListener()
		{
			@Override
			public void onNewFile(File newFile)
			{
			}
			
			@Override
			public void onError(File doneFile, Exception e)
			{
			}
			
			@Override
			public void onChecksumMismatch(File newFile, File doneFile)
			{
				notifyFromCallback();
			}
		});
		
		observer.start();
		
		Path newFile = Files.createFile(tempDir.resolve(myFileName));
		IOUtils.write("ciao", new FileWriter(newFile.toFile()));
		
		Files.createFile(tempDir.resolve(myFileName + ".fakemd5.done"));
		
		waitForCallback();
	}
	
	@Test
	public void testErrorBecauseDoneFileDoesntHaveNewFile() throws Exception
	{
		observer.addListener(new NewFileListener()
		{
			@Override
			public void onNewFile(File newFile)
			{
			}
			
			@Override
			public void onError(File doneFile, Exception e)
			{
				notifyFromCallback();
			}
			
			@Override
			public void onChecksumMismatch(File newFile, File doneFile)
			{
			}
		});
		
		observer.start();
		
		Files.createFile(tempDir.resolve(myFileName + ".fakemd5.done"));
		
		waitForCallback();
	}

}
