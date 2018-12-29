/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.exceptions;

import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XADiskBasicIOOperations;
import org.xadisk.bridge.proxies.interfaces.XASession;
import org.xadisk.connector.outbound.XADiskConnection;
import org.xadisk.filesystem.FileSystemConfiguration;

/**
 * This exception is thrown by the i/o operation methods in
 * {@link XADiskBasicIOOperations} when a lock over a relevant file/directory could
 * not be acquired within the lock wait timeout period. Waiting for a lock
 * allows some space for other transactions holding the lock to complete and release
 * the locks.
 * <p> Note that the value of lock wait timeout period defaults to the
 * {@link FileSystemConfiguration#getLockTimeOut() global-configuration}, and can be overridden
 * at a {@link Session}/{@link XASession}/{@link XADiskConnection} level by
 * {@link XADiskBasicIOOperations#setFileLockWaitTimeout(long) setFileLockWaitTimeout}.
 *
 * @since 1.0
 */
public class LockingTimedOutException extends LockingFailedException {

    private static final long serialVersionUID = 1L;

    public LockingTimedOutException(String path) {
        super(path);
    }

    @Override
    public String getMessage() {
        return super.getGenericMessage() + " The reason is : "
                + "An attempt to acquire the lock has timed-out.";
    }
}
