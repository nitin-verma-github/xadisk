/*
 Copyright © 2010-2014, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem;

import org.xadisk.filesystem.pools.PooledBuffer;
import org.xadisk.filesystem.virtual.TransactionVirtualView;
import org.xadisk.filesystem.virtual.NativeXAFileOutputStream;
import org.xadisk.filesystem.virtual.NativeXAFileInputStream;
import org.xadisk.filesystem.virtual.VirtualViewFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;
import org.xadisk.bridge.proxies.impl.RemoteConcurrencyControl;
import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;
import org.xadisk.filesystem.exceptions.DeadLockVictimizedException;
import org.xadisk.filesystem.exceptions.DirectoryNotEmptyException;
import org.xadisk.filesystem.exceptions.FileAlreadyExistsException;
import org.xadisk.filesystem.exceptions.FileNotExistsException;
import org.xadisk.filesystem.exceptions.FileUnderUseException;
import org.xadisk.filesystem.exceptions.InsufficientPermissionOnFileException;
import org.xadisk.filesystem.exceptions.LockingFailedException;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;
import org.xadisk.filesystem.exceptions.TransactionFailedException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;
import org.xadisk.filesystem.exceptions.TransactionTimeoutException;
import org.xadisk.filesystem.exceptions.XASystemException;
import org.xadisk.filesystem.exceptions.internal.XASystemIOException;
import org.xadisk.filesystem.exceptions.XASystemNoMoreAvailableException;
import org.xadisk.filesystem.utilities.FileIOUtility;
import org.xadisk.filesystem.utilities.MiscUtils;

public class NativeSession implements SessionCommonness {

    private final HashMap<File, Lock> allAcquiredLocks = new HashMap<File, Lock>(1000);
    private final ArrayList<NativeXAFileInputStream> allAcquiredInputStreams = new ArrayList<NativeXAFileInputStream>(5);
    private final ArrayList<NativeXAFileOutputStream> allAcquiredOutputStreams = new ArrayList<NativeXAFileOutputStream>(5);
    private final NativeXAFileSystem xaFileSystem;
    private final ConcurrencyControl concurrencyControl;
    private volatile int transactionTimeout = 0;
    private final TransactionInformation xid;
    private boolean rolledbackPrematurely = false;
    private boolean sessionIsUseless = false;
    private volatile boolean startedCommitting = false;
    private Throwable rollbackCause = null;
    private volatile boolean systemHasFailed = false;
    private volatile boolean systemGotShutdown = false;
    private volatile boolean operationsCanContinue = true;
    private volatile Throwable systemFailureCause = null;
    private final TransactionVirtualView view;
    private long fileLockWaitTimeout = 0;
    private boolean createdForRecovery = false;
    private ArrayList<FileSystemStateChangeEvent> fileStateChangeEventsToRaise = new ArrayList<FileSystemStateChangeEvent>(10);
    private final ArrayList<File> directoriesPinnedInThisSession = new ArrayList<File>(5);
    private final long timeOfEntryToTransaction;
    private final ReentrantLock asynchronousRollbackLock = new ReentrantLock(false);
    private final ArrayList<Long> transactionLogPositions = new ArrayList<Long>(25);
    private final ArrayList<Buffer> transactionInMemoryBuffers = new ArrayList<Buffer>(25);
    private boolean publishFileStateChangeEventsOnCommit = false;
    private final HashMap<File, NativeXAFileOutputStream> fileAndOutputStream = new HashMap<File, NativeXAFileOutputStream>(1000);
    private boolean usingReadOnlyOptimization = true;
    private final DurableDiskSession diskSession;

    NativeSession(TransactionInformation xid, boolean createdForRecovery, NativeXAFileSystem xaFileSystem) {
        this.xid = xid;
        xid.setOwningSession(this);
        this.xaFileSystem = xaFileSystem;
        this.concurrencyControl = xaFileSystem.getConcurrencyControl();
        this.diskSession = xaFileSystem.createDurableDiskSession();
        this.createdForRecovery = createdForRecovery;
        if (createdForRecovery) {
            this.transactionTimeout = 0;
            this.view = null;
            this.timeOfEntryToTransaction = -100;
            this.usingReadOnlyOptimization = false;
        } else {
            this.transactionTimeout = xaFileSystem.getDefaultTransactionTimeout();
            this.fileLockWaitTimeout = this.xaFileSystem.getLockTimeOut();
            view = new TransactionVirtualView(xid, this, xaFileSystem, diskSession);
            timeOfEntryToTransaction = System.currentTimeMillis();
            xaFileSystem.assignSessionToTransaction(xid, this);
        }
    }

    public NativeSession(TransactionInformation xid, ArrayList<FileSystemStateChangeEvent> events, NativeXAFileSystem xaFileSystem) {
        this.xid = xid;
        xid.setOwningSession(this);
        this.xaFileSystem = xaFileSystem;
        this.concurrencyControl = xaFileSystem.getConcurrencyControl();
        this.diskSession = xaFileSystem.createDurableDiskSession();
        this.createdForRecovery = true;
        this.usingReadOnlyOptimization = false;
        this.transactionTimeout = 0;
        this.view = null;
        this.timeOfEntryToTransaction = -100;
        this.fileStateChangeEventsToRaise = events;
        this.publishFileStateChangeEventsOnCommit = true;
    }

    public void rollbackAsynchronously(Throwable rollbackCause) {
        try {
            asynchronousRollbackLock.lock();
            if (!startedCommitting) {
                rollbackPrematurely(rollbackCause);
            }
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    void rollbackPrematurely(Throwable rollbackCause) {
        try {
            rollback();
            this.rolledbackPrematurely = true;
            this.operationsCanContinue = false;
            this.rollbackCause = rollbackCause;
        } catch (TransactionRolledbackException trbe) {
        } catch (NoTransactionAssociatedException note) {
        }
    }

    void notifySystemFailure(Throwable systemFailureCause) {
        this.systemHasFailed = true;
        this.operationsCanContinue = false;
        this.systemFailureCause = systemFailureCause;
    }

    void notifySystemShutdown() {
        this.systemGotShutdown = true;
        this.operationsCanContinue = false;
    }

    public NativeXAFileInputStream createXAFileInputStream(File f)
            throws FileNotExistsException, InsufficientPermissionOnFileException, LockingFailedException,
            InterruptedException, NoTransactionAssociatedException {
        return createXAFileInputStream(f, false);
    }

    public NativeXAFileInputStream createXAFileInputStream(File f, boolean lockExclusively)
            throws FileNotExistsException, InsufficientPermissionOnFileException, LockingFailedException,
            InterruptedException, NoTransactionAssociatedException {
        f = f.getAbsoluteFile();
        Lock newLock = null;
        boolean success = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            newLock = acquireLockIfRequired(f, lockExclusively);
            checkPermission(PermissionType.READ_FILE, f);
            VirtualViewFile vvf = view.getVirtualViewFile(f);
            NativeXAFileInputStream temp = new NativeXAFileInputStream(vvf, this, xaFileSystem);
            allAcquiredInputStreams.add(temp);
            success = true;
            return temp;
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            try {
                if (!success) {
                    releaseLocks(newLock);
                }
            } finally {
                asynchronousRollbackLock.unlock();
            }
        }
    }

    public NativeXAFileOutputStream createXAFileOutputStream(File f, boolean heavyWrite) throws FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            InterruptedException, NoTransactionAssociatedException {
        f = f.getAbsoluteFile();
        Lock newLock = null;
        boolean success = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            newLock = acquireLockIfRequired(f, true);
            checkPermission(PermissionType.WRITE_FILE, f);
            VirtualViewFile vvf = view.getVirtualViewFile(f);
            NativeXAFileOutputStream temp = getCachedXAFileOutputStream(vvf, xid, heavyWrite, this);
            allAcquiredOutputStreams.add(temp);
            addToFileSystemEvents(FileSystemStateChangeEvent.FileSystemEventType.MODIFIED, f, false);
            success = true;
            usingReadOnlyOptimization = false;
            return temp;
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            try {
                if (!success) {
                    releaseLocks(newLock);
                }
            } finally {
                asynchronousRollbackLock.unlock();
            }
        }
    }

    public void createFile(File f, boolean isDirectory) throws FileAlreadyExistsException, FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException,
            InterruptedException, NoTransactionAssociatedException {
        f = f.getAbsoluteFile();
        Lock newLock = null;
        boolean success = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            newLock = acquireLockIfRequired(f, true);
            File parentFile = f.getParentFile();
            checkValidParent(f);
            checkPermission(PermissionType.WRITE_DIRECTORY, parentFile);
            view.createFile(f, isDirectory);
            byte operation = isDirectory ? TransactionLogEntry.DIR_CREATE : TransactionLogEntry.FILE_CREATE;
            ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, f.getAbsolutePath(),
                    operation));
            Buffer logEntry = new Buffer(logEntryBytes, xaFileSystem);
            xaFileSystem.getTheGatheringDiskWriter().submitBuffer(logEntry, xid);
            addToFileSystemEvents(FileSystemStateChangeEvent.FileSystemEventType.CREATED, f, isDirectory);
            success = true;
            usingReadOnlyOptimization = false;
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            try {
                if (!success) {
                    releaseLocks(newLock);
                }
            } finally {
                asynchronousRollbackLock.unlock();
            }
        }
    }

    public void deleteFile(File f) throws DirectoryNotEmptyException, FileNotExistsException, FileUnderUseException,
            InsufficientPermissionOnFileException, LockingFailedException,
            InterruptedException, NoTransactionAssociatedException {
        f = f.getAbsoluteFile();
        Lock newLock = null;
        boolean success = false;
        boolean isDirectory = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            newLock = acquireLockIfRequired(f, true);
            File parentFile = f.getParentFile();
            checkValidParent(f);
            checkPermission(PermissionType.WRITE_DIRECTORY, parentFile);
            isDirectory = view.deleteFile(f);
            ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, f.getAbsolutePath(),
                    TransactionLogEntry.FILE_DELETE));
            Buffer logEntry = new Buffer(logEntryBytes, xaFileSystem);
            xaFileSystem.getTheGatheringDiskWriter().submitBuffer(logEntry, xid);
            addToFileSystemEvents(FileSystemStateChangeEvent.FileSystemEventType.DELETED, f, isDirectory);
            success = true;
            usingReadOnlyOptimization = false;
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            try {
                if (!success) {
                    releaseLocks(newLock);
                }
            } finally {
                asynchronousRollbackLock.unlock();
            }
        }
    }

    public void moveFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            InterruptedException, NoTransactionAssociatedException {
        src = src.getAbsoluteFile();
        dest = dest.getAbsoluteFile();
        Lock newLocks[] = new Lock[2];
        boolean success = false;
        boolean isDirectoryMove = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            newLocks[0] = acquireLockIfRequired(src, true);
            newLocks[1] = acquireLockIfRequired(dest, true);
            File srcParentFile = src.getParentFile();
            checkValidParent(src);
            File destParentFile = dest.getParentFile();
            checkValidParent(dest);
            checkPermission(PermissionType.WRITE_DIRECTORY, srcParentFile);
            checkPermission(PermissionType.WRITE_DIRECTORY, destParentFile);

            if (view.fileExistsAndIsNormal(src)) {
                view.moveNormalFile(src, dest);
            } else if (view.fileExistsAndIsDirectory(src)) {
                isDirectoryMove = true;
                checkAnyOpenStreamToDescendantFiles(src);
                concurrencyControl.pinDirectoryForRename(src, xid);
                directoriesPinnedInThisSession.add(src);
                view.moveDirectory(src, dest);
            } else {
                throw new FileNotExistsException(src.getAbsolutePath());
            }
            ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, src.getAbsolutePath(),
                    dest.getAbsolutePath(), TransactionLogEntry.FILE_MOVE));
            Buffer logEntry = new Buffer(logEntryBytes, xaFileSystem);
            xaFileSystem.getTheGatheringDiskWriter().submitBuffer(logEntry, xid);

            addToFileSystemEvents(new FileSystemStateChangeEvent.FileSystemEventType[]{FileSystemStateChangeEvent.FileSystemEventType.DELETED,
                        FileSystemStateChangeEvent.FileSystemEventType.CREATED, FileSystemStateChangeEvent.FileSystemEventType.MODIFIED},
                    new File[]{src, dest, dest}, isDirectoryMove);

            success = true;
            usingReadOnlyOptimization = false;
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            try {
                if (!success) {
                    releaseLocks(newLocks);
                    if (isDirectoryMove) {
                        concurrencyControl.releaseRenamePinOnDirectory(src);
                    }
                }
            } finally {
                asynchronousRollbackLock.unlock();
            }
        }
    }

    public void copyFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException,
            InterruptedException, NoTransactionAssociatedException {
        src = src.getAbsoluteFile();
        dest = dest.getAbsoluteFile();
        Lock newLocks[] = new Lock[2];
        boolean success = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            newLocks[0] = acquireLockIfRequired(src, false);
            newLocks[1] = acquireLockIfRequired(dest, true);
            File destParentFile = dest.getParentFile();
            checkValidParent(src);
            checkValidParent(dest);
            checkPermission(PermissionType.READ_FILE, src);
            checkPermission(PermissionType.WRITE_DIRECTORY, destParentFile);
            view.createFile(dest, false);
            VirtualViewFile srcFileInView = view.getVirtualViewFile(src);
            VirtualViewFile destFileInView = view.getVirtualViewFile(dest);
            srcFileInView.takeSnapshotInto(destFileInView);
            ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, src.getAbsolutePath(),
                    dest.getAbsolutePath(), TransactionLogEntry.FILE_COPY));
            Buffer logEntry = new Buffer(logEntryBytes, xaFileSystem);
            xaFileSystem.getTheGatheringDiskWriter().submitBuffer(logEntry, xid);

            addToFileSystemEvents(new FileSystemStateChangeEvent.FileSystemEventType[]{FileSystemStateChangeEvent.FileSystemEventType.CREATED, FileSystemStateChangeEvent.FileSystemEventType.MODIFIED},
                    new File[]{dest, dest}, false);

            success = true;
            usingReadOnlyOptimization = false;
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            try {
                if (!success) {
                    releaseLocks(newLocks);
                }
            } finally {
                asynchronousRollbackLock.unlock();
            }
        }
    }

    public boolean fileExists(File f) throws LockingFailedException, InsufficientPermissionOnFileException,
            InterruptedException, NoTransactionAssociatedException {
        return fileExists(f, false);
    }

    public boolean fileExists(File f, boolean lockExclusively) throws LockingFailedException,
            InsufficientPermissionOnFileException,
            InterruptedException, NoTransactionAssociatedException {
        f = f.getAbsoluteFile();
        Lock newLock = null;
        boolean success = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            if (!MiscUtils.isRootPath(f)) {
                File parentDir = f.getParentFile();
                newLock = acquireLockIfRequired(f, lockExclusively);
                try {
                    checkPermission(PermissionType.READ_DIRECTORY, parentDir);
                } catch (FileNotExistsException fnee) {
                    return false;
                }
                success = true;
                return view.fileExists(f);
            } else {
                //f is the root.
                return f.exists();//yes, we do a physical file system check here, as no one including this txn deletes a root (/, or C:\ or D:\).
            }
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            try {
                if (!success) {
                    releaseLocks(newLock);
                }
            } finally {
                asynchronousRollbackLock.unlock();
            }
        }
    }

    public boolean fileExistsAndIsDirectory(File f) throws
            LockingFailedException, InsufficientPermissionOnFileException,
            InterruptedException, NoTransactionAssociatedException {
        return fileExistsAndIsDirectory(f, false);
    }

    public boolean fileExistsAndIsDirectory(File f, boolean lockExclusively) throws
            LockingFailedException, InsufficientPermissionOnFileException,
            InterruptedException, NoTransactionAssociatedException {
        f = f.getAbsoluteFile();
        Lock newLock = null;
        boolean success = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            if (!MiscUtils.isRootPath(f)) {
                File parentDir = f.getParentFile();
                newLock = acquireLockIfRequired(f, lockExclusively);
                try {
                    checkPermission(PermissionType.READ_DIRECTORY, parentDir);
                } catch (FileNotExistsException fnee) {
                    return false;
                }
                success = true;
                return view.fileExistsAndIsDirectory(f);
            } else {
                //f is the root.
                return f.exists();//yes, we do a physical file system check here, as no one including this txn deletes a root (/, or C:\ or D:\).
            }
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            try {
                if (!success) {
                    releaseLocks(newLock);
                }
            } finally {
                asynchronousRollbackLock.unlock();
            }
        }
    }

    //now onwards, this locking flag is ignored and no locking is involved in this operation.
    public String[] listFiles(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            InsufficientPermissionOnFileException,
            InterruptedException, NoTransactionAssociatedException {
        return listFiles(f);
    }

    public String[] listFiles(File f) throws FileNotExistsException, LockingFailedException,
            InsufficientPermissionOnFileException,
            InterruptedException, NoTransactionAssociatedException {
        f = f.getAbsoluteFile();
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            if (!MiscUtils.isRootPath(f)) {
                checkPermission(PermissionType.READ_DIRECTORY, f.getParentFile());
                return view.listFiles(f);
            } else {
                return f.list();
            }
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    public long getFileLength(File f) throws FileNotExistsException, LockingFailedException,
            InsufficientPermissionOnFileException,
            InterruptedException, NoTransactionAssociatedException {
        return getFileLength(f, false);
    }

    public long getFileLength(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            InsufficientPermissionOnFileException,
            InterruptedException, NoTransactionAssociatedException {
        f = f.getAbsoluteFile();
        Lock newLock = null;
        boolean success = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            newLock = acquireLockIfRequired(f, lockExclusively);
            checkPermission(PermissionType.READ_FILE, f);
            if (!view.fileExistsAndIsNormal(f)) {
                throw new FileNotExistsException(f.getAbsolutePath());
            }
            long length = view.getVirtualViewFile(f).getLength();
            success = true;
            return length;
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            try {
                if (!success) {
                    releaseLocks(newLock);
                }
            } finally {
                asynchronousRollbackLock.unlock();
            }
        }
    }

    public void truncateFile(File f, long newLength) throws FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException,
            InterruptedException, NoTransactionAssociatedException {
        f = f.getAbsoluteFile();
        Lock newLock = null;
        boolean success = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            newLock = acquireLockIfRequired(f, true);
            checkPermission(PermissionType.WRITE_FILE, f);
            if (view.isNormalFileBeingReadOrWritten(f)) {
                //earlier we threw an exception here.
            }
            if (!view.fileExistsAndIsNormal(f)) {
                throw new FileNotExistsException(f.getAbsolutePath());
            }
            VirtualViewFile vvf = view.getVirtualViewFile(f);
            vvf.truncate(newLength);
            ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, f.getAbsolutePath(), newLength,
                    TransactionLogEntry.FILE_TRUNCATE));
            Buffer logEntry = new Buffer(logEntryBytes, xaFileSystem);
            xaFileSystem.getTheGatheringDiskWriter().submitBuffer(logEntry, xid);

            addToFileSystemEvents(FileSystemStateChangeEvent.FileSystemEventType.MODIFIED, f, false);

            success = true;
            usingReadOnlyOptimization = false;
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            try {
                if (!success) {
                    releaseLocks(newLock);
                }
            } finally {
                asynchronousRollbackLock.unlock();
            }
        }
    }

    private void submitPreCommitInformationForLogging() throws NoTransactionAssociatedException,
            IOException {
        releaseAllStreams();
        Iterator<VirtualViewFile> vvfsUpdatedDirectly = view.getViewFilesWithLatestViewOnDisk().iterator();
        while (vvfsUpdatedDirectly.hasNext()) {
            vvfsUpdatedDirectly.next().forceAndFreePhysicalChannel();
        }
        HashSet<File> filesOnDisk = view.getFilesWithLatestViewOnDisk();
        ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, filesOnDisk));
        xaFileSystem.getTheGatheringDiskWriter().submitBuffer(new Buffer(logEntryBytes, xaFileSystem), xid);

        if (publishFileStateChangeEventsOnCommit) {
            fileStateChangeEventsToRaise = xaFileSystem.getFileSystemEventDelegator().retainOnlyInterestingEvents(fileStateChangeEventsToRaise);
            logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, fileStateChangeEventsToRaise,
                    TransactionLogEntry.EVENT_ENQUEUE));
            xaFileSystem.getTheGatheringDiskWriter().submitBuffer(new Buffer(logEntryBytes, xaFileSystem), xid);
        }
        xaFileSystem.getTheGatheringDiskWriter().writeRemainingBuffersNow(xid);
    }

    public void prepare() throws NoTransactionAssociatedException {
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            submitPreCommitInformationForLogging();
            xaFileSystem.getTheGatheringDiskWriter().transactionPrepareCompletes(xid);
        } catch (NoTransactionAssociatedException note) {
            throw note;
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailure(ioe);
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    public void commit(boolean onePhase) throws NoTransactionAssociatedException {
        ArrayList<FileInputStream> logInputStreams = new ArrayList<FileInputStream>();
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            if (onePhase) {
                try {
                    if (usingReadOnlyOptimization) {
                        completeReadOnlyTransaction();
                        return;
                    }
                    if (!createdForRecovery) {
                        submitPreCommitInformationForLogging();
                        xaFileSystem.getTheGatheringDiskWriter().transactionCommitBegins(xid);
                    }
                } catch (IOException ioe) {
                    xaFileSystem.notifySystemFailure(ioe);
                }
            }
            startedCommitting = true;
            ArrayList<Long> logPositions;
            HashSet<File> filesDirectlyWrittenToDisk;
            HashMap<Integer, FileChannel> logReaderChannels = new HashMap<Integer, FileChannel>(2);
            FileChannel logReaderChannel = null;
            String transactionLogBaseName = xaFileSystem.getTransactionLogFileBaseName();
            int latestCheckPointForRecoveryCase = 0;
            HashSet<File> srcFilesMoved = new HashSet<File>();
            HashSet<File> srcFilesCopied = new HashSet<File>();
            if (createdForRecovery) {
                filesDirectlyWrittenToDisk = xaFileSystem.getRecoveryWorker().getFilesOnDiskForTransaction(xid);
                logPositions = xaFileSystem.getRecoveryWorker().getTransactionLogsPositions(xid);
                latestCheckPointForRecoveryCase = xaFileSystem.getRecoveryWorker().getTransactionsLatestCheckPoint(xid);
                if (latestCheckPointForRecoveryCase == -1) {
                    latestCheckPointForRecoveryCase = 0;
                } else {
                    latestCheckPointForRecoveryCase += 2;
                }
            } else {
                filesDirectlyWrittenToDisk = view.getFilesWithLatestViewOnDisk();
                logPositions = this.transactionLogPositions;
            }
            Buffer inMemoryLog;
            for (int i = latestCheckPointForRecoveryCase; i < logPositions.size() - 1; i += 2) {
                ByteBuffer temp = null;
                int logFileIndex = (int) logPositions.get(i).longValue();
                long localPosition = logPositions.get(i + 1);
                TransactionLogEntry logEntry;
                if (logFileIndex == -1) {
                    inMemoryLog = transactionInMemoryBuffers.get((int) localPosition);
                    temp = inMemoryLog.getBuffer();
                    temp.position(0);
                    logEntry = TransactionLogEntry.parseLogEntry(temp);
                } else {
                    if (logReaderChannels.get(logFileIndex) == null) {
                        FileInputStream fis = new FileInputStream(transactionLogBaseName + "_" + logFileIndex);
                        logReaderChannels.put(logFileIndex, fis.getChannel());
                        logInputStreams.add(fis);
                    }
                    logReaderChannel = logReaderChannels.get(logFileIndex);
                    logReaderChannel.position(localPosition);
                    logEntry = TransactionLogEntry.getNextTransactionLogEntry(logReaderChannel, localPosition, false);
                }
                try {
                    if (logEntry.getOperationType() == TransactionLogEntry.FILE_APPEND) {
                        File f = new File(logEntry.getFileName());
                        if (filesDirectlyWrittenToDisk.contains(f)) {
                            continue;
                        }
                        checkPointDuringModificationAgainstCopy(i - 2, f, srcFilesCopied, srcFilesMoved);
                        commitFileAppend(logEntry, temp, logReaderChannel, logFileIndex, localPosition);
                    } else if (logEntry.getOperationType() == TransactionLogEntry.FILE_DELETE) {
                        String fileName = logEntry.getFileName();
                        File f = new File(fileName);
                        if (filesDirectlyWrittenToDisk.contains(f)) {
                            continue;
                        }
                        checkPointDuringModificationAgainstCopy(i - 2, f, srcFilesCopied, srcFilesMoved);
                        commitDeleteFile(fileName, filesDirectlyWrittenToDisk);
                    } else if (logEntry.getOperationType() == TransactionLogEntry.FILE_CREATE) {
                        String fileName = logEntry.getFileName();
                        File f = new File(fileName);
                        if (filesDirectlyWrittenToDisk.contains(f)) {
                            continue;
                        }
                        checkPointDuringCreationAgainstMove(i - 2, f, srcFilesCopied, srcFilesMoved);
                        commitCreateFile(fileName);
                    } else if (logEntry.getOperationType() == TransactionLogEntry.DIR_CREATE) {
                        String dirName = logEntry.getFileName();
                        checkPointDuringCreationAgainstMove(i - 2, new File(dirName), srcFilesCopied, srcFilesMoved);
                        commitCreateDir(dirName);
                    } else if (logEntry.getOperationType() == TransactionLogEntry.FILE_COPY) {
                        File dest = new File(logEntry.getDestFileName());
                        if (filesDirectlyWrittenToDisk.contains(dest)) {
                            continue;
                        }
                        checkPointDuringCreationAgainstMove(i - 2, dest, srcFilesCopied, srcFilesMoved);
                        commitFileCopy(logEntry, srcFilesCopied);
                    } else if (logEntry.getOperationType() == TransactionLogEntry.FILE_MOVE) {
                        File src = new File(logEntry.getFileName());
                        File dest = new File(logEntry.getDestFileName());
                        if (filesDirectlyWrittenToDisk.contains(dest)) {
                            continue;
                        }
                        boolean isDirectoryMove = src.isDirectory();
                        if (isDirectoryMove) {
                            declareCheckPoint(i - 2, srcFilesCopied, srcFilesMoved);
                            commitMove(logEntry);
                            declareCheckPoint(i - 2, srcFilesCopied, srcFilesMoved);
                        } else {
                            if (!checkPointDuringModificationAgainstCopy(i - 2, src, srcFilesCopied, srcFilesMoved)) {
                                checkPointDuringCreationAgainstMove(i - 2, dest, srcFilesCopied, srcFilesMoved);
                            }
                            commitFileMove(logEntry, srcFilesMoved);
                        }
                    } else if (logEntry.getOperationType() == TransactionLogEntry.FILE_TRUNCATE) {
                        File f = new File(logEntry.getFileName());
                        if (filesDirectlyWrittenToDisk.contains(f)) {
                            continue;
                        }
                        checkPointDuringModificationAgainstCopy(i - 2, f, srcFilesCopied, srcFilesMoved);
                        commitFileTruncate(logEntry);
                    } else if (logEntry.getOperationType() == TransactionLogEntry.FILE_SPECIAL_MOVE) {
                        File src = new File(logEntry.getFileName());
                        File dest = new File(logEntry.getDestFileName());
                        if (!checkPointDuringModificationAgainstCopy(i - 2, src, srcFilesCopied, srcFilesMoved)) {
                            checkPointDuringCreationAgainstMove(i - 2, dest, srcFilesCopied, srcFilesMoved);
                        }
                        commitFileSpecialMove(logEntry, srcFilesMoved);
                    }
                } catch (XASystemIOException xasioe) {
                    throw (IOException) xasioe.getCause();
                } catch (IOException ioe) {
                    //all these ioexceptions will be transaction specific (file_append just
                    //reads from the txn-log) and so would not affect the system.
                    xaFileSystem.notifyTransactionFailure(xid);
                    throw new TransactionFailedException(ioe, xid);
                }
            }
            diskSession.forceToDisk();
            xaFileSystem.getTheGatheringDiskWriter().transactionCompletes(xid, true);
            for (FileInputStream logInputStream : logInputStreams) {
                MiscUtils.closeAll(logInputStream);
                //need to close logs here to allow cleanup of logs in crashRecoveryWorker.
            }
            logInputStreams.clear();//to avoid the loop in finally block.
            cleanup();
            raiseFileStateChangeEvents();
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailure(ioe);
        } finally {
            for (FileInputStream logInputStream : logInputStreams) {
                MiscUtils.closeAll(logInputStream);
            }
            asynchronousRollbackLock.unlock();
        }
    }

    private boolean checkPointDuringModificationAgainstCopy(int currentLogPosition, File fileBeingModified,
            HashSet<File> srcFilesCopied, HashSet<File> srcFilesMoved) throws IOException {
        if (srcFilesCopied.contains(fileBeingModified)) {
            declareCheckPoint(currentLogPosition, srcFilesCopied, srcFilesMoved);
            return true;
        }
        return false;
    }

    private boolean checkPointDuringCreationAgainstMove(int currentLogPosition, File fileBeingCreated,
            HashSet<File> srcFilesCopied, HashSet<File> srcFilesMoved) throws IOException {
        if (srcFilesMoved.contains(fileBeingCreated)) {
            declareCheckPoint(currentLogPosition, srcFilesCopied, srcFilesMoved);
            return true;
        }
        return false;
    }

    private void declareCheckPoint(int currentLogPosition, HashSet<File> srcFilesCopied, HashSet<File> srcFilesMoved) throws IOException {
        diskSession.forceToDisk();
        try {
            ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, currentLogPosition));
            xaFileSystem.getTheGatheringDiskWriter().forceLog(xid, logEntryBytes);
        } catch (IOException ioe) {
            throw new XASystemIOException(ioe);
        }
        srcFilesMoved.clear();
        srcFilesCopied.clear();
    }

    public void completeReadOnlyTransaction() throws NoTransactionAssociatedException {
        //would be called both for commit and rollbacck of a read-only txn.
        if (!usingReadOnlyOptimization) {
            throw new IllegalStateException("Read-only optimization is not being used.");
        }
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            releaseAllStreams();
            cleanup();
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailure(ioe);
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    void completeTheTransaction() {
        try {
            asynchronousRollbackLock.lock();
            cleanup();
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailure(ioe);
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    private void commitFileAppend(TransactionLogEntry logEntry, ByteBuffer inMemoryLogEntry,
            FileChannel logReaderChannel, int logFileIndex, long localPosition)
            throws IOException {
        String fileName = logEntry.getFileName();
        if (!new File(fileName).exists()) {
            return;
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileName, true);
            long contentLength = logEntry.getFileContentLength();
            FileChannel fc = fos.getChannel();
            if (logFileIndex == -1) {
                long num = 0;
                inMemoryLogEntry.position(logEntry.getHeaderLength());
                while (num < contentLength) {
                    num += fc.write(inMemoryLogEntry, logEntry.getFilePosition());
                }

            } else {
                logReaderChannel.position(localPosition + logEntry.getHeaderLength());
                long num = 0;
                if (logEntry.getFilePosition() <= fc.size()) {
                    while (num < contentLength) {
                        num += fc.transferFrom(logReaderChannel, num + logEntry.getFilePosition(),
                                NativeXAFileSystem.maxTransferToChannel(contentLength - num));
                    }
                }
            }
            fc.force(false);
        } finally {
            MiscUtils.closeAll(fos);
        }
    }

    private void commitDeleteFile(String fileName, HashSet<File> filesDirectlyWrittenToDisk)
            throws IOException {
        File f = new File(fileName);
        if (f.exists()) {
            try {
                diskSession.deleteFile(f);
            } catch (IOException ioe) {
                if (f.isDirectory()) {
                    if (f.list().length != 0) {
                        for (File file : filesDirectlyWrittenToDisk) {
                            if (file.getParentFile().equals(f)) {
                                return;
                                //bug#132 (see testcase there). For a file written in heavyWrite mode,
                                //we ignore general io-operations for that file. though such a management
                                //of one file does not affect any other entity - no file or no directory
                                //except the parent directory for the delete operation (as delete-dir
                                //is dependent upon presence of no child objects).
                            }
                        }
                    }
                }
                throw ioe;
            }
        }
    }

    private void commitCreateFile(String fileName) throws IOException {
        File f = new File(fileName);
        if (f.exists()) {
            diskSession.deleteFile(f);
        }
        diskSession.createFile(f);
    }

    private void commitCreateDir(String fileName) throws IOException {
        File f = new File(fileName);
        if (f.exists()) {
            return;
        }
        diskSession.createDirectory(f);
    }

    private void commitFileCopy(TransactionLogEntry logEntry, HashSet<File> srcFilesCopied) throws IOException {
        File src = new File(logEntry.getFileName());
        File dest = new File(logEntry.getDestFileName());
        if (dest.exists()) {
            diskSession.deleteFile(dest);
            diskSession.createFile(dest);
        }
        FileIOUtility.copyFile(src, dest, true);
        srcFilesCopied.add(src);
    }

    private void commitFileMove(TransactionLogEntry logEntry, HashSet<File> srcFilesMoved) throws IOException {
        commitMove(logEntry);
        srcFilesMoved.add(new File(logEntry.getFileName()));
    }

    private void commitMove(TransactionLogEntry logEntry) throws IOException {
        File src = new File(logEntry.getFileName());
        File dest = new File(logEntry.getDestFileName());
        if (!src.exists()) {
            return;
        }
        if (dest.isDirectory()) {
            diskSession.deleteDirectoryRecursively(dest);
        } else if (dest.exists()) {
            diskSession.deleteFile(dest);
        }
        diskSession.renameTo(src, dest);
    }

    private void commitFileTruncate(TransactionLogEntry logEntry) throws IOException {
        String fileName = logEntry.getFileName();
        if (!new File(fileName).exists()) {
            return;
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileName, true);
            FileChannel fc = fos.getChannel();
            fc.truncate(logEntry.getNewLength());
            fc.force(false);
        } finally {
            MiscUtils.closeAll(fos);
        }
    }

    private void commitFileSpecialMove(TransactionLogEntry logEntry, HashSet<File> srcFilesMoved) throws IOException {
        File src = new File(logEntry.getFileName());
        File dest = new File(logEntry.getDestFileName());
        if (!src.exists()) {
            return;
        }
        if (dest.exists()) {
            diskSession.deleteFile(dest);
        }
        diskSession.renameTo(src, dest);
        srcFilesMoved.add(src);
    }

    private void raiseFileStateChangeEvents() {
        if (publishFileStateChangeEventsOnCommit) {
            xaFileSystem.getFileSystemEventQueue().addAll(fileStateChangeEventsToRaise);
        }
    }

    public void rollback() throws NoTransactionAssociatedException {
        ArrayList<FileInputStream> logInputStreams = new ArrayList<FileInputStream>();
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();

            if (usingReadOnlyOptimization) {
                completeReadOnlyTransaction();
                return;
            }

            releaseAllStreams();

            ArrayList<Long> logPositions;
            HashMap<Integer, FileChannel> logReaderChannels = new HashMap<Integer, FileChannel>(2);
            FileChannel logReaderChannel = null;
            String transactionLogBaseName = xaFileSystem.getTransactionLogFileBaseName();
            if (createdForRecovery) {
                logPositions = xaFileSystem.getRecoveryWorker().getTransactionLogsPositions(xid);
            } else {
                HashSet<VirtualViewFile> filesTouchedInPlace = view.getViewFilesWithLatestViewOnDisk();
                for (VirtualViewFile vvf : filesTouchedInPlace) {
                    vvf.freePhysicalChannel();
                }
                logPositions = this.transactionLogPositions;
            }

            xaFileSystem.getTheGatheringDiskWriter().transactionRollbackBegins(xid);

            Buffer inMemoryLog;
            for (int i = logPositions.size() - 2; i >= 0; i -= 2) {
                ByteBuffer temp = null;
                int logFileIndex = logPositions.get(i).intValue();
                long localPosition = logPositions.get(i + 1);
                TransactionLogEntry logEntry;

                if (logFileIndex == -1) {
                    inMemoryLog = transactionInMemoryBuffers.get((int) localPosition);
                    temp =
                            inMemoryLog.getBuffer();
                    temp.position(0);
                    logEntry =
                            TransactionLogEntry.parseLogEntry(temp);
                } else {
                    if (!logReaderChannels.containsKey(logFileIndex)) {
                        FileInputStream fis = new FileInputStream(transactionLogBaseName + "_" + logFileIndex);
                        logReaderChannels.put(logFileIndex, fis.getChannel());
                        logInputStreams.add(fis);
                    }

                    logReaderChannel = logReaderChannels.get(logFileIndex);
                    logReaderChannel.position(localPosition);
                    logEntry =
                            TransactionLogEntry.getNextTransactionLogEntry(logReaderChannel, localPosition, false);
                }

                FileOutputStream fos = null;
                try {
                    if (logEntry.getOperationType() == TransactionLogEntry.UNDOABLE_FILE_TRUNCATE) {
                        String fileName = logEntry.getFileName();
                        fos = new FileOutputStream(fileName, true);
                        long contentLength = logEntry.getFileContentLength();
                        FileChannel fc = fos.getChannel();
                        if (logFileIndex == -1) {
                        } else {
                            logReaderChannel.position(localPosition + logEntry.getHeaderLength());
                            long num = 0;
                            if (logEntry.getFilePosition() <= fc.size()) {
                                while (num < contentLength) {
                                    num += fc.transferFrom(logReaderChannel, num + logEntry.getFilePosition(),
                                            NativeXAFileSystem.maxTransferToChannel(contentLength - num));
                                }
                            }
                        }
                        fc.force(false);//improve this. force for every piece of content? (same in commit method).
                    } else if (logEntry.getOperationType() == TransactionLogEntry.UNDOABLE_FILE_APPEND) {
                        String fileName = logEntry.getFileName();
                        fos = new FileOutputStream(fileName, true);
                        FileChannel fc = fos.getChannel();
                        fc.truncate(logEntry.getNewLength());
                        fc.force(false);//the file length may be part of meta-data (not sure). Make "true"?
                    }
                } catch (IOException ioe) {
                    //all these ioexceptions will be transaction specific (file_append just
                    //reads from the txn-log) and so would not affect the system.
                    xaFileSystem.notifyTransactionFailure(xid);
                    throw new TransactionFailedException(ioe, xid);
                } finally {
                    MiscUtils.closeAll(fos);
                }
            }
            xaFileSystem.getTheGatheringDiskWriter().transactionCompletes(xid, false);
            for (FileInputStream logInputStream : logInputStreams) {
                MiscUtils.closeAll(logInputStream);
                //need to close logs here to allow cleanup of logs in crashRecoveryWorker.
            }
            logInputStreams.clear();//to avoid the loop in finally block.
            cleanup();
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailure(ioe);
        } finally {
            for (FileInputStream logInputStream : logInputStreams) {
                MiscUtils.closeAll(logInputStream);
            }
            asynchronousRollbackLock.unlock();
        }

    }

    private void cleanup() throws IOException {
        this.sessionIsUseless = true;
        this.operationsCanContinue = false;
        if (createdForRecovery) {
            xaFileSystem.getRecoveryWorker().cleanupTransactionInfo(xid);
        } else {
            xaFileSystem.getTheGatheringDiskWriter().cleanupTransactionInfo(xid);
        }

        releaseAllLocks();
        xaFileSystem.removeTransactionSessionEntry(xid);

        if (!createdForRecovery) {
            Iterator<VirtualViewFile> vvfsInBackupDir = view.getViewFilesUsingBackupDir().iterator();
            while (vvfsInBackupDir.hasNext()) {
                vvfsInBackupDir.next().cleanupBackup();
            }
            concurrencyControl.releaseRenamePinOnDirectories(directoriesPinnedInThisSession);
        }

        if (concurrencyControl instanceof RemoteConcurrencyControl) {
            concurrencyControl.shutdown();
        }

        for (Buffer buffer : transactionInMemoryBuffers) {
            if (buffer instanceof PooledBuffer) {
                xaFileSystem.getBufferPool().checkIn((PooledBuffer) buffer);
            }
        }
    }

    private void releaseAllLocks() {
        for (Lock lock : allAcquiredLocks.values()) {
            concurrencyControl.releaseLock(xid, lock);
        }
        allAcquiredLocks.clear();
    }

    private void releaseAllStreams() throws NoTransactionAssociatedException {
        for (XAFileInputStream xafis : allAcquiredInputStreams) {
            xafis.close();
        }

        for (NativeXAFileOutputStream xafos : allAcquiredOutputStreams) {
            xafos.close();
            deCacheXAFileOutputStream(xafos.getDestinationFile());
        }

    }

    public int getTransactionTimeout() {
        return transactionTimeout;
    }

    public boolean setTransactionTimeout(int transactionTimeout) {
        this.transactionTimeout = transactionTimeout;
        return true;
    }

    private void checkPermission(PermissionType operation, File f) throws FileNotExistsException,
            InsufficientPermissionOnFileException {
        switch (operation) {
            case READ_FILE:
                if (view.isNormalFileReadable(f)) {
                    return;
                }
                break;
            case WRITE_FILE:
                if (view.isNormalFileWritable(f)) {
                    return;
                }
                break;
            case READ_DIRECTORY:
                if (view.isDirectoryReadable(f)) {
                    return;
                }
                break;
            case WRITE_DIRECTORY:
                if (view.isDirectoryWritable(f)) {
                    return;
                }
                break;
        }
        throw new InsufficientPermissionOnFileException(operation, f.getAbsolutePath());
    }

    private Lock acquireLockIfRequired(File f, boolean exclusive) throws LockingFailedException,
            InterruptedException, TransactionRolledbackException {
        Lock newLock = null;
        if (!alreadyHaveALock(f, exclusive)) {
            try {
                newLock = concurrencyControl.acquireFileLock(xid, f, fileLockWaitTimeout, exclusive);
                if (exclusive) {
                    xid.incrementNumOwnedExclusiveLocks();
                }
            } catch (DeadLockVictimizedException dlve) {
                rollbackPrematurely(dlve);
                throw new TransactionRolledbackException(dlve);
            } catch (TransactionTimeoutException tte) {
                rollbackPrematurely(tte);
                throw new TransactionRolledbackException(tte);
            }
            allAcquiredLocks.put(f, newLock);
            //above includes the case of lock upgrade by doing a "redundant put" of the same "value".
        }
        return newLock;
    }

    private boolean alreadyHaveALock(File f, boolean exclusive) {
        Lock existingLock = allAcquiredLocks.get(f);
        if (existingLock == null) {
            return false;
        }
        if (existingLock.isExclusive() || !exclusive) {
            return true;
        }
        return false;
    }

    private void checkValidParent(File f) throws FileNotExistsException {
        if (f.getParentFile() == null) {
            throw new FileNotExistsException("{Parent directory of (" + f.getAbsolutePath() + ")}");
        }
    }

    private void releaseLocks(Lock locks[]) {
        for (Lock lock : locks) {
            if (lock != null) {
                allAcquiredLocks.remove(lock.getResource());
                concurrencyControl.releaseLock(xid, lock);
            }
        }
    }

    private void releaseLocks(Lock lock) {
        if (lock != null) {
            allAcquiredLocks.remove(lock.getResource());
            concurrencyControl.releaseLock(xid, lock);
        }
    }

    public long getFileLockWaitTimeout() {
        return fileLockWaitTimeout;
    }

    public void setFileLockWaitTimeout(long fileLockWaitTimeout) {
        this.fileLockWaitTimeout = fileLockWaitTimeout;
    }

    public void checkIfCanContinue() throws NoTransactionAssociatedException {
        if (operationsCanContinue) {
            return;
        } else {
            if (rolledbackPrematurely) {
                throw new TransactionRolledbackException(rollbackCause);
            }
            if (sessionIsUseless) {
                throw new NoTransactionAssociatedException();
            }
            if (systemHasFailed) {
                throw new XASystemNoMoreAvailableException(systemFailureCause);
            }
            if (systemGotShutdown) {
                throw new XASystemNoMoreAvailableException();
            }
        }
    }

    public void declareTransactionUsingUndoLogs() throws IOException {
        ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid,
                TransactionLogEntry.TXN_USES_UNDO_LOGS));
        xaFileSystem.getTheGatheringDiskWriter().forceLog(xid, logEntryBytes);
    }

    public long getTimeOfEntryToTransaction() {
        return timeOfEntryToTransaction;
    }

    public ReentrantLock getAsynchronousRollbackLock() {
        return asynchronousRollbackLock;
    }

    public void addLogPositionToTransaction(int logFileIndex, long localPosition) {
        transactionLogPositions.add((long) logFileIndex);
        transactionLogPositions.add(localPosition);
    }

    public void addInMemoryBufferToTransaction(Buffer buffer) {
        transactionInMemoryBuffers.add(buffer);
        int indexIntoBufferArray = transactionInMemoryBuffers.size() - 1;
        addLogPositionToTransaction(-1, indexIntoBufferArray);
    }

    boolean hasStartedCommitting() {
        return startedCommitting;
    }

    public TransactionInformation getXid() {
        return xid;
    }

    private void checkAnyOpenStreamToDescendantFiles(File ancestor) throws FileUnderUseException {
        for (NativeXAFileInputStream is : allAcquiredInputStreams) {
            if (isAncestorOf(ancestor, is.getSourceFileName()) && !is.isClosed()) {
                throw new FileUnderUseException(is.getSourceFileName().getAbsolutePath(), false);
            }
        }
        for (NativeXAFileOutputStream os : allAcquiredOutputStreams) {
            if (isAncestorOf(ancestor, os.getDestinationFile()) && !os.isClosed()) {
                throw new FileUnderUseException(os.getDestinationFile().getAbsolutePath(), false);
            }
        }
    }

    private boolean isAncestorOf(File a, File b) {
        File parentB = b.getParentFile();
        while (parentB != null) {
            if (a.equals(parentB)) {
                return true;
            }
            parentB = parentB.getParentFile();
        }
        return false;
    }

    public boolean getPublishFileStateChangeEventsOnCommit() {
        return publishFileStateChangeEventsOnCommit;
    }

    public void setPublishFileStateChangeEventsOnCommit(boolean publishFileStateChangeEventsOnCommit) {
        this.publishFileStateChangeEventsOnCommit = publishFileStateChangeEventsOnCommit;
    }

    private void addToFileSystemEvents(FileSystemStateChangeEvent.FileSystemEventType actionType, File affectedObject, boolean isDirectory) {
        File parentDirectory = null;

        if (actionType.equals(FileSystemStateChangeEvent.FileSystemEventType.CREATED)) {
            fileStateChangeEventsToRaise.add(new FileSystemStateChangeEvent(affectedObject, isDirectory, FileSystemStateChangeEvent.FileSystemEventType.CREATED, xid));
            parentDirectory = affectedObject.getParentFile();
            if (parentDirectory != null) {
                fileStateChangeEventsToRaise.add(new DirectoryModificationEvent(affectedObject, parentDirectory, true, FileSystemStateChangeEvent.FileSystemEventType.MODIFIED, xid));
            }
            return;
        }
        if (actionType.equals(FileSystemStateChangeEvent.FileSystemEventType.DELETED)) {
            fileStateChangeEventsToRaise.add(new FileSystemStateChangeEvent(affectedObject, isDirectory, FileSystemStateChangeEvent.FileSystemEventType.DELETED, xid));
            parentDirectory = affectedObject.getParentFile();
            if (parentDirectory != null) {
                fileStateChangeEventsToRaise.add(new DirectoryModificationEvent(affectedObject, parentDirectory, true, FileSystemStateChangeEvent.FileSystemEventType.MODIFIED, xid));
            }
            return;
        }
        if (actionType.equals(FileSystemStateChangeEvent.FileSystemEventType.MODIFIED)) {
            fileStateChangeEventsToRaise.add(new FileSystemStateChangeEvent(affectedObject, isDirectory, FileSystemStateChangeEvent.FileSystemEventType.MODIFIED, xid));
            return;
        }
    }

    private void addToFileSystemEvents(FileSystemStateChangeEvent.FileSystemEventType[] actionType, File[] affectedObject, boolean areDirectories) {
        for (int i = 0; i < actionType.length; i++) {
            addToFileSystemEvents(actionType[i], affectedObject[i], areDirectories);
        }
    }

    public void commit() throws NoTransactionAssociatedException {
        this.commit(true);
    }

    public NativeXAFileOutputStream getCachedXAFileOutputStream(VirtualViewFile vvf, TransactionInformation xid, boolean heavyWrite,
            NativeSession owningSession)
            throws FileUnderUseException {
        synchronized (fileAndOutputStream) {
            File f = vvf.getFileName();
            NativeXAFileOutputStream xaFOS = fileAndOutputStream.get(f);
            if (xaFOS == null || xaFOS.isClosed()) {
                xaFOS = new NativeXAFileOutputStream(vvf, xid, heavyWrite, owningSession, xaFileSystem);
                fileAndOutputStream.put(f, xaFOS);
            } else {
                if (!vvf.isUsingHeavyWriteOptimization() && heavyWrite) {
                    throw new FileUnderUseException(f.getAbsolutePath(), true);
                }
            }
            return xaFOS;
        }
    }

    public void deCacheXAFileOutputStream(File f) {
        synchronized (fileAndOutputStream) {
            fileAndOutputStream.remove(f);
        }
    }

    public boolean isUsingReadOnlyOptimization() {
        return usingReadOnlyOptimization;
    }
}
