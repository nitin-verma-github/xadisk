/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.exceptions;

import org.xadisk.bridge.proxies.interfaces.XADiskBasicIOOperations;

/**
 * This exception is thrown by the i/o operation methods in
 * {@link XADiskBasicIOOperations} when the operation cannot be performed due to insufficient
 * permissions over the file/directory involved in the operation.
 *
 * @since 1.0
 */
public class InsufficientPermissionOnFileException extends XAApplicationException {

    private static final long serialVersionUID = 1L;
    private XADiskBasicIOOperations.PermissionType missingPermission;
    private String path;

    public InsufficientPermissionOnFileException(XADiskBasicIOOperations.PermissionType missingPermission, String path) {
        this.missingPermission = missingPermission;
        this.path = path;
    }

    @Override
    public String getMessage() {
        return "Permission of type [" + missingPermission.name() + "] is needed over"
                + " the file/directory with path [" + path + "] for the i/o operation to succeed.";
    }

    /**
     * Returns the path of the file/directory on which required permissions are missing.
     * @return the path of the file/directory.
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the permission which was found missing and is required to complete the
     * operation.
     * @return the required permission.
     */
    public XADiskBasicIOOperations.PermissionType getMissingPermission() {
        return missingPermission;
    }
}
