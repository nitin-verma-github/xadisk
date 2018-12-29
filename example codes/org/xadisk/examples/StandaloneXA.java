/*
Copyright © 2010-2011, Nitin Verma (project owner for XADisk http://xadisk.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */


package org.xadisk.examples;

import java.io.File;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.bridge.proxies.interfaces.XASession;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

/**
 * This is a very basic example showing how XADisk can be used with an standalone
 * Transaction Manager like Atomikos.
 */
/**
 * How to run this example:
 *
 * 1) Change the various constants used in the code below according to your environment: xadiskSystemDirectory, testFile.
 * 2) To compile/run this example, one needs:
 *      - XADisk.jar
 *      - JCA API jar, which can be downloaded from http://download.java.net/maven/1/javax.resource/jars/connector-api-1.5.jar
 *      - Java 5 or above
 *      - a JTA Transaction Manager (e.g. Atomikos).
 */
/**
 * Please refer to the XADisk JavaDoc and User Guide for knowing more about using XADisk.
 */
public class StandaloneXA {

    //the XADisk System directory.
    private static final String xadiskSystemDirectory = "C:\\XADiskSystem1";
    //test-file on which a sample operation will be invoked via XADisk.
    private static final String testFile = "C:\\testXA.txt";

    public static void main(String args[]) {
        XAFileSystem xafs = null;
        try {
            System.out.println("Botting an XADisk instance...");
            StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration(xadiskSystemDirectory, "1");
            xafs = XAFileSystemProxy.bootNativeXAFileSystem(configuration);
            xafs.waitForBootup(-1);
            System.out.println("Successfully booted the XADisk instance.\n");

            TransactionManager tm = new com.atomikos.icatch.jta.UserTransactionManager();

            System.out.println("Starting an XA transaction...\n");
            tm.begin();
            Transaction tx1 = tm.getTransaction();

            XASession xaSession = xafs.createSessionForXATransaction();

            System.out.println("Enlisting XADisk in the XA transaction.");
            XAResource xarXADisk = xaSession.getXAResource();
            tx1.enlistResource(xarXADisk);

            System.out.println("Enlisting other XA-enabled resources (e.g. Oracle, MQ) in the XA transaction.\n");
            /*XAResource xarOracle = null;
            tx1.enlistResource(xarOracle);*/

            /*XAResource xarMQ = null;
            tx1.enlistResource(xarMQ);*/

            System.out.println("Performing transactional work over XADisk and other involved resources (e.g. Oracle, MQ)\n");
            xaSession.createFile(new File(testFile), false);

            System.out.println("Completed all the work. Now committing the XA transaction...");
            tm.commit();
            System.out.println("Successfully committed the XA transaction.");

            xafs.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
