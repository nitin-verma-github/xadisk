/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.exceptions;

/**
 * This exception can appear as a "cause" of a {@link TransactionRolledbackException}
 * and indicates that an attempt to acquire required locks (over file/directory objects) resulted in a deadlock
 * <i> (a situation when a set of transactions asking for resources, here locks, and also holding
 * some resources, form a cycle of dependencies. This results in all transactions in the cycle getting stuck)
 * </i>, and among the transactions which are member of the deadlock cycle, the
 * current transaction was chosen, by XADisk system, for rollback to remedy the deadlock.
 *
 * @since 1.0
 */
public class DeadLockVictimizedException extends XAApplicationException {

    private static final long serialVersionUID = 1L;
    private String path;

    public DeadLockVictimizedException(String path) {
        this.path = path;
    }

    @Override
    public String getMessage() {
        return "The current transaction was rolled back prematurely because the transaction"
                + " was one of the transactions stuck in a deadlock, and was chosen for rollback"
                + " as a remedy to the deadlock.";
    }

    /**
     * Returns the file/directory's path, waiting for lock on which resulted in this exception.
     * @return the path of the file/directory.
     */
    public String getPath() {
        return path;
    }
}
