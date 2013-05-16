package main.java.directoryobserver;

import java.io.File;

@FunctionalInterface
public interface ErrorListener
{
    void onError(File doneFile, Exception e);
}
