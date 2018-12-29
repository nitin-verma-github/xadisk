/*
 Copyright � 2010-2012, Nitin Verma (project owner for XADisk http://xadisk.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem;

import java.io.File;
import java.io.Serializable;
import javax.ejb.MessageDrivenBean;
import org.xadisk.connector.inbound.FileSystemEventListener;

/**
 * An object of this class represents a directory modification event (a child object
 * added/deleted to/from the directory) published to an <i>interested</i> (as indicated
 * through its activation-spec) {@link MessageDrivenBean} by XADisk. Such a
 * {@link MessageDrivenBean} is handed-over this event object through the 
 * {@link FileSystemEventListener#onFileSystemEvent(FileSystemStateChangeEvent) onFileSystemEvent} method.
 *
 * @since 1.2.1
 */
public class DirectoryModificationEvent extends FileSystemStateChangeEvent implements Serializable {

    private static final long serialVersionUID = 1L;
    private final File childFilePath;

    DirectoryModificationEvent(File childFilePath, File file, boolean isDirectory, FileSystemEventType eventType,
            TransactionInformation enqueuingTransaction) {
        super(file, isDirectory, eventType, enqueuingTransaction);
        this.childFilePath = childFilePath;
    }

    /**
     * Returns the child object (file/directory) the addition/deletion of which
     * triggered this event.
     * @return a {@link File} object representing the child object (file/directory).
     */
    public File getChildFilePath() {
        return childFilePath;
    }

    @Override
    public int hashCode() {
        return childFilePath.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DirectoryModificationEvent) {
            DirectoryModificationEvent that = (DirectoryModificationEvent) obj;
            return this.childFilePath.equals(that.childFilePath) && super.equals(obj);
        }
        return false;
    }
}
