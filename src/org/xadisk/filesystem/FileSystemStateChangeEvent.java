/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem;

import java.io.File;
import java.io.Serializable;
import javax.ejb.MessageDrivenBean;
import org.xadisk.connector.inbound.FileSystemEventListener;

/**
 * An object of this class represents a file system event published to an
 * <i>interested</i> (as indicated through its activation-spec) {@link MessageDrivenBean}
 * by XADisk. Such a {@link MessageDrivenBean} is handed-over this event object
 * through the {@link FileSystemEventListener#onFileSystemEvent(FileSystemStateChangeEvent)
 * onFileSystemEvent} method.
 * 
 * Note that in case of directory modification, an event object of subclass
 * {@link DirectoryModificationEvent} is generated.
 *
 * @since 1.0
 */
public class FileSystemStateChangeEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * This enum represents the event type of a {@link FileSystemStateChangeEvent} object. The same
     * type applies to both files and directories.
     *
     * @since 1.0
     */
    public enum FileSystemEventType {

        /**
         * Implies a change in a file's content, or a directory's children list (e.g.
         * addition of a children file inside the directory).
         * <p> Its internal byte value is 1.
         */
        MODIFIED((byte) 0x1),
        /**
         * Implies deletion of a file or a directory.
         * <p> Its internal byte value is 2.
         */
        DELETED((byte) 0x2),
        /**
         * Implies creation of a file or a directory.
         * <p> Its internal byte value is 4.
         */
        CREATED((byte) 0x4);
        private final byte byteValue;

        private FileSystemEventType(byte byteValue) {
            this.byteValue = byteValue;
        }

        /**
         * Returns the internal byte value representing this event type.
         * @return the byte value (one among 1, 2 or 4).
         */
        public byte getByteValue() {
            return byteValue;
        }

        /**
         * Given a byte value among 1, 2 or 4, converts it to the corresponding
         * {@link FileSystemEventType FileSystemEventType} to which the
         * byte value maps internally.
         * @param byteValue the input byte value. One among 1, 2 or 4.
         * @return the corresponding {@link FileSystemEventType FileSystemEventType}.
         */
        public static FileSystemEventType getFileSystemEventType(byte byteValue) {
            return values()[byteValue / 2];
        }
    };
    private final File file;
    private final boolean isDirectory;
    private final FileSystemEventType eventType;
    private transient final TransactionInformation enqueuingTransaction;

    FileSystemStateChangeEvent(File file, boolean isDirectory, FileSystemEventType eventType, TransactionInformation enqueuingTransaction) {
        this.file = file;
        this.isDirectory = isDirectory;
        this.eventType = eventType;
        this.enqueuingTransaction = enqueuingTransaction;
    }

    /**
     * Returns the file/directory on which this event has taken place.
     * @return a {@link File} object representing the file/directory.
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns the type of the event.
     * @return type of the event.
     */
    public FileSystemEventType getEventType() {
        return eventType;
    }

    /**
     * Returns true if this event has taken place on a directory; false otherwise.
     * @return true for directory; false for normal file.
     */
    public boolean isDirectory() {
        return isDirectory;
    }

    TransactionInformation getEnqueuingTransaction() {
        return enqueuingTransaction;
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FileSystemStateChangeEvent) {
            FileSystemStateChangeEvent that = (FileSystemStateChangeEvent) obj;
            return that.file.equals(this.file) && that.eventType.equals(this.eventType)
                    && that.isDirectory == this.isDirectory
                    && that.enqueuingTransaction.equals(this.enqueuingTransaction);
        }
        return false;
    }

    /**
     * Returns a human readable string describing this event.
     * @return description of the event.
     */
    @Override
    public String toString() {
        return "Event Type : " + this.eventType.name()
                + " || File Name : " + this.file
                + " || Is a Directory : " + this.isDirectory
                + System.getProperty("line.separator");
    }
}
