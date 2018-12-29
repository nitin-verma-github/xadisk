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
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

/**
 * This is a very basic example which
 *      - boots an XADisk instance.
 *      - once this XADisk instance is up, a small application module is invoked, which does
 *          some file-operations via the XADisk instance.
 *      - can keep the XADisk running so that remote client applications can invoke it.
 */
/**
 * How to run this example:
 *
 * 1) Change the various constants used in the code below according to your environment: thisMachineAddress, port,
 *          testFile and keepXADiskRunning.
 * 2) To compile/run this example, one needs:
 *      - XADisk.jar
 *      - JCA API jar, which can be downloaded from http://download.java.net/maven/1/javax.resource/jars/connector-api-1.5.jar
 *      - Java 5 or above
 */
/**
 * Please refer to the XADisk JavaDoc and User Guide for knowing more about using XADisk.
 */
public class BootXADisk {

    //network address of this machine. Remote client applications willing to call this XADisk instance
    //should be able to connect to this address.
    private static final String thisMachineAddress = "localhost";
    //network port of this machine on which this XADisk instance should listen for calls from remote
    //client applications.
    private static final int port = 5151;
    //the XADisk System directory.
    private static final String xadiskSystemDirectory = "C:\\XADiskSystem1";
    //test-file on which operations will be invoked via XADisk.
    private static final String testFile = "C:\\orders1.csv";
    //specify whether the XADisk instance should be shutdown after the application-module completes. You
    //can leaving it running to allow remote client applications to invoke it.
    private static final boolean keepXADiskRunning = true;

    public static void main(String args[]) {
        try {
            bootNativeXADiskInstance("id-1");

            runApplicationModule1("id-1");

            if (!keepXADiskRunning) {
                shutdownNativeXADiskInstance("id-1");
            } else {
                System.out.println("The XADisk instance will keep running in this JVM...");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void bootNativeXADiskInstance(String instanceId) throws Exception {
        StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration(xadiskSystemDirectory + "\\" + instanceId,
                instanceId);
        configuration.setEnableRemoteInvocations(true);
        configuration.setServerAddress(thisMachineAddress);
        configuration.setServerPort(port);

        System.out.println("Botting an instance of XADisk. This instance can serve applications running on the same JVM, or "
                + "remote applications through [" + thisMachineAddress + ":" + port + "]");
        XAFileSystem xaf = XAFileSystemProxy.bootNativeXAFileSystem(configuration);

        xaf.waitForBootup(10000L);
        System.out.println("Booting completed for the XADisk instance.");
    }

    private static void shutdownNativeXADiskInstance(String instanceId) throws Exception {
        XAFileSystem nativeXAF = XAFileSystemProxy.getNativeXAFileSystemReference(instanceId);

        System.out.println("Shutting down the XADisk instance...");
        nativeXAF.shutdown();

        System.out.println("Shutdown completed.");
    }

    private static void runApplicationModule1(String instanceId) throws Exception {
        System.out.println("Executing the application-module1...");

        File businessData = new File(testFile);

        System.out.println("Obtaining a reference to the native XADisk instance...");
        XAFileSystem nativeXAF = XAFileSystemProxy.getNativeXAFileSystemReference(instanceId);
        System.out.println("Obtaining a session from the native XADisk instance...");
        Session session = nativeXAF.createSessionForLocalTransaction();

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
        }
    }
}
