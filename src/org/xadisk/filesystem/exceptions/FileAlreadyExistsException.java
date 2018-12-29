/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.exceptions;

import org.xadisk.bridge.proxies.interfaces.XADiskBasicIOOperations;

/**
 * This exception is thrown by {@link XADiskBasicIOOperations#createFile(java.io.File, boolean)
 * createFile}, {@link XADiskBasicIOOperations#copyFile(java.io.File, java.io.File) copyFile} and
 * {@link XADiskBasicIOOperations#moveFile(java.io.File, java.io.File) moveFile} methods when
 * the file or directory (whichever relevant) that has to be created by the method already exists.
 *
 * <p> Note that the existence of a file/directory is derived from the perspective of the current
 * transaction. So, there may be a file/directory on disk, but which was deleted (virtually)
 * by the current transaction. Such a file/directory is <i>non-existing</i> for the current transaction.
 * Similarly, a file/directory which is not on disk, but which was created by the current transaction,
 * is <i>existing</i> from the perspective of the current transaction.
 * 
 * @since 1.0
 */
public class FileAlreadyExistsException extends XAApplicationException {

    private static final long serialVersionUID = 1L;
    private String path;

    public FileAlreadyExistsException(String path) {
        this.path = path;
    }

    @Override
    public String getMessage() {
        return "The file/directory [" + path + "] being created already exists.";
    }

    /**
     * Returns the path of the file/directory which already exists and hence can't be created.
     * <p> See the class description for definition of <i>existence</i> of
     * a file/directory.
     * @return the path of the file/directory.
     */
    public String getPath() {
        return path;
    }
}
