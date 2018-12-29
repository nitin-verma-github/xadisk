/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.impl;

import org.xadisk.bridge.proxies.facilitators.RemoteMethodInvoker;
import org.xadisk.bridge.proxies.facilitators.RemoteObjectProxy;
import java.io.File;
import org.xadisk.filesystem.SessionCommonness;
import org.xadisk.filesystem.exceptions.DirectoryNotEmptyException;
import org.xadisk.filesystem.exceptions.FileAlreadyExistsException;
import org.xadisk.filesystem.exceptions.FileNotExistsException;
import org.xadisk.filesystem.exceptions.FileUnderUseException;
import org.xadisk.filesystem.exceptions.InsufficientPermissionOnFileException;
import org.xadisk.filesystem.exceptions.LockingFailedException;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;

public class RemoteSession extends RemoteObjectProxy implements SessionCommonness {

    private static final long serialVersionUID = 1L;

    public RemoteSession(long objectId, RemoteMethodInvoker invoker) {
        super(objectId, invoker);
    }

    public RemoteXAFileInputStream createXAFileInputStream(File f, boolean lockExclusively) throws
            FileNotExistsException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        try {
            return (RemoteXAFileInputStream) invokeRemoteMethod("createXAFileInputStream", f, lockExclusively);
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public RemoteXAFileInputStream createXAFileInputStream(File f) throws
            FileNotExistsException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        try {
            return (RemoteXAFileInputStream) invokeRemoteMethod("createXAFileInputStream", f);
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public RemoteXAFileOutputStream createXAFileOutputStream(File f, boolean heavyWrite) throws FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        try {
            return (RemoteXAFileOutputStream) invokeRemoteMethod("createXAFileOutputStream", f, heavyWrite);
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (FileUnderUseException fuue) {
            throw fuue;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void copyFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        try {
            invokeRemoteMethod("copyFile", src, dest);
        } catch (FileAlreadyExistsException faee) {
            throw faee;
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void createFile(File f, boolean isDirectory) throws FileAlreadyExistsException, FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException, NoTransactionAssociatedException,
            InterruptedException {
        try {
            invokeRemoteMethod("createFile", f, isDirectory);
        } catch (FileAlreadyExistsException faee) {
            throw faee;
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void deleteFile(File f) throws DirectoryNotEmptyException, FileNotExistsException, FileUnderUseException,
            InsufficientPermissionOnFileException, LockingFailedException, NoTransactionAssociatedException,
            InterruptedException {
        try {
            invokeRemoteMethod("deleteFile", f);
        } catch (DirectoryNotEmptyException dnee) {
            throw dnee;
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (FileUnderUseException fuue) {
            throw fuue;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public boolean fileExists(File f, boolean lockExclusively) throws LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException, InterruptedException {
        try {
            return (Boolean) invokeRemoteMethod("fileExists", f, lockExclusively);
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public boolean fileExists(File f) throws LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException, InterruptedException {
        try {
            return (Boolean) invokeRemoteMethod("fileExists", f);
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public boolean fileExistsAndIsDirectory(File f, boolean lockExclusively) throws LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException, InterruptedException {
        try {
            return (Boolean) invokeRemoteMethod("fileExistsAndIsDirectory", f, lockExclusively);
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public boolean fileExistsAndIsDirectory(File f) throws LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException, InterruptedException {
        try {
            return (Boolean) invokeRemoteMethod("fileExistsAndIsDirectory", f);
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public long getFileLength(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException, InterruptedException {
        try {
            return (Long) invokeRemoteMethod("getFileLength", f, lockExclusively);
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public long getFileLength(File f) throws FileNotExistsException, LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException, InterruptedException {
        try {
            return (Long) invokeRemoteMethod("getFileLength", f);
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public String[] listFiles(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException, InterruptedException {
        try {
            return (String[]) invokeRemoteMethod("listFiles", f, lockExclusively);
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public String[] listFiles(File f) throws FileNotExistsException, LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException, InterruptedException {
        try {
            return (String[]) invokeRemoteMethod("listFiles", f);
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void moveFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException {
        try {
            invokeRemoteMethod("moveFile", src, dest);
        } catch (FileAlreadyExistsException faee) {
            throw faee;
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (FileUnderUseException fuue) {
            throw fuue;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void truncateFile(File f, long newLength) throws FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException, NoTransactionAssociatedException,
            InterruptedException {
        try {
            invokeRemoteMethod("truncateFile", f, newLength);
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void prepare() throws NoTransactionAssociatedException {
        try {
            invokeRemoteMethod("prepare");
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void commit() throws NoTransactionAssociatedException {
        this.commit(true);
    }

    public void commit(boolean onePhase) throws NoTransactionAssociatedException {
        try {
            invokeRemoteMethod("commit", onePhase);
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void rollback() throws NoTransactionAssociatedException {
        try {
            invokeRemoteMethod("rollback");
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void setPublishFileStateChangeEventsOnCommit(boolean publish) {
        try {
            invokeRemoteMethod("setPublishFileStateChangeEventsOnCommit", publish);
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public boolean getPublishFileStateChangeEventsOnCommit() {
        try {
            return (Boolean) invokeRemoteMethod("getPublishFileStateChangeEventsOnCommit");
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public boolean setTransactionTimeout(int transactionTimeout) {
        try {
            return (Boolean) invokeRemoteMethod("setTransactionTimeout", transactionTimeout);
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public int getTransactionTimeout() {
        try {
            return (Integer) invokeRemoteMethod("getTransactionTimeout");
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public long getFileLockWaitTimeout() {
        try {
            return (Long) invokeRemoteMethod("getFileLockWaitTimeout");
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void setFileLockWaitTimeout(long fileLockWaitTimeout) {
        try {
            invokeRemoteMethod("setFileLockWaitTimeout", fileLockWaitTimeout);
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public boolean isUsingReadOnlyOptimization() {
        try {
            return (Boolean) invokeRemoteMethod("isUsingReadOnlyOptimization");
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void completeReadOnlyTransaction() throws NoTransactionAssociatedException {
        try {
            invokeRemoteMethod("completeReadOnlyTransaction");
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }
}
