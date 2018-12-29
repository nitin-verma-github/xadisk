/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.tests.correctness;

import java.io.File;
import java.io.IOException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.bridge.proxies.impl.RemoteXAFileSystem;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.connector.inbound.EndPointActivation;
import org.xadisk.connector.inbound.XADiskActivationSpecImpl;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.SessionCommonness;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

public class TestRemoteInboundMessaging {

    private static final String SEPERATOR = File.separator;
    private static final String currentWorkingDirectory = "C:\\test";
    private static final String XADiskSystemDirectory = currentWorkingDirectory + SEPERATOR + "XADiskSystemLocal";

    public static void main(String args[]) {
        try {
            TestUtility.cleanupDirectory(new File(XADiskSystemDirectory));
            TestUtility.cleanupDirectory(new File(currentWorkingDirectory));
            TestUtility.cleanupDirectory(new File(RemoteXADiskBootup.XADiskSystemDirectory));

            File f = new File(currentWorkingDirectory + "\\a.txt");

            System.out.println("Remember to set the cleanupSystemDir flag in RemoteXADiskBootup class to false");
            NativeXAFileSystem nativeXAFS = bootLocalXADisk();

            SimulatedMessageEndpointFactory mef = new SimulatedMessageEndpointFactory();
            mef.goTill = SimulatedMessageEndpointFactory.GoTill.commit;
            XADiskActivationSpecImpl as = new XADiskActivationSpecImpl();
            as.setAreFilesRemote("true");
            as.setRemoteServerAddress("localhost");
            as.setRemoteServerPort(RemoteXADiskBootup.DEFAULT_PORT + "");
            as.setFileNamesAndEventInterests(currentWorkingDirectory + "\\::111");
            EndPointActivation activation = new EndPointActivation(mef, as);

            bootRemoteXADisk();
            RemoteXAFileSystem remoteXAFS = new RemoteXAFileSystem("localhost", RemoteXADiskBootup.DEFAULT_PORT, NativeXAFileSystem.getXAFileSystem("local"));
            remoteXAFS.registerEndPointActivation(activation);

            Session session = remoteXAFS.createSessionForLocalTransaction();

            session.createFile(f, false);
            session.setPublishFileStateChangeEventsOnCommit(true);
            session.commit();
            createSpaceForMessageDelivery();

            session = remoteXAFS.createSessionForLocalTransaction();
            session.deleteFile(f);
            session.setPublishFileStateChangeEventsOnCommit(true);
            ((SessionCommonness) session).prepare();

            System.out.println("Shutting down remote XADisk...");
            shutdownRemoteXADisk();

            bootRemoteXADisk();

            remoteXAFS = new RemoteXAFileSystem("localhost", RemoteXADiskBootup.DEFAULT_PORT, NativeXAFileSystem.getXAFileSystem("local"));
            XAResource xar = remoteXAFS.getXAResourceForRecovery();
            Xid xids[] = xar.recover(XAResource.TMSTARTRSCAN);
            for (Xid xid : xids) {
                System.out.println("Committing after crash...");
                xar.commit(xid, true);
            }
            createSpaceForMessageDelivery();

            remoteXAFS.deRegisterEndPointActivation(activation);
            remoteXAFS.registerEndPointActivation(activation);

            session = remoteXAFS.createSessionForLocalTransaction();
            session.createFile(f, false);
            session.setPublishFileStateChangeEventsOnCommit(true);
            session.commit();

            createSpaceForMessageDelivery();

            shutdownRemoteXADisk();
            bootRemoteXADisk();
            remoteXAFS = new RemoteXAFileSystem("localhost", RemoteXADiskBootup.DEFAULT_PORT, NativeXAFileSystem.getXAFileSystem("local"));

            session = remoteXAFS.createSessionForLocalTransaction();
            session.deleteFile(f);
            session.setPublishFileStateChangeEventsOnCommit(true);
            session.commit();

            createSpaceForMessageDelivery();

            shutdownRemoteXADisk();
            nativeXAFS.shutdown();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void bootRemoteXADisk() throws IOException {
        RemoteXADiskBootup.main(new String[0]);
    }

    private static void shutdownRemoteXADisk() throws IOException {
        XAFileSystemProxy.getNativeXAFileSystemReference("remote").shutdown();
    }

    private static NativeXAFileSystem bootLocalXADisk() throws InterruptedException {
        StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration(XADiskSystemDirectory, "local");
        configuration.setWorkManagerCorePoolSize(100);
        configuration.setWorkManagerMaxPoolSize(100);
        configuration.setServerPort(2345);
        configuration.setEnableRemoteInvocations(true);
        NativeXAFileSystem nativeXAFS = NativeXAFileSystem.bootXAFileSystemStandAlone(configuration);
        nativeXAFS.waitForBootup(-1);
        System.out.println("Booted local...");
        return nativeXAFS;
    }

    private static void createSpaceForMessageDelivery() throws InterruptedException {
        System.out.println("Expecting message delivery here...");
        Thread.sleep(3000);
    }
}
