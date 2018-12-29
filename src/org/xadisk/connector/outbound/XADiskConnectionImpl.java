/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.connector.outbound;

import java.io.File;
import org.xadisk.bridge.proxies.interfaces.XADiskRemoteConnection;
import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.filesystem.exceptions.DirectoryNotEmptyException;
import org.xadisk.filesystem.exceptions.FileAlreadyExistsException;
import org.xadisk.filesystem.exceptions.FileNotExistsException;
import org.xadisk.filesystem.exceptions.FileUnderUseException;
import org.xadisk.filesystem.exceptions.InsufficientPermissionOnFileException;
import org.xadisk.filesystem.exceptions.LockingFailedException;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;

public class XADiskConnectionImpl implements XADiskConnection, XADiskRemoteConnection {

    private volatile XADiskManagedConnection mc;
    private final XADiskUserLocalTransaction userLocalTransaction;
    private boolean publishFileStateChangeEventsOnCommit = false;

    protected XADiskConnectionImpl(XADiskManagedConnection mc) {
        this.mc = mc;
        this.userLocalTransaction = new XADiskUserLocalTransaction(mc);
    }

    protected void setManagedConnection(XADiskManagedConnection mc) {
        this.mc = mc;
    }

    public XADiskUserLocalTransaction getUserLocalTransaction() {
        return userLocalTransaction;
    }

    public XAFileInputStream createXAFileInputStream(File f, boolean lockExclusively) throws
            FileNotExistsException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        return mc.getSessionForCurrentWorkAssociation().createXAFileInputStream(f, lockExclusively);
    }

    public XAFileInputStream createXAFileInputStream(File f) throws
            FileNotExistsException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        return mc.getSessionForCurrentWorkAssociation().createXAFileInputStream(f);
    }

    public XAFileOutputStream createXAFileOutputStream(File f, boolean heavyWrite) throws
            FileNotExistsException, FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        return mc.getSessionForCurrentWorkAssociation().createXAFileOutputStream(f, heavyWrite);
    }

    public void createFile(File f, boolean isDirectory) throws
            FileAlreadyExistsException, FileNotExistsException, InsufficientPermissionOnFileException,
            LockingFailedException, NoTransactionAssociatedException,
            InterruptedException {
        mc.getSessionForCurrentWorkAssociation().createFile(f, isDirectory);
    }

    public void deleteFile(File f) throws DirectoryNotEmptyException, FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        mc.getSessionForCurrentWorkAssociation().deleteFile(f);
    }

    public void copyFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        mc.getSessionForCurrentWorkAssociation().copyFile(src, dest);
    }

    public void moveFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        mc.getSessionForCurrentWorkAssociation().moveFile(src, dest);
    }

    public boolean fileExists(File f, boolean lockExclusively) throws LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException {
        return mc.getSessionForCurrentWorkAssociation().fileExists(f, lockExclusively);
    }

    public boolean fileExists(File f) throws LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException {
        return mc.getSessionForCurrentWorkAssociation().fileExists(f);
    }

    public boolean fileExistsAndIsDirectory(File f, boolean lockExclusively) throws LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException {
        return mc.getSessionForCurrentWorkAssociation().fileExistsAndIsDirectory(f, lockExclusively);
    }

    public boolean fileExistsAndIsDirectory(File f) throws LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException {
        return mc.getSessionForCurrentWorkAssociation().fileExistsAndIsDirectory(f);
    }

    public String[] listFiles(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException {
        return mc.getSessionForCurrentWorkAssociation().listFiles(f, lockExclusively);
    }

    public String[] listFiles(File f) throws FileNotExistsException, LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException {
        return mc.getSessionForCurrentWorkAssociation().listFiles(f);
    }

    public long getFileLength(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException {
        return mc.getSessionForCurrentWorkAssociation().getFileLength(f, lockExclusively);
    }

    public long getFileLength(File f) throws FileNotExistsException, LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException {
        return mc.getSessionForCurrentWorkAssociation().getFileLength(f);
    }

    public void truncateFile(File f, long newLength) throws FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        mc.getSessionForCurrentWorkAssociation().truncateFile(f, newLength);
    }

    public void close() {
        mc.connectionClosed(this);
    }

    public boolean getPublishFileStateChangeEventsOnCommit() {
        return publishFileStateChangeEventsOnCommit;
    }

    public void setPublishFileStateChangeEventsOnCommit(boolean publishFileStateChangeEventsOnCommit) {
        this.publishFileStateChangeEventsOnCommit = publishFileStateChangeEventsOnCommit;
        this.mc.setPublishFileStateChangeEventsOnCommit(publishFileStateChangeEventsOnCommit);
    }

    public long getFileLockWaitTimeout() {
        return this.mc.getFileLockWaitTimeout();
    }

    public void setFileLockWaitTimeout(long fileLockWaitTimeout) {
        this.mc.setFileLockWaitTimeout(fileLockWaitTimeout);
    }
}
