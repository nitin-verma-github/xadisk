/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.exceptions;

import org.xadisk.bridge.proxies.interfaces.XADiskBasicIOOperations;

/**
 * This exception is a super class of all other exceptions which are thrown by
 * various i/o methods in {@link XADiskBasicIOOperations} when
 * a resource (file or directory) locking fails. Note that this class is an abstract class.
 *
 * @since 1.0
 */
public abstract class LockingFailedException extends XAApplicationException {

    private String path;

    public LockingFailedException(String path) {
        this.path = path;
    }

    /**
     * Returns the path of the file/directory on which the lock required for the operation could not be acquired.
     * @return the path of the file/directory.
     */
    public String getPath() {
        return path;
    }

    public String getGenericMessage() {
        return "A lock required over the file/directory [" + path + "], which was required"
                + " for the operation, could not be acquired.";
    }
}
