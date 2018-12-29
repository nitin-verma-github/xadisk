/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.tests.correctness;

import java.io.File;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.bridge.proxies.interfaces.XASession;
import org.xadisk.filesystem.NativeSession;
import org.xadisk.filesystem.NativeXASession;
import org.xadisk.filesystem.SessionCommonness;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

/*For testing, we used Atomikos (open source version) as a JTA implementation. One can get it from
 http://www.atomikos.com/Main/TransactionsEssentials .
 */
public class TestStandaloneXA {

    public static void main(String args[]) {
        try {
            boolean testRemote = false;
            int remotePort = 4678;
            StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration("C:\\xa", "local");
            configuration.setEnableRemoteInvocations(Boolean.TRUE);
            configuration.setServerAddress("localhost");
            configuration.setServerPort(remotePort);
            XAFileSystem xafs;
            XAFileSystem nativeXAFS = XAFileSystemProxy.bootNativeXAFileSystem(configuration);
            nativeXAFS.waitForBootup(-1);
            if (testRemote) {
                xafs = XAFileSystemProxy.getRemoteXAFileSystemReference("localhost", remotePort);
            } else {
                xafs = nativeXAFS;
            }
            XASession xaSession = xafs.createSessionForXATransaction();
            XAResource xar = xaSession.getXAResource();
            TransactionManager tm = new com.atomikos.icatch.jta.UserTransactionManager();
            //UNCOMMENT ABOVE ONCE YOU BRING ATOMIKOS INTO THE CLASSPATH.
            //TransactionManager tm = null;
            tm.setTransactionTimeout(60);

            File f1 = new File("C:\\1.txt");
            File f2 = new File("C:\\2.txt");
            File f3 = new File("C:\\3.txt");
            f1.delete();
            f2.delete();
            f3.delete();

            tm.begin();
            Transaction tx1 = tm.getTransaction();
            tx1.enlistResource(xar);
            xaSession.createFile(f1, false);
            tm.suspend();

            tm.begin();
            Transaction tx2 = tm.getTransaction();
            tx2.enlistResource(xar);
            xaSession.createFile(f2, false);
            tm.commit();

            tm.resume(tx1);
            tm.commit();

            tm.begin();
            Transaction tx3 = tm.getTransaction();
            tx3.enlistResource(xar);
            xaSession.createFile(f3, false);
            ((SessionCommonness) ((NativeXASession) xaSession).getSessionForCurrentWorkAssociation()).prepare();

            nativeXAFS.shutdown();
            nativeXAFS = XAFileSystemProxy.bootNativeXAFileSystem(configuration);

            if (testRemote) {
                xafs = XAFileSystemProxy.getRemoteXAFileSystemReference("localhost", remotePort);
            } else {
                xafs = nativeXAFS;
            }
            xar = xafs.getXAResourceForRecovery();
            Xid xids[] = xar.recover(XAResource.TMSTARTRSCAN);
            System.out.println(xids.length);
            xar.commit(xids[0], true);

            nativeXAFS.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
