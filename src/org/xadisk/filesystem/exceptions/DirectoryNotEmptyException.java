/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.exceptions;

import org.xadisk.bridge.proxies.interfaces.XADiskBasicIOOperations;

/**
 * This exception is thrown when a directory {@link XADiskBasicIOOperations#deleteFile(java.io.File) delete}
 * operation was invoked but the directory is not empty.
 *
 * @since 1.0
 */
public class DirectoryNotEmptyException extends XAApplicationException {

    private static final long serialVersionUID = 1L;
    private String path;

    public DirectoryNotEmptyException(String path) {
        this.path = path;
    }

    @Override
    public String getMessage() {
        return "The directory [" + path + "] could not be deleted as it is not empty.";
    }

    /**
     * Returns the path of the directory which could not be deleted.
     * @return the path of the directory.
     */
    public String getPath() {
        return path;
    }
}
