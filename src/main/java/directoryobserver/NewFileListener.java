package main.java.directoryobserver;

import java.io.File;

@FunctionalInterface
public interface NewFileListener
{
	void onNewFile(File newFile);
}

