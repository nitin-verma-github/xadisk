/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem;

import java.io.File;
import javax.resource.ResourceException;
import javax.transaction.xa.XAResource;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XASession;
import org.xadisk.connector.XAResourceImpl;
import org.xadisk.filesystem.exceptions.DirectoryNotEmptyException;
import org.xadisk.filesystem.exceptions.FileAlreadyExistsException;
import org.xadisk.filesystem.exceptions.FileNotExistsException;
import org.xadisk.filesystem.exceptions.FileUnderUseException;
import org.xadisk.filesystem.exceptions.InsufficientPermissionOnFileException;
import org.xadisk.filesystem.exceptions.LockingFailedException;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;

public class NativeXASession implements XASession {

    private volatile Session sessionOfXATransaction;
    private volatile Session sessionOfLocalTransaction;
    private volatile XAResourceImpl xaResourceImpl;
    private boolean publishFileStateChangeEventsOnCommit = false;
    private volatile byte typeOfCurrentTransaction = NO_TRANSACTION;
    protected volatile XAFileSystem theXAFileSystem;
    public static final byte NO_TRANSACTION = 0;
    public static final byte LOCAL_TRANSACTION = 1;
    public static final byte XA_TRANSACTION = 2;

    public NativeXASession(XAFileSystem xaFileSystem, String instanceId) {
        this.theXAFileSystem = xaFileSystem;
        this.xaResourceImpl = new XAResourceImpl(this);
    }

    public XAFileSystem getUnderlyingXAFileSystem() {
        return theXAFileSystem;
    }

    public XAResource getXAResource() {
        return xaResourceImpl;
    }

    protected void cleanup() throws ResourceException {
        this.xaResourceImpl = new XAResourceImpl(this);
        //DO NOT clear the listeners. When trying to implement pooling, this guy
        //tested my patience. [this.listeners.clear();]
        this.publishFileStateChangeEventsOnCommit = false;
        this.sessionOfLocalTransaction = null;
        this.sessionOfXATransaction = null;
        this.typeOfCurrentTransaction = NO_TRANSACTION;
    }

    public XAFileInputStream createXAFileInputStream(File f, boolean lockExclusively) throws
            FileNotExistsException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        return getSessionForCurrentWorkAssociation().createXAFileInputStream(f, lockExclusively);
    }

    public XAFileInputStream createXAFileInputStream(File f) throws
            FileNotExistsException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        return getSessionForCurrentWorkAssociation().createXAFileInputStream(f);
    }

    public XAFileOutputStream createXAFileOutputStream(File f, boolean heavyWrite) throws
            FileNotExistsException, FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        return getSessionForCurrentWorkAssociation().createXAFileOutputStream(f, heavyWrite);
    }

    public void createFile(File f, boolean isDirectory) throws
            FileAlreadyExistsException, FileNotExistsException, InsufficientPermissionOnFileException,
            LockingFailedException, NoTransactionAssociatedException,
            InterruptedException {
        getSessionForCurrentWorkAssociation().createFile(f, isDirectory);
    }

    public void deleteFile(File f) throws DirectoryNotEmptyException, FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        getSessionForCurrentWorkAssociation().deleteFile(f);
    }

    public void copyFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        getSessionForCurrentWorkAssociation().copyFile(src, dest);
    }

    public void moveFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        getSessionForCurrentWorkAssociation().moveFile(src, dest);
    }

    public boolean fileExists(File f, boolean lockExclusively) throws LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException {
        return getSessionForCurrentWorkAssociation().fileExists(f, lockExclusively);
    }

    public boolean fileExists(File f) throws LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException {
        return getSessionForCurrentWorkAssociation().fileExists(f);
    }

    public boolean fileExistsAndIsDirectory(File f, boolean lockExclusively) throws LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException {
        return getSessionForCurrentWorkAssociation().fileExistsAndIsDirectory(f, lockExclusively);
    }

    public boolean fileExistsAndIsDirectory(File f) throws LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException {
        return getSessionForCurrentWorkAssociation().fileExistsAndIsDirectory(f);
    }

    public String[] listFiles(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException {
        return getSessionForCurrentWorkAssociation().listFiles(f, lockExclusively);
    }

    public String[] listFiles(File f) throws FileNotExistsException, LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException {
        return getSessionForCurrentWorkAssociation().listFiles(f);
    }

    public long getFileLength(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException {
        return getSessionForCurrentWorkAssociation().getFileLength(f, lockExclusively);
    }

    public long getFileLength(File f) throws FileNotExistsException, LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException {
        return getSessionForCurrentWorkAssociation().getFileLength(f);
    }

    public void truncateFile(File f, long newLength) throws FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        getSessionForCurrentWorkAssociation().truncateFile(f, newLength);
    }

    public boolean getPublishFileStateChangeEventsOnCommit() {
        return publishFileStateChangeEventsOnCommit;
    }

    public long getFileLockWaitTimeout() {
        try {
            return getSessionForCurrentWorkAssociation().getFileLockWaitTimeout();
        } catch (NoTransactionAssociatedException ntae) {
            return -1;
        }
    }

    public void setPublishFileStateChangeEventsOnCommit(boolean publishFileStateChangeEventsOnCommit) {
        this.publishFileStateChangeEventsOnCommit = publishFileStateChangeEventsOnCommit;
        switch (typeOfCurrentTransaction) {
            case LOCAL_TRANSACTION:
                sessionOfLocalTransaction.setPublishFileStateChangeEventsOnCommit(
                        publishFileStateChangeEventsOnCommit);
                break;
            case XA_TRANSACTION:
                sessionOfXATransaction.setPublishFileStateChangeEventsOnCommit(
                        publishFileStateChangeEventsOnCommit);
                break;
        }
    }

    public void setFileLockWaitTimeout(long fileLockWaitTimeout) {
        try {
            getSessionForCurrentWorkAssociation().setFileLockWaitTimeout(fileLockWaitTimeout);
        } catch (NoTransactionAssociatedException ntae) {
        }
    }

    public Session getSessionOfLocalTransaction() {
        return sessionOfLocalTransaction;
    }

    public void setSessionOfExistingXATransaction(Session session) {
        this.sessionOfXATransaction = session;
    }

    public Session refreshSessionForNewXATransaction(TransactionInformation xid) {
        this.sessionOfXATransaction = ((XAFileSystemCommonness) theXAFileSystem).createSessionForXATransaction(xid);
        this.sessionOfXATransaction.setPublishFileStateChangeEventsOnCommit(publishFileStateChangeEventsOnCommit);
        return this.sessionOfXATransaction;
    }

    public Session refreshSessionForBeginLocalTransaction() {
        this.sessionOfLocalTransaction = theXAFileSystem.createSessionForLocalTransaction();
        this.sessionOfLocalTransaction.setPublishFileStateChangeEventsOnCommit(publishFileStateChangeEventsOnCommit);
        return this.sessionOfLocalTransaction;
    }

    public void setTypeOfCurrentTransaction(byte typeOfCurrentTransaction) {
        this.typeOfCurrentTransaction = typeOfCurrentTransaction;
    }

    public Session getSessionForCurrentWorkAssociation() throws NoTransactionAssociatedException {
        switch (typeOfCurrentTransaction) {
            case LOCAL_TRANSACTION:
                return sessionOfLocalTransaction;
            case XA_TRANSACTION:
                return sessionOfXATransaction;
        }
        throw new NoTransactionAssociatedException();
    }
}
