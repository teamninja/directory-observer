import directoryobserver.*;
import org.apache.commons.codec.digest.*;
import org.apache.commons.io.*;
import org.apache.commons.io.filefilter.*;
import org.apache.log4j.*;
import org.junit.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.Assert.*;

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
		final ConcurrentLinkedDeque<String> newFiles = new ConcurrentLinkedDeque<>();
		
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
				fail();
			}
			
			@Override
			public void onChecksumMismatch(File newFile, File doneFile)
			{
				fail();
			}
		});
		
		observer.start();
		
		String[] newFileNames = new String[]{"f1", "f2", "f3"};
		List<Path> doneFiles = new ArrayList<>();
		
		for (String newFileName : newFileNames)
		{
            Path doneFile = writeNewFileWithDoneFile(newFileName);

            doneFiles.add(doneFile);
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
		observer.addListener(new NewFileListener()
		{
			@Override
			public void onNewFile(File newFile)
			{
				fail();
			}
			
			@Override
			public void onError(File doneFile, Exception e)
			{
				fail();
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
    public void testDoneFileAlreadyInTheDirectoryBeforeStart() throws Exception
    {
        observer.addListener(new NewFileListener()
        {
            @Override
            public void onNewFile(File newFile)
            {
                notifyFromCallback();
            }

            @Override
            public void onError(File doneFile, Exception e)
            {
                fail();
            }

            @Override
            public void onChecksumMismatch(File newFile, File doneFile)
            {
                fail();
            }
        });

        writeNewFileWithDoneFile("alreadyHere");

        observer.start();

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

    @Test
    public void testDoneFileAddedInTheDirectoryDuringStart() throws Exception
    {
        instantiateListener();

        Thread doneFileCreatorThread = createThreadCreatingNewFilesInDirectory();

        writeNewFileWithDoneFile("alreadyHere1");

        doneFileCreatorThread.start();
        observer.start();

        doneFileCreatorThread.join();

        Thread.sleep(3000);
        callbackExecuted.set(true);    // To make @After happy

        assertEquals(0, FileUtils.listFiles(tempDir.toFile(), FileFilterUtils.suffixFileFilter(".done"), FileFilterUtils.directoryFileFilter()).size());
    }

    private Thread createThreadCreatingNewFilesInDirectory()
    {
        return new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for(int i = 0; i< 5; i++)
                        {
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            writeNewFileWithDoneFile("created_during_start" + i);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
    }

    private void instantiateListener()
    {
        observer.addListener(new NewFileListener()
        {
            @Override
            public void onNewFile(File newFile)
            {
                try {
                    Thread.sleep(50);
                    newFile.delete();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(File doneFile, Exception e)
            {
                fail();
            }

            @Override
            public void onChecksumMismatch(File newFile, File doneFile)
            {
                fail();
            }
        });
    }

    private Path writeNewFileWithDoneFile(String newFileName) throws IOException
    {
        Path newFile = Files.createFile(tempDir.resolve(newFileName));
        IOUtils.write(newFileName, new FileWriter(newFile.toFile()));

        String md5 = DigestUtils.md5Hex(new FileInputStream(newFile.toFile()));
        Path doneFile = tempDir.resolve(newFileName + "." + md5 + ".done");

        Files.createFile(doneFile);
        return doneFile;
    }

	private void testForWrongDoneFile(String doneFileName, final Class<?> expectedException) throws Exception
	{
		observer.addListener(new NewFileListener()
		{
			@Override
			public void onNewFile(File newFile)
			{
				fail();
			}
			
			@Override
			public void onError(File doneFile, Exception e)
			{
				assertEquals(expectedException, e.getClass());
				notifyFromCallback();
			}
			
			@Override
			public void onChecksumMismatch(File newFile, File doneFile)
			{
				fail();
			}
		});
		
		observer.start();
		
		Files.createFile(tempDir.resolve(doneFileName + ".done"));
		
		waitForCallback();
	}

}
