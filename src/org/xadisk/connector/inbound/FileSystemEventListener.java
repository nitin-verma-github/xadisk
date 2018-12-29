/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.connector.inbound;

import javax.ejb.MessageDrivenBean;
import org.xadisk.filesystem.FileSystemStateChangeEvent;

/**
 * An interface which must be implemented by the {@link MessageDrivenBean} willing to
 * receive file-system events ({@link FileSystemStateChangeEvent}) from XADisk.
 *
 * @since 1.0
 */
public interface FileSystemEventListener {

    /**
     * This method is called by the JavaEE container when an event, targeted for this MDB,
     * is published by the XADisk instance.
     * @param event the event object providing details of the event which has taken place.
     */
    public void onFileSystemEvent(FileSystemStateChangeEvent event);
}
