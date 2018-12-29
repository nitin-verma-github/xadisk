/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.tests.correctness;

class RunnableTest implements Runnable {

    private CoreXAFileSystemTests.testNames testName;
    private String testDirectory;

    public RunnableTest(CoreXAFileSystemTests.testNames testName, String testDirectory) {
        this.testName = testName;
        this.testDirectory = testDirectory;
    }

    public void run() {
        try {
            CoreXAFileSystemTests coreXAFileSystemTests = new CoreXAFileSystemTests();
            if (testName.equals(CoreXAFileSystemTests.testNames.testConcurrentMoneyTransfer)) {
                coreXAFileSystemTests.testConcurrentMoneyTransfer(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testNames.testIOOperations)) {
                coreXAFileSystemTests.testIOOperations(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testNames.testDynamicReadWrite)) {
                coreXAFileSystemTests.testDynamicReadWrite(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testNames.testFileSystemEventing)) {
                coreXAFileSystemTests.testFileSystemEventing(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testNames.testConcurrentMoneyTransferPostCrash)) {
                coreXAFileSystemTests.testConcurrentMoneyTransferPostCrash(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testNames.testDynamicReadWritePostCrash)) {
                coreXAFileSystemTests.testDynamicReadWritePostCrash(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testNames.testIOOperationsPostCrash)) {
                coreXAFileSystemTests.testIOOperationsPostCrash(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testNames.testFileSystemEventingPostCrash)) {
                coreXAFileSystemTests.testFileSystemEventingPostCrash(testDirectory);
            }
        } catch (Throwable t) {
            System.out.println("Test failed " + testName + " in " + testDirectory + " due to " + t);
            t.printStackTrace();
        }
    }
}
