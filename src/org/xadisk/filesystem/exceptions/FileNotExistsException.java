/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.exceptions;

import org.xadisk.bridge.proxies.interfaces.XADiskBasicIOOperations;

/**
 * This exception is thrown by those I/O methods in {@link XADiskBasicIOOperations}
 * which are expecting a file/directory to exist, but didn't find it.
 *
 * <p> Note that the existence of a file/directory is derived from the perspective of the current
 * transaction. So, there may be a file/directory on disk, but which was deleted (virtually)
 * by the current transaction. Such a file/directory is <i>non-existing</i> for the current transaction.
 * Similarly, a file/directory which is not on disk, but which was created by the current transaction,
 * is <i>existing</i> from the perspective of the current transaction.
 *
 * @since 1.0
 */
public class FileNotExistsException extends XAApplicationException {

    private static final long serialVersionUID = 1L;
    private String path;

    public FileNotExistsException(String path) {
        this.path = path;
    }

    @Override
    public String getMessage() {
        return "The file/directory [" + path + "] is expected by the i/o operation, but does not exist.";
    }

    /**
     * Returns the path of the file/directory which was expected by the i/o operation, but
     * does not exist.
     * <p> See the class description for definition of <i>existence</i> of
     * a file/directory.
     * @return the path of the file/directory.
     */
    public String getPath() {
        return path;
    }
}
