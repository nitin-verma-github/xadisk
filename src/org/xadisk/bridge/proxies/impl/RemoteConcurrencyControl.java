/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.impl;

import java.io.File;
import java.util.ArrayList;
import org.xadisk.bridge.proxies.facilitators.RemoteMethodInvoker;
import org.xadisk.bridge.proxies.facilitators.RemoteObjectProxy;
import org.xadisk.filesystem.ConcurrencyControl;
import org.xadisk.filesystem.Lock;
import org.xadisk.filesystem.TransactionInformation;
import org.xadisk.filesystem.exceptions.AncestorPinnedException;
import org.xadisk.filesystem.exceptions.DeadLockVictimizedException;
import org.xadisk.filesystem.exceptions.DirectoryPinningFailedException;
import org.xadisk.filesystem.exceptions.LockingFailedException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;
import org.xadisk.filesystem.exceptions.TransactionTimeoutException;

public class RemoteConcurrencyControl extends RemoteObjectProxy implements ConcurrencyControl {

    private static final long serialVersionUID = 1L;

    public RemoteConcurrencyControl(String serverAddress, int serverPort) {
        super(1, new RemoteMethodInvoker(serverAddress, serverPort));
    }

    public RemoteConcurrencyControl getNewInstance() {
        return new RemoteConcurrencyControl(this.invoker.getServerAddress(), this.invoker.getServerPort());
    }

    private RemoteTransactionInformation convertToRemoteTransactionInformation(TransactionInformation transactionInformation) {
        return new RemoteTransactionInformation(transactionInformation, invoker.getServerAddress(), invoker.getServerPort());
    }

    public Lock acquireFileLock(TransactionInformation requestor, File f, long time, boolean exclusive) throws
            LockingFailedException, InterruptedException, TransactionRolledbackException,
            DeadLockVictimizedException, TransactionTimeoutException {
        try {
            return (Lock) invokeRemoteMethod("acquireFileLock", convertToRemoteTransactionInformation(requestor), f, time, exclusive);
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (DeadLockVictimizedException dlve) {
            throw dlve;
        } catch (TransactionTimeoutException tte) {
            throw tte;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void pinDirectoryForRename(File dir, TransactionInformation requestor) throws
            DirectoryPinningFailedException, AncestorPinnedException {
        try {
            invokeRemoteMethod("pinDirectoryForRename", dir, convertToRemoteTransactionInformation(requestor));
        } catch (DirectoryPinningFailedException dpfe) {
            throw dpfe;
        } catch (AncestorPinnedException ape) {
            throw ape;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void releaseLock(TransactionInformation releasor, Lock lock) {
        try {
            invokeRemoteMethod("releaseLock", convertToRemoteTransactionInformation(releasor), lock);
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void releaseRenamePinOnDirectory(File dir) {
        try {
            invokeRemoteMethod("releaseRenamePinOnDirectory", dir);
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void releaseRenamePinOnDirectories(ArrayList<File> dirs) {
        try {
            invokeRemoteMethod("releaseRenamePinOnDirectories", dirs);
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void interruptTransactionIfWaitingForResourceLock(TransactionInformation xid, byte cause) {
        try {
            invokeRemoteMethod("interruptTransactionIfWaitingForResourceLock", convertToRemoteTransactionInformation(xid), cause);
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void shutdown() {
        disconnect();
    }
}
