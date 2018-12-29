/*
Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/


package org.xadisk.examples;

import java.io.File;
import org.xadisk.additional.XAFileOutputStreamWrapper;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.filesystem.exceptions.XAApplicationException;


/**
 * This is a very basic example to show how to work with remotely running XADisk instances. Such
 * remote XADisk instance could either be deployed as a JCA Resource Adapter inside a JavaEE server, or
 * it could be running in any JVM (e.g. as done in "BootXADisk" example).
 * The below example will work in both cases as long as the correct remote address/port are specified.
 */
/**
 * How to run this example:
 *
 * 1) Change the various constants used in the code below: remoteXADiskAddress, remoteXADiskPort and testFile.
 * 2) Bring the remote XADisk instance up, if it is not up already.
 * 3) To compile/run this example, one needs:
 *      - XADisk.jar
 *      - JCA API jar, which can be downloaded from http://download.java.net/maven/1/javax.resource/jars/connector-api-1.5.jar
 *      - Java 5 or above
 */
/**
 * Please refer to the XADisk JavaDoc and User Guide for knowing more about using XADisk.
 */
public class CallRemoteXADisk {

    //network address of the machine on which the remote XADisk instance is running.
    private static final String remoteXADiskAddress = "localhost";
    //network port on which the remote XADisk is instance listening.
    private static final int remoteXADiskPort = 5151;
    //test file, on the file-system wrapped by the remote XADisk instance.
    private static final String testFile = "C:\\orders2.csv";

    public static void main(String args[]) {
        try {
            runApplicationModule1();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runApplicationModule1() throws Exception {

        System.out.println("Executing the application-module1...");

        File businessData = new File(testFile);

        System.out.println("Obtaining a reference to the remote XADisk instance...");
        XAFileSystem remoteXAF = XAFileSystemProxy.getRemoteXAFileSystemReference(remoteXADiskAddress, remoteXADiskPort);

        System.out.println("Obtaining a session from the remote XADisk instance...");
        Session session = remoteXAF.createSessionForLocalTransaction();

        try {
            System.out.println("Doing some operations on the file-system via XADisk...");
            if (!session.fileExists(businessData)) {
                session.createFile(businessData, false);
            }

            XAFileOutputStream xaFOS = session.createXAFileOutputStream(businessData, false);
            XAFileOutputStreamWrapper wrapperOS = new XAFileOutputStreamWrapper(xaFOS);
            wrapperOS.write("Coffee Beans, 5, 100, Street #11, Moon - 311674 \n".getBytes());
            wrapperOS.close();

            System.out.println("Committing the transaction...");
            session.commit();

            System.out.println("The application-module1 completed successfully.");
        } catch (XAApplicationException xaae) {
            System.out.println("The application-module1 could not execute successfully.");
            xaae.printStackTrace();
            System.out.println("Rolling back the transaction...");
            session.rollback();
        } finally {
            System.out.println("Disconnecting from the remote XADisk instance...");
            remoteXAF.shutdown();
        }
    }
}
