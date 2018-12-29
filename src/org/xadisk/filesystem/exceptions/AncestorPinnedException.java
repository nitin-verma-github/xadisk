/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.exceptions;

import org.xadisk.bridge.proxies.interfaces.XADiskBasicIOOperations;

/**
 * This exception is thrown by the various I/O methods in {@link XADiskBasicIOOperations}
 * when an attempt to acquire required locks (over file/directory objects) could not succeed
 * because one of the ancestors (parent directory, parent's parent directory, and further)
 * of the corresponding files/directories has been "pinned" by some other transaction. A
 * Pin is a special kind of lock over a directory which is acquired by a transaction
 * willing to {@link XADiskBasicIOOperations#moveFile(java.io.File, java.io.File) move}
 * the directory.
 * <p> As of XADisk 1.0, no wait is done for release of the pin on the ancestor directory, and this
 * exception is thrown immediately. (<i>Such waiting would be implemented in upcoming releases.</i>)
 *
 * @since 1.0
 */
public class AncestorPinnedException extends LockingFailedException {

    private static final long serialVersionUID = 1L;
    private final String ancestorPath;

    public AncestorPinnedException(String path, String ancestorPath) {
        super(path);
        this.ancestorPath = ancestorPath;
    }

    @Override
    public String getMessage() {
        return super.getGenericMessage() + " The reason is : "
                + "An ancestor directory [" + ancestorPath + "] has been pinned by some other transaction.";
    }

    /**
     * Returns the ancestor directory's path, whose pinning is blocking the current operation to proceed.
     * @return the path of the ancestor directory.
     */
    public String getAncestorPath() {
        return ancestorPath;
    }
}
