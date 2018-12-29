/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.tests.correctness;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Random;
import org.xadisk.bridge.proxies.impl.RemoteXAFileSystem;
import org.xadisk.connector.inbound.XADiskActivationSpecImpl;
import org.xadisk.connector.inbound.EndPointActivation;
import org.xadisk.filesystem.utilities.FileIOUtility;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.SessionCommonness;
import org.xadisk.filesystem.XAFileSystemCommonness;

public class CoreXAFileSystemTests {

    static enum testNames {

        testIOOperations, testIOOperationsPostCrash, testDynamicReadWrite,
        testDynamicReadWritePostCrash, testConcurrentMoneyTransfer, testConcurrentMoneyTransferPostCrash,
        testFileSystemEventing, testFileSystemEventingPostCrash
    };
    private static final String SEPERATOR = File.separator;
    static Object namesake = new CoreXAFileSystemTests();
    static boolean testProgressive = false;
    static boolean testHighNumber = false;
    static boolean usePessimisticLock = true;
    static final int initialFileSizeForDynamicRWTest = 100000;
    private Session ioOperationsSession;
    private File ioOperationsRoot1;
    private File ioOperationsRoot2;
    private File baseForRollbackRoot;
    private int nextCheckpointAt = -1;
    private int currentCheckpoint = 0;
    private boolean checkForRollbackNext = false;

    public void testIOOperations(String testDirectory) throws Exception {
        if (testProgressive) {
            checkForRollbackNext = true;
            for (int i = -1; i <= 40; i++) {
                System.out.println("TestIOOperatios : Incremental Step # " + i);
                nextCheckpointAt = i;
                currentCheckpoint = 1;
                testIOOperationsOneRound(testDirectory, testHighNumber);
            }
            checkForRollbackNext = false;
            for (int i = -1; i <= 40; i++) {
                System.out.println("TestIOOperatios : Incremental Step # " + i);
                nextCheckpointAt = i;
                currentCheckpoint = 1;
                testIOOperationsOneRound(testDirectory, testHighNumber);
            }
        } else {
            checkForRollbackNext = true;
            nextCheckpointAt = 100;
            currentCheckpoint = 1;
            testIOOperationsOneRound(testDirectory, testHighNumber);
            checkForRollbackNext = false;
            nextCheckpointAt = 100;
            currentCheckpoint = 1;
            testIOOperationsOneRound(testDirectory, testHighNumber);
        }
    }

    private void testIOOperationsOneRound(String testDirectory, boolean testHighNumber) throws Exception {
        String roots[] = new String[2];
        roots[0] = testDirectory + SEPERATOR + "dir1";
        roots[1] = testDirectory + SEPERATOR + "dir2";
        String baseForRollback = testDirectory + SEPERATOR + "baseForRollback";
        ioOperationsRoot1 = new File(roots[0]);
        ioOperationsRoot2 = new File(roots[1]);
        baseForRollbackRoot = new File(baseForRollback);
        TestUtility.cleanupDirectory(ioOperationsRoot1);
        TestUtility.cleanupDirectory(ioOperationsRoot2);
        TestUtility.cleanupDirectory(baseForRollbackRoot);
        FileIOUtility.createDirectoriesIfRequired(ioOperationsRoot1);
        FileIOUtility.createDirectoriesIfRequired(ioOperationsRoot2);
        FileIOUtility.createDirectoriesIfRequired(baseForRollbackRoot);
        byte content[] = "TextContent...".getBytes();
        int smallFileSize = 100;
        int mediumFileSize = 1000;
        int largeFileSize = 200000;

        resetIOOperationsSession();

        if (testHighNumber) {
            for (int i = 0; i < 1000; i++) {
                String fileName = "small.txt" + i;
                TestUtility.createFile(fileName, false, ioOperationsSession, roots);
                TestUtility.writeDataToOutputStreams(content, smallFileSize, fileName, ioOperationsSession, roots);
                fileName = "large.txt" + i;
                TestUtility.createFile(fileName, false, ioOperationsSession, roots);
                TestUtility.writeDataToOutputStreams(content, largeFileSize, fileName, ioOperationsSession, roots);
            }
            checkpoint();
        }

        TestUtility.createFile("childDir1", true, ioOperationsSession, roots);
        checkpoint();
        TestUtility.createFile("childDir2", true, ioOperationsSession, roots);
        checkpoint();
        System.out.println("*******************");

        TestUtility.createFile("childDir1" + SEPERATOR + "small.txt", false, ioOperationsSession, roots);
        checkpoint();
        TestUtility.createFile("childDir1" + SEPERATOR + "medium.txt", false, ioOperationsSession, roots);
        checkpoint();
        TestUtility.createFile("childDir1" + SEPERATOR + "large.txt", false, ioOperationsSession, roots);
        checkpoint();
        System.out.println("*******************");

        TestUtility.writeDataToOutputStreams(content, smallFileSize, "childDir1" + SEPERATOR + "small.txt", ioOperationsSession, roots);
        checkpoint();
        TestUtility.writeDataToOutputStreams(content, mediumFileSize, "childDir1" + SEPERATOR + "medium.txt", ioOperationsSession, roots);
        checkpoint();
        TestUtility.writeDataToOutputStreams(content, largeFileSize, "childDir1" + SEPERATOR + "large.txt", ioOperationsSession, roots);
        checkpoint();

        TestUtility.moveFile("childDir1" + SEPERATOR + "medium.txt", "childDir2" + SEPERATOR + "medium.txt", ioOperationsSession, roots);
        checkpoint();
        TestUtility.moveFile("childDir1" + SEPERATOR + "small.txt", "childDir2" + SEPERATOR + "small.txt", ioOperationsSession, roots);
        checkpoint();
        TestUtility.moveFile("childDir1" + SEPERATOR + "large.txt", "childDir2" + SEPERATOR + "large.txt", ioOperationsSession, roots);
        checkpoint();
        System.out.println("*******************");

        TestUtility.copyFile("childDir2" + SEPERATOR + "small.txt", "childDir2" + SEPERATOR + "smallRegenerated.txt", ioOperationsSession, roots);
        checkpoint();
        TestUtility.copyFile("childDir2" + SEPERATOR + "medium.txt", "childDir2" + SEPERATOR + "mediumRegenerated.txt", ioOperationsSession, roots);
        checkpoint();
        TestUtility.copyFile("childDir2" + SEPERATOR + "large.txt", "childDir2" + SEPERATOR + "largeRegenerated.txt", ioOperationsSession, roots);
        checkpoint();

        TestUtility.deleteFile("childDir2" + SEPERATOR + "small.txt", ioOperationsSession, roots);
        checkpoint();
        TestUtility.deleteFile("childDir2" + SEPERATOR + "medium.txt", ioOperationsSession, roots);
        checkpoint();
        TestUtility.deleteFile("childDir2" + SEPERATOR + "large.txt", ioOperationsSession, roots);
        checkpoint();

        TestUtility.moveFile("childDir2" + SEPERATOR + "smallRegenerated.txt", "childDir1" + SEPERATOR + "small.txt", ioOperationsSession, roots);
        checkpoint();
        TestUtility.moveFile("childDir2" + SEPERATOR + "mediumRegenerated.txt", "childDir1" + SEPERATOR + "medium.txt", ioOperationsSession, roots);
        checkpoint();
        TestUtility.moveFile("childDir2" + SEPERATOR + "largeRegenerated.txt", "childDir1" + SEPERATOR + "large.txt", ioOperationsSession, roots);
        checkpoint();

        TestUtility.truncateFile("childDir1" + SEPERATOR + "small.txt", smallFileSize / 2, ioOperationsSession, roots);
        checkpoint();
        TestUtility.truncateFile("childDir1" + SEPERATOR + "medium.txt", mediumFileSize / 2, ioOperationsSession, roots);
        checkpoint();
        TestUtility.truncateFile("childDir1" + SEPERATOR + "large.txt", largeFileSize / 2, ioOperationsSession, roots);
        checkpoint();

        TestUtility.writeDataToOutputStreams(content, smallFileSize / 2, "childDir1" + SEPERATOR + "small.txt", ioOperationsSession, roots);
        checkpoint();
        TestUtility.writeDataToOutputStreams(content, mediumFileSize / 2, "childDir1" + SEPERATOR + "medium.txt", ioOperationsSession, roots);
        checkpoint();
        TestUtility.writeDataToOutputStreams(content, largeFileSize / 2, "childDir1" + SEPERATOR + "large.txt", ioOperationsSession, roots);
        checkpoint();

        TestUtility.moveFile("childDir1", "childDir2" + SEPERATOR + "childDir1", ioOperationsSession, roots);
        checkpoint();

        TestUtility.createFile("childDir2" + SEPERATOR + "childDir1" + SEPERATOR + "grandChild11", true, ioOperationsSession, roots);
        checkpoint();
        TestUtility.createFile("childDir2" + SEPERATOR + "childDir1" + SEPERATOR + "grandChild11" + SEPERATOR + "grandgrandChild111", false, ioOperationsSession, roots);
        checkpoint();

        TestUtility.moveFile("childDir2" + SEPERATOR + "childDir1" + SEPERATOR + "grandChild11", "childDir2" + SEPERATOR + "grandChild11", ioOperationsSession, roots);
        checkpoint();
        TestUtility.moveFile("childDir2" + SEPERATOR + "childDir1", "childDir1", ioOperationsSession, roots);
        checkpoint();
        TestUtility.moveFile("childDir2" + SEPERATOR + "grandChild11", "childDir1" + SEPERATOR + "grandChild11", ioOperationsSession, roots);
        checkpoint();

        TestUtility.createFile("childDir1" + SEPERATOR + "dirToDelete", true, ioOperationsSession, roots);
        checkpoint();
        TestUtility.createFile("childDir1" + SEPERATOR + "dirToDelete" + SEPERATOR + "delete.txt", false, ioOperationsSession, roots);
        checkpoint();

        TestUtility.moveFile("childDir1" + SEPERATOR + "dirToDelete", "childDir2" + SEPERATOR + "dirToDeleteMoved", ioOperationsSession, roots);
        checkpoint();

        TestUtility.deleteFile("childDir2" + SEPERATOR + "dirToDeleteMoved" + SEPERATOR + "delete.txt", ioOperationsSession, roots);
        checkpoint();

        TestUtility.moveFile("childDir2" + SEPERATOR + "dirToDeleteMoved", "childDir1" + SEPERATOR + "dirToDelete", ioOperationsSession, roots);
        checkpoint();

        TestUtility.deleteFile("childDir1" + SEPERATOR + "dirToDelete", ioOperationsSession, roots);
        checkpoint();
        System.out.println("*******************");

        TestUtility.compareDiskAndView(ioOperationsRoot2, ioOperationsRoot1, ioOperationsSession);
        if (checkForRollbackNext) {
            System.out.println("Rolling Back...");
            ioOperationsSession.rollback();
            TestUtility.compareDiskAndDisk(baseForRollbackRoot, ioOperationsRoot1);
        } else {
            System.out.println("Committing...");
            ((SessionCommonness) ioOperationsSession).commit(true);
            TestUtility.compareDiskAndDisk(ioOperationsRoot2, ioOperationsRoot1);
        }
        System.out.println("Done.");
    }

    private void checkpoint() throws Exception {
        if (nextCheckpointAt == -1 || currentCheckpoint == nextCheckpointAt) {
            if (checkForRollbackNext) {
                System.out.println("Rolling Back...");
                ioOperationsSession.rollback();
                resetIOOperationsSession();
                TestUtility.compareDiskAndDisk(baseForRollbackRoot, ioOperationsRoot1);
                TestUtility.cleanupDirectory(ioOperationsRoot1);
                TestUtility.cleanupDirectory(baseForRollbackRoot);
                TestUtility.copyDirectory(ioOperationsRoot2, baseForRollbackRoot);
                TestUtility.copyDirectory(ioOperationsRoot2, ioOperationsRoot1);
            } else {
                //no need for view verification in rollback case; we just need to do it in either place.
                TestUtility.compareDiskAndView(ioOperationsRoot2, ioOperationsRoot1, ioOperationsSession);
                System.out.println("Committing...");
                ((SessionCommonness) ioOperationsSession).commit(true);
                resetIOOperationsSession();
                TestUtility.compareDiskAndDisk(ioOperationsRoot2, ioOperationsRoot1);
            }
        }
        currentCheckpoint++;
    }

    public void testIOOperationsPostCrash(String testDirectory) throws Exception {
        String roots[] = new String[2];
        roots[0] = testDirectory + SEPERATOR + "dir1";
        roots[1] = testDirectory + SEPERATOR + "dir2";
        File fileRoot1 = new File(roots[0]);
        File fileRoot2 = new File(roots[1]);
        if (fileRoot1.exists() && fileRoot2.exists() && fileRoot1.listFiles().length > 0) {
            System.out.println("Assuming it was Commit before crash.");
            TestUtility.compareDiskAndDisk(fileRoot1, fileRoot2);
        } else {
            System.out.println("Assuming it was NO Commit before crash.");
        }
    }

    public void testDynamicReadWrite(String testDirectory) throws Exception {
        String roots[] = new String[2];
        roots[0] = testDirectory + SEPERATOR + "dir1";
        roots[1] = testDirectory + SEPERATOR + "dir2";
        File fileRoot1 = new File(roots[0]);
        File fileRoot2 = new File(roots[1]);
        TestUtility.cleanupDirectory(fileRoot1);
        TestUtility.cleanupDirectory(fileRoot2);
        FileIOUtility.createDirectoriesIfRequired(fileRoot1);
        FileIOUtility.createDirectoriesIfRequired(fileRoot2);

        byte modulo = 111;
        for (int i = 0; i < 2; i++) {
            File source = new File(roots[i] + SEPERATOR + "source.txt");
            FileOutputStream fos = new FileOutputStream(source);
            for (int j = 0; j < initialFileSizeForDynamicRWTest; j++) {
                fos.write(j % modulo);
            }
            fos.flush();
            fos.close();
        }
        File source1 = new File(roots[0] + SEPERATOR + "source.txt");
        File source2 = new File(roots[1] + SEPERATOR + "source.txt");

        XAFileSystem xaFileSystem = TestUtility.getXAFileSystemForTest();
        Session session = xaFileSystem.createSessionForLocalTransaction();

        XAFileInputStream xisSource1 = session.createXAFileInputStream(source1, false);
        XAFileOutputStream xosSource1 = session.createXAFileOutputStream(source1, false);
        FileOutputStream fosSource2 = new FileOutputStream(source2, true);
        FileChannel fcSource2 = fosSource2.getChannel();

        long currentViewFileSize = initialFileSizeForDynamicRWTest;
        Random randomPosition = new Random();
        long appendAmountInEachRound = initialFileSizeForDynamicRWTest / 5;
        int totalRounds = 100;
        for (int rounds = 1; rounds < totalRounds; rounds++) {
            if (rounds == totalRounds / 2) {
                xosSource1.close();
                xosSource1 = session.createXAFileOutputStream(source1, true);
            }
            if (rounds % (totalRounds / 20 + 1) == 0) {
                xosSource1.close();
                long sizeToTruncateTo = randomPosition.nextInt((int) currentViewFileSize - 1);
                session.truncateFile(source1, sizeToTruncateTo);
                fcSource2.truncate(sizeToTruncateTo);
                xosSource1 = session.createXAFileOutputStream(source1, false);
                currentViewFileSize = sizeToTruncateTo;
            }
            for (long i = currentViewFileSize; i < currentViewFileSize + appendAmountInEachRound; i++) {
                xosSource1.write((int) (i % modulo));
                fosSource2.write((int) (i % modulo));
            }
            currentViewFileSize += appendAmountInEachRound;
            xosSource1.flush();

            for (long i = 0; i < currentViewFileSize / 100000; i++) {
                long position = randomPosition.nextInt((int) currentViewFileSize - 1);
                xisSource1.position(position);
                byte b[] = new byte[1];
                xisSource1.read(b);
                if (b[0] != position % modulo) {
                    throw new Exception("Testcase testDynamicReadWrite failed for position : " + position);
                }
            }
        }
        fosSource2.flush();
        ((SessionCommonness) session).commit(true);

        TestUtility.compareDiskAndDisk(fileRoot2, fileRoot1);
    }

    public void testDynamicReadWritePostCrash(String testDirectory) throws Exception {
        String roots[] = new String[2];
        roots[0] = testDirectory + SEPERATOR + "dir1";
        roots[1] = testDirectory + SEPERATOR + "dir2";
        File source1 = new File(roots[0] + SEPERATOR + "source.txt");
        File source2 = new File(roots[1] + SEPERATOR + "source.txt");
        if (source1.exists() && source2.exists() && source1.length() != initialFileSizeForDynamicRWTest) {
            System.out.println("Assuming it was Commit before crash.");
            TestUtility.compareDiskAndDisk(source1, source2);
        } else {
            System.out.println("Assuming it was NO Commit before crash.");
            byte modulo = 111;
            if (source1.exists()) {
                long length = source1.length();
                if (length != initialFileSizeForDynamicRWTest) {
                    throw new AssertionFailedException("File Content-Length Mismatch: " + source1);
                }
                FileInputStream fis1 = new FileInputStream(source1);
                for (int i = 0; i < length; i++) {
                    int b = fis1.read();
                    if (b != i % modulo) {
                        throw new AssertionFailedException("File Content Mismatch: " + source1 + " at " + "position " + i);
                    }
                }
                fis1.close();
            }
        }
    }

    public void testConcurrentMoneyTransfer(String testDirectory)
            throws Exception {
        final File rich = new File(testDirectory + SEPERATOR + "rich.txt");
        final File poor = new File(testDirectory + SEPERATOR + "poor.txt");
        TestUtility.cleanupDirectory(new File(testDirectory));
        FileIOUtility.createDirectoriesIfRequired(new File(testDirectory));
        FileIOUtility.createFile(rich);
        FileIOUtility.createFile(poor);
        FileOutputStream fos = new FileOutputStream(rich);
        fos.write(100);
        fos.close();
        fos = new FileOutputStream(poor);
        fos.write(0);
        fos.close();

        ArrayList<Thread> allThreads = new ArrayList<Thread>();

        for (int round = 0; round < 120; round++) {
            System.out.println("Entering into round " + round);
            int concurrencyLevel = 3;
            for (int i = 0; i < concurrencyLevel; i++) {
                Thread accountant = new Thread(new Runnable() {

                    public void run() {
                        try {
                            XAFileSystem xaFileSystem = TestUtility.getXAFileSystemForTest();
                            xaFileSystem.createSessionForLocalTransaction();
                            plainOldMoneyTransfer(rich, poor, xaFileSystem.createSessionForLocalTransaction(),
                                    usePessimisticLock);
                            System.out.println("Suceess.");
                        } catch (Exception e) {
                            System.out.println("Failure due to :" + e.getMessage());
                        }
                    }
                });
                accountant.start();
                allThreads.add(accountant);
            }
            TestUtility.waitForAllAtHeaven(allThreads);
            allThreads.clear();
        }
        verifyMoneyTransfersPostAllCommit(rich, poor);
    }

    private void plainOldMoneyTransfer(File rich, File poor, Session session,
            boolean bePessimistic) throws Exception {
        XAFileInputStream xisPoor = session.createXAFileInputStream(poor, bePessimistic);
        XAFileInputStream xisRich = session.createXAFileInputStream(rich, bePessimistic);

        long poorLength = session.getFileLength(poor, bePessimistic);
        long richLength = session.getFileLength(rich, bePessimistic);

        xisPoor.position(poorLength - 1);
        byte poorHas = (byte) xisPoor.read();
        xisRich.position(richLength - 1);
        byte richHas = (byte) xisRich.read();
        XAFileOutputStream xosPoor = session.createXAFileOutputStream(poor, false);
        XAFileOutputStream xosRich = session.createXAFileOutputStream(rich, false);
        if (richHas > 0) {
            richHas--;
            poorHas++;
            xosPoor.write((int) poorHas);
            xosRich.write((int) richHas);
        }
        xisPoor.close();
        xisRich.close();
        xosPoor.close();
        xosRich.close();

        ((SessionCommonness) session).commit(true);
    }

    private void verifyMoneyTransfersPostAllCommit(File rich, File poor) throws Exception {
        FileInputStream fisRich = new FileInputStream(rich);
        for (int i = 0; i < fisRich.getChannel().size(); i++) {
            if (fisRich.read() != 100 - i) {
                throw new AssertionFailedException("Content Unexpected@" + i + ": " + rich);
            }
        }
        FileInputStream fisPoor = new FileInputStream(poor);
        for (int i = 0; i < fisPoor.getChannel().size(); i++) {
            if (fisPoor.read() != i) {
                throw new AssertionFailedException("Content Unexpected@" + i + ": " + poor);
            }
        }
        if (fisRich.getChannel().size() != fisPoor.getChannel().size()) {
            throw new AssertionFailedException("Content Length Mismtach among poor and rich");
        }
    }

    public void testConcurrentMoneyTransferPostCrash(String testDirectory)
            throws Exception {
        File rich = new File(testDirectory + SEPERATOR + "rich.txt");
        File poor = new File(testDirectory + SEPERATOR + "poor.txt");
        if (rich.exists() && poor.exists() && rich.length() + poor.length() > 2) {
            verifyMoneyTransfersPostAllCommit(rich, poor);
        }
    }

    public void testFileSystemEventing(String testDirectory) throws Exception {
        File testDirectoryFile = new File(testDirectory);
        TestUtility.cleanupDirectory(testDirectoryFile);
        FileIOUtility.createDirectoriesIfRequired(testDirectoryFile);

        File interestingFiles[] = new File[2];
        interestingFiles[0] = new File(testDirectory + SEPERATOR + "polledFile1.txt");
        interestingFiles[1] = new File(testDirectory + SEPERATOR + "polledFile2.txt");

        XADiskActivationSpecImpl actSpec = new XADiskActivationSpecImpl();
        String fileNamesAndInterests = interestingFiles[0].getAbsolutePath() + "::111" + "|" + interestingFiles[0].getParentFile().getAbsolutePath() + "::111";
        actSpec.setFileNamesAndEventInterests(fileNamesAndInterests);

        SimulatedMessageEndpointFactory smef = new SimulatedMessageEndpointFactory();
        EndPointActivation epActivation = new EndPointActivation(smef, actSpec);
        XAFileSystemCommonness xaFileSystem = new RemoteXAFileSystem("localhost", TestUtility.remoteXADiskPort, NativeXAFileSystem.getXAFileSystem("local"));
        xaFileSystem.registerEndPointActivation(epActivation);

        Session session = xaFileSystem.createSessionForLocalTransaction();
        session.setPublishFileStateChangeEventsOnCommit(Boolean.TRUE);
        session.createFile(interestingFiles[0], false);
        session.copyFile(interestingFiles[0], interestingFiles[1]);
        session.deleteFile(interestingFiles[0]);
        ((SessionCommonness) session).commit(true);

        while (smef.getEventsReceivedCount() < 5) {
            System.out.println("Not all events recevied yet. Receieved : " + smef.getEventsReceivedCount());
            Thread.sleep(1000);
        }

        System.out.println("Inbound Messaging Test Successful.");
        //we should allow some time here as the connections opened in this thread
        //will still be required for calls like afterDelivery, and the termination
        //of this thread will lead to closing of such connections/sockets.
        Thread.sleep(10000);
    }

    public void testFileSystemEventingPostCrash(String testDirectory) throws Exception {
        return;
    }

    public void resetIOOperationsSession() {
        ioOperationsSession = TestUtility.getXAFileSystemForTest().createSessionForLocalTransaction();
    }
}
