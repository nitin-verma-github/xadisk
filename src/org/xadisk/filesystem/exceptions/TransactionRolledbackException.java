/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.exceptions;

import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XADiskBasicIOOperations;
import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.bridge.proxies.interfaces.XASession;
import org.xadisk.connector.outbound.XADiskUserLocalTransaction;

/**
 * This exception is a subclass of {@link NoTransactionAssociatedException} and can
 * be thrown when a method call (in {@link Session}, {@link XASession}, {@link XADiskBasicIOOperations},
 * {@link XAFileInputStream}, {@link XAFileOutputStream} or {@link XADiskUserLocalTransaction})
 * was expecting an associated transaction, but such a transaction has already been rolled-back
 * by XADisk System.
 * <p> Such a situation may result when:
 * <ol>
 * <li> the current transaction has been already rolled-back by XADisk system due
 * to transaction time out.
 * <li> the current transaction gets rolled-back by XADisk system during the method call
 * itself (e.g. this transaction was involved in a deadlock and was rolled-back by XADisk system
 * to remedy the deadlock. See {@link DeadLockVictimizedException}).
 * </ol>
 * 
 * <p> For every exception of this class, there is always an associated cause; this cause
 * is usually {@link DeadLockVictimizedException} or {@link TransactionTimeoutException}.
 *
 * @since 1.0
 */
public class TransactionRolledbackException extends NoTransactionAssociatedException {

    private static final long serialVersionUID = 1L;

    public TransactionRolledbackException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getMessage() {
        return "The method call expected a transaction to be associated, but no such transaction exists. "
                + "An associated transaction that could be expected by the client had been rolled back earlier.";
    }
}
