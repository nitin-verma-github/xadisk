/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.tests.performance;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;
import org.xadisk.filesystem.utilities.FileIOUtility;
import org.xadisk.tests.correctness.TestUtility;

/**
 * Note that these performance tests are "under construction". Your suggestions about
 * writing these tests, setting up the system and taking measurements are always welcome.
 * Thanks.
 */
public class Appraiser {

    private static final int UNIT_SIZE = 100;
    public static final int BUFFER_SIZE = UNIT_SIZE * 10;
    public static final int FILE_SIZE = UNIT_SIZE * 10000;

    public static void main(String args[]) {
        try {
            String xadiskHome = "C:\\xadiskPerformance";
            XAFileSystem xafs = null;
            File testDirectory = new File("C:\\test");
            int concurrency = 4;
            boolean useXADisk = true;
            long averageWriterTime = 0;
            long averageReaderTime = 0;
            long averageTruncaterTime = 0;
            long repetitions = 3;

            for (int repetition = 0; repetition < repetitions; repetition++) {
                TestUtility.cleanupDirectory(new File(xadiskHome));
                if (useXADisk) {
                    StandaloneFileSystemConfiguration configuration =
                            new StandaloneFileSystemConfiguration(xadiskHome, "");
                    configuration.setTransactionTimeout(Integer.MAX_VALUE);
                    configuration.setBufferPoolRelieverInterval(Integer.MAX_VALUE);
                    xafs = XAFileSystemProxy.bootNativeXAFileSystem(configuration);
                    xafs.waitForBootup(-1);
                }

                FileIOUtility.deleteDirectoryRecursively(testDirectory);
                FileIOUtility.createDirectory(testDirectory);

                AtomicLong totalTimeReaders = new AtomicLong(0);
                AtomicLong totalTimeWriters = new AtomicLong(0);
                AtomicLong totalTimeTruncaters = new AtomicLong(0);
                File filePaths[] = new File[concurrency];
                Thread writers[] = new Thread[concurrency];
                Thread readers[] = new Thread[concurrency];
                Thread truncaters[] = new Thread[concurrency];
                for (int i = 0; i < concurrency; i++) {
                    filePaths[i] = new File(testDirectory, i + "");
                    FileIOUtility.createFile(filePaths[i]);
                    writers[i] = new Thread(new FileWriter(filePaths[i], totalTimeWriters, useXADisk));
                    readers[i] = new Thread(new FileReader(filePaths[i], totalTimeReaders, useXADisk));
                    truncaters[i] = new Thread(new FileTruncater(filePaths[i], totalTimeTruncaters,
                            useXADisk));
                }
                startAll(writers);
                joinAll(writers);
                startAll(readers);
                joinAll(readers);
                startAll(truncaters);
                joinAll(truncaters);

                for (int i = 0; i < concurrency; i++) {
                    FileIOUtility.deleteFile(filePaths[i]);
                }

                averageWriterTime += totalTimeWriters.longValue() / concurrency;
                averageReaderTime += totalTimeReaders.longValue() / concurrency;
                averageTruncaterTime += totalTimeTruncaters.longValue() / concurrency;
                System.out.println("Cumulative Times[write/read/truncate] [" + averageWriterTime + "\t"
                        + averageReaderTime + "\t" + averageTruncaterTime + "]");

                if (useXADisk) {
                    xafs.shutdown();
                }
            }
            System.out.println("Average Times[write/read/truncate] [" + averageWriterTime / repetitions + "\t"
                    + averageReaderTime / repetitions + "\t" + averageTruncaterTime / repetitions + "]");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startAll(Thread[] threads) {
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
    }

    private static void joinAll(Thread[] threads) throws InterruptedException {
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
    }
}
