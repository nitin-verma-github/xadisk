/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.interfaces;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import javax.ejb.MessageDrivenBean;
import org.xadisk.additional.XAFileInputStreamWrapper;
import org.xadisk.additional.XAFileOutputStreamWrapper;
import org.xadisk.connector.outbound.XADiskConnection;
import org.xadisk.filesystem.FileSystemConfiguration;
import org.xadisk.filesystem.exceptions.DirectoryNotEmptyException;
import org.xadisk.filesystem.exceptions.FileAlreadyExistsException;
import org.xadisk.filesystem.exceptions.FileNotExistsException;
import org.xadisk.filesystem.exceptions.FileUnderUseException;
import org.xadisk.filesystem.exceptions.InsufficientPermissionOnFileException;
import org.xadisk.filesystem.exceptions.LockingFailedException;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;

/**
 * This interface declares a set of i/o operations which can be called on XADisk.
 * <p> An object of this interface is obtained as a session object ({@link Session}),
 * XA-enabled session object ({@link XASession}) or as a connection object ({@link XADiskConnection}).
 *
 * @since 1.0
 */
public interface XADiskBasicIOOperations {

    /**
     * This enum enumerates the set of permission types required to perform
     * read or write operations on files and directories.
     *
     * @since 1.0
     */
    public enum PermissionType {

        /**
         * Represents read permission over a file.
         */
        READ_FILE,
        /**
         * Represents write permission over a file.
         */
        WRITE_FILE,
        /**
         * Represents read permission over a directory.
         */
        READ_DIRECTORY,
        /**
         * Represents write permission over a directory.
         */
        WRITE_DIRECTORY
    };

    /**
     * Sets the values of publishFileStateChangeEventsOnCommit.
     * <p> This property can be used to control whether the commit of the transaction on this object
     * should result in publishing of events to registered {@link MessageDrivenBean MessageDrivenBeans} or not.
     * @param publish new value of publishFileStateChangeEventsOnCommit.
     */
    public void setPublishFileStateChangeEventsOnCommit(boolean publish);

    /**
     * Returns the value of publishFileStateChangeEventsOnCommit.
     * <p> This property can be used to control whether the commit of the transaction on this object
     * should result in publishing of events to registered {@link MessageDrivenBean MessageDrivenBeans} or not.
     * @return value of publishFileStateChangeEventsOnCommit.
     */
    public boolean getPublishFileStateChangeEventsOnCommit();

    /**
     * Returns the current value of the lock wait timeout for this object.
     * <p> This is the time an i/o operation will wait for acquiring a lock over a file/directory.
     * Waiting for a lock allows some space for other transactions holding the lock to complete
     * and release the locks.
     * <p> Note that the value of lock wait timeout period defaults to the
     * {@link FileSystemConfiguration#getLockTimeOut() global-configuration}.
     * <p> If there is no transaction currently associated with this object, a value of -1 is returned.
     * @return the lock wait timeout, in milliseconds.
     */
    public long getFileLockWaitTimeout();

    /**
     * Sets a new value for the lock wait timeout for this object.
     * <p> This is the time an i/o operation will wait for acquiring a lock over a file/directory.
     * Waiting for a lock allows some space for other transactions holding the lock to complete
     * and release the locks.
     * <p> Note that the value of lock wait timeout period defaults to the
     * {@link FileSystemConfiguration#getLockTimeOut() global-configuration}.
     * <p> If there is no transaction currently associated with this object, this method is ineffective.
     * @param fileLockWaitTimeout the new lock wait timeout, in milliseconds.
     */
    public void setFileLockWaitTimeout(long fileLockWaitTimeout);

    /**
     * Creates an input stream to the file.
     * This stream can be further be wrapped by a utility class {@link XAFileInputStreamWrapper} to
     * get easy pluggability via the standard {@link InputStream}.
     * @param f the target file from where to read.
     * @param lockExclusively set to true for obtaining an exclusive lock over the file or
     * directory. False will only obtain a shared lock.
     * @return the input stream object.
     * @throws FileNotExistsException
     * @throws InsufficientPermissionOnFileException
     * @throws LockingFailedException
     * @throws NoTransactionAssociatedException
     * @throws InterruptedException
     */
    public XAFileInputStream createXAFileInputStream(File f, boolean lockExclusively) throws
            FileNotExistsException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException;

    /**
     * Creates an input stream to the file.
     * This stream can be further be wrapped by a utility class {@link XAFileInputStreamWrapper} to
     * get easy pluggability via the standard {@link InputStream}.
     * This method is equivalent to:
     * <i> {@link #createXAFileInputStream(File, boolean) createXAFileInputStream(f, false)} </i>
     * @param f the target file from where to read.
     * @return the input stream object.
     * @throws FileNotExistsException
     * @throws InsufficientPermissionOnFileException
     * @throws LockingFailedException
     * @throws NoTransactionAssociatedException
     * @throws InterruptedException
     * @since 1.1
     */
    public XAFileInputStream createXAFileInputStream(File f) throws
            FileNotExistsException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException;

    /**
     * Creates an output stream to the file. The file should exist
     * from the viewpoint of the current transaction (i.e. if the file didn't exist before
     * the transaction, it should be created first).
     * <p> This stream would always append to the file.
     * <p> To write at an arbitrary offset of the file, this method
     * can be used along with {@link #truncateFile} and the {@link XAFileInputStream} for example.
     * <p> This stream can further be wrapped by a utility class {@link XAFileOutputStreamWrapper} to
     * get easy pluggability via the standard {@link OutputStream}.
     * @param f the target file to which to write.
     * @param heavyWrite a clue for performance tuning. When writing just a few hundred bytes, set this to false.
     * @return the output stream object.
     * @throws FileNotExistsException
     * @throws FileUnderUseException
     * @throws InsufficientPermissionOnFileException
     * @throws LockingFailedException
     * @throws NoTransactionAssociatedException
     * @throws InterruptedException
     */
    public XAFileOutputStream createXAFileOutputStream(File f, boolean heavyWrite) throws
            FileNotExistsException, FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException;

    /**
     * Create a new file or directory.
     * @param f the file or directory to create.
     * @param isDirectory tells whether to create a directory.
     * @throws FileAlreadyExistsException
     * @throws FileNotExistsException
     * @throws InsufficientPermissionOnFileException
     * @throws LockingFailedException
     * @throws NoTransactionAssociatedException
     * @throws InterruptedException
     */
    public void createFile(File f, boolean isDirectory) throws
            FileAlreadyExistsException, FileNotExistsException, InsufficientPermissionOnFileException,
            LockingFailedException, NoTransactionAssociatedException, InterruptedException;

    /**
     * Deletes a file or directory. A directory should be empty for this deletion to succeed; else
     * {@link DirectoryNotEmptyException} is thrown.
     * @param f the file/directory to delete.
     * @throws DirectoryNotEmptyException
     * @throws FileNotExistsException
     * @throws FileUnderUseException
     * @throws InsufficientPermissionOnFileException
     * @throws LockingFailedException
     * @throws NoTransactionAssociatedException
     * @throws InterruptedException
     */
    public void deleteFile(File f) throws DirectoryNotEmptyException, FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException;

    /**
     * Copies a file <i>src</i> to another non-existing file <i>dest</i>.
     * @param src source file.
     * @param dest destination file.
     * @throws FileAlreadyExistsException
     * @throws FileNotExistsException
     * @throws InsufficientPermissionOnFileException
     * @throws LockingFailedException
     * @throws NoTransactionAssociatedException
     * @throws InterruptedException
     */
    public void copyFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException;

    /**
     * Renames a file/directory with path <i>src</i> to a non-existing path <i>dest</i>.
     * @param src the source path for the file or directory.
     * @param dest the destination path for the file or directory.
     * @throws FileAlreadyExistsException
     * @throws FileNotExistsException
     * @throws FileUnderUseException
     * @throws InsufficientPermissionOnFileException
     * @throws LockingFailedException
     * @throws NoTransactionAssociatedException
     * @throws InterruptedException
     */
    public void moveFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException;

    /**
     * Tells whether the file or directory exists.
     * @param f the file/directory path.
     * @param lockExclusively set to true for obtaining an exclusive lock over the file or
     * directory. False will only obtain a shared lock.
     * @return true if the file/directory exists.
     * @throws LockingFailedException
     * @throws NoTransactionAssociatedException
     * @throws InterruptedException
     * @throws InsufficientPermissionOnFileException
     */
    public boolean fileExists(File f, boolean lockExclusively) throws LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException;

    /**
     * Tells whether the file or directory exists.
     * This method is equivalent to:
     * <i> {@link #fileExists(File, boolean) fileExists(f, false)} </i>
     * @param f the file/directory path.
     * @return true if the file/directory exists.
     * @throws LockingFailedException
     * @throws NoTransactionAssociatedException
     * @throws InterruptedException
     * @throws InsufficientPermissionOnFileException
     * @since 1.1
     */
    public boolean fileExists(File f) throws LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException;

    /**
     * Tells whether the directory exists.
     * @param f the directory path.
     * @param lockExclusively set to true for obtaining an exclusive lock over the
     * directory. False will only obtain a shared lock.
     * @return true if the directory exists; false otherwise.
     * @throws LockingFailedException
     * @throws NoTransactionAssociatedException
     * @throws InterruptedException
     * @throws InsufficientPermissionOnFileException
     */
    public boolean fileExistsAndIsDirectory(File f, boolean lockExclusively) throws LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException;

    /**
     * Tells whether the directory exists.
     * This method is equivalent to:
     * <i> {@link #fileExistsAndIsDirectory(File, boolean) fileExistsAndIsDirectory(f, false)} </i>
     * @param f the directory path.
     * @return true if the directory exists; false otherwise.
     * @throws LockingFailedException
     * @throws NoTransactionAssociatedException
     * @throws InterruptedException
     * @throws InsufficientPermissionOnFileException
     * @since 1.1
     */
    public boolean fileExistsAndIsDirectory(File f) throws LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException;

    /**
     * Lists the contents of the directory.
     * @param f the directory path.
     * @param lockExclusively this parameter is ignored and is being retained only to protect existing
     * applications' code. Version 1.1 onwards, this method is equivalent to
     * <i> {@link #listFiles(File) listFiles(f)} </i>.
     * @return an array of Strings containing names of files/directories.
     * @throws FileNotExistsException
     * @throws LockingFailedException
     * @throws NoTransactionAssociatedException
     * @throws InterruptedException
     * @throws InsufficientPermissionOnFileException
     */
    public String[] listFiles(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException,
            InsufficientPermissionOnFileException;

    /**
     * Lists the contents of the directory.
     * This method is equivalent to:
     * <i> {@link #listFiles(File, boolean) listFiles(f, false)} </i>
     * @param f the directory path.
     * @return an array of Strings containing names of files/directories.
     * @throws FileNotExistsException
     * @throws LockingFailedException
     * @throws NoTransactionAssociatedException
     * @throws InterruptedException
     * @throws InsufficientPermissionOnFileException
     * @since 1.1
     */
    public String[] listFiles(File f) throws FileNotExistsException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException,
            InsufficientPermissionOnFileException;

    /**
     * Gets the length of the file.
     * @param f the file path.
     * @param lockExclusively set to true for obtaining an exclusive lock over the
     * file. False will only obtain a shared lock.
     * @return length of the file in bytes.
     * @throws FileNotExistsException
     * @throws LockingFailedException
     * @throws NoTransactionAssociatedException
     * @throws InterruptedException
     * @throws InsufficientPermissionOnFileException
     */
    public long getFileLength(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException;

    /**
     * Gets the length of the file.
     * This method is equivalent to:
     * <i> {@link #getFileLength(File, boolean) getFileLength(f, false)} </i>
     * @param f the file path.
     * @return length of the file in bytes.
     * @throws FileNotExistsException
     * @throws LockingFailedException
     * @throws NoTransactionAssociatedException
     * @throws InterruptedException
     * @throws InsufficientPermissionOnFileException
     * @since 1.1
     */
    public long getFileLength(File f) throws FileNotExistsException, LockingFailedException,
            NoTransactionAssociatedException, InsufficientPermissionOnFileException,
            InterruptedException;

    /**
     * Truncates a file.
     * @param f the file path.
     * @param newLength new length to truncate to. It should be a non-negative number less
     * than or equal to the file size.
     * @throws FileNotExistsException
     * @throws InsufficientPermissionOnFileException
     * @throws LockingFailedException
     * @throws NoTransactionAssociatedException
     * @throws InterruptedException
     */
    public void truncateFile(File f, long newLength) throws FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException,
            NoTransactionAssociatedException, InterruptedException;
}
