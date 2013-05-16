package main.java.directoryobserver;

import java.io.File;

@FunctionalInterface
public interface ChecksumMismatchListener
{
    void onChecksumMismatch(File newFile, File doneFile);
}
