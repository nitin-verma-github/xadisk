/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.exceptions;

import org.xadisk.bridge.proxies.interfaces.XADiskBasicIOOperations;

/**
 * This exception is thrown by the
 * {@link XADiskBasicIOOperations#moveFile(java.io.File, java.io.File) move}
 * operation on a directory when an attempt to acquire a "pin" (<i> a pin is a special kind of
 * lock over a directory which is acquired by a transaction willing to
 * {@link XADiskBasicIOOperations#moveFile(java.io.File, java.io.File) move}
 * the directory</i>) over the target directory could not succeed because one of the 
 * descendants (children files/directories, children's children file/directories, and further)
 * of the directory has been locked by some other transaction.
 * 
 * <p> As of XADisk 1.0, no wait is done if any such descendant file/directory is locked, and this
 * exception is thrown immediately. (<i>Such waiting would be implemented in upcoming releases.</i>)
 *
 * @since 1.0
 */
public class DirectoryPinningFailedException extends LockingFailedException {

    private static final long serialVersionUID = 1L;
    private final String descendantPath;

    public DirectoryPinningFailedException(String path, String descendantPath) {
        super(path);
        this.descendantPath = descendantPath;
    }

    @Override
    public String getMessage() {
        return super.getGenericMessage() + " The reason is : "
                + "A descendant file/directory [" + descendantPath + "] has been locked by some other transaction.";
    }

    /**
     * Returns the descendant file/directory's path, lock on which is blocking the current operation to proceed.
     * @return the path of the descendant file/directory.
     */
    public String getDescendantPath() {
        return descendantPath;
    }
}
