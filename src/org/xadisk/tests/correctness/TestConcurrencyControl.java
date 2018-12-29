/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.tests.correctness;

import java.io.File;
import org.xadisk.filesystem.ConcurrencyControl;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

public class TestConcurrencyControl {

    public static void main(String args[]) {
        try {
            StandaloneFileSystemConfiguration configurationMaster = new StandaloneFileSystemConfiguration("C:\\xaMaster", "master");
            configurationMaster.setEnableClusterMode(true);
            configurationMaster.setServerAddress("localhost");
            configurationMaster.setServerPort(9999);
            configurationMaster.setDeadLockDetectorInterval(1);
            XAFileSystemProxy.bootNativeXAFileSystem(configurationMaster).waitForBootup(-1);

            StandaloneFileSystemConfiguration configurationSlave = new StandaloneFileSystemConfiguration("C:\\xaSlave", "slave");
            configurationSlave.setEnableClusterMode(true);
            configurationSlave.setClusterMasterAddress("localhost");
            //configurationSlave.setClusterMasterAddress("#master");
            configurationSlave.setClusterMasterPort(9999);
            configurationSlave.setLockTimeOut(1);
            XAFileSystem xafs = XAFileSystemProxy.bootNativeXAFileSystem(configurationSlave);
            xafs.waitForBootup(-1);

            testTransactionTimeout(xafs);
            testDeadlock(xafs);

            File rename = new File("C:\\test\\1");
            File delete = new File(rename, "a.txt");
            rename.mkdirs();
            delete.createNewFile();

            for (int i = 0; i < 20; i++) {
                int concurrency = 5;
                RenameDirApplication renameDirApplication[] = new RenameDirApplication[concurrency];
                Thread tRename[] = new Thread[concurrency];
                for (int t = 0; t < concurrency; t++) {
                    renameDirApplication[t] = new RenameDirApplication(xafs.createSessionForLocalTransaction(), rename);
                    tRename[t] = new Thread(renameDirApplication[t]);
                }

                DeleteFileApplication deleteFileApplication[] = new DeleteFileApplication[concurrency];
                Thread tDelete[] = new Thread[concurrency];
                for (int t = 0; t < concurrency; t++) {
                    deleteFileApplication[t] = new DeleteFileApplication(xafs.createSessionForLocalTransaction(), delete);
                    tDelete[t] = new Thread(deleteFileApplication[t]);
                }

                for (int t = 0; t < concurrency; t++) {
                    tRename[t].start();
                    tDelete[t].start();
                }

                for (int t = 0; t < concurrency; t++) {
                    tRename[t].join();
                    tDelete[t].join();
                }

                System.out.println(i + "th run...");

                byte numSucceeded = 0;
                for (int t = 0; t < concurrency; t++) {
                    if (renameDirApplication[t].lockingSucceeded) {
                        System.out.print("Rename succeeded " + t);
                        numSucceeded++;
                    }
                    if (deleteFileApplication[t].lockingSucceeded) {
                        System.out.print("Delete succeeded " + t);
                        numSucceeded++;
                    }
                }
                if (numSucceeded != 1) {
                    throw new Exception("Something went wrong...");
                }
                System.out.println();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Done");
    }

    private static void testTransactionTimeout(XAFileSystem xafs) {
        try {
            File f = new File("C:\\a.txt");
            Session session1 = xafs.createSessionForLocalTransaction();
            session1.setTransactionTimeout(1);
            Thread.sleep(3000);
            session1.createFile(f, false);
        } catch (Exception e) {
            System.err.println(e.getCause());
        }
    }

    private static void testDeadlock(XAFileSystem xafs) {
        try {
            File f1 = new File("C:\\1.txt");
            File f2 = new File("C:\\2.txt");
            f1.createNewFile();
            f2.createNewFile();
            Session session1 = xafs.createSessionForLocalTransaction();
            Session session2 = xafs.createSessionForLocalTransaction();
            session1.setFileLockWaitTimeout(Integer.MAX_VALUE);
            session2.setFileLockWaitTimeout(Integer.MAX_VALUE);
            session1.deleteFile(f1);
            session2.deleteFile(f2);
            new Thread(new DeleteConcurrently(session2, f1)).start();
            Thread.sleep(1000);
            session1.deleteFile(f2);
        } catch (Exception e) {
            System.err.println(e.getCause());
        }
    }
}

class RenameDirApplication implements Runnable {

    File srcDir;
    Session session;
    boolean lockingSucceeded = true;

    public RenameDirApplication(Session session, File srcDir) {
        this.session = session;
        this.srcDir = srcDir;
    }

    public void run() {
        try {
            session.moveFile(srcDir, new File(srcDir.getParentFile(), "xyz"));
            //the sleep time is big enough so that only one of the 6 transactions in the iteration succeeds in locking/pinning.
            Thread.sleep(100);
            session.rollback();
        } catch (Exception e) {
            lockingSucceeded = false;
        }
    }
}

class DeleteFileApplication implements Runnable {

    ConcurrencyControl fcc;
    File testFile;
    boolean lockingSucceeded = true;
    Session session;

    public DeleteFileApplication(Session session, File testFile) {
        this.session = session;
        this.testFile = testFile;
    }

    public void run() {
        try {
            Thread.sleep(3);
            session.deleteFile(testFile);
            Thread.sleep(100);
            session.rollback();
        } catch (Exception e) {
            lockingSucceeded = false;
        }
    }
}

class DeleteConcurrently implements Runnable {

    ConcurrencyControl fcc;
    File testFile;
    Session session;

    public DeleteConcurrently(Session session, File testFile) {
        this.session = session;
        this.testFile = testFile;
    }

    public void run() {
        try {
            session.deleteFile(testFile);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
