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
 * This exception is thrown by various methods in {@link Session},
 * {@link XASession}, {@link XADiskBasicIOOperations}, {@link XAFileInputStream},
 * {@link XAFileOutputStream} and {@link XADiskUserLocalTransaction}
 * when an associated (<i>current</i>) transaction was expected, but there is no such transaction.
 * <p> Such a situation may result when:
 * <ol>
 * <li> the current transaction has been explicitly rolled-back or committed earlier by the client
 * <li> the current transaction has been already rolled-back by XADisk system due
 * to transaction time out.
 * <li> the current transaction gets rolled-back by XADisk system during the method call
 * itself (e.g. this transaction was involved in a deadlock and was rolled-back by XADisk system
 * to remedy the deadlock. See {@link DeadLockVictimizedException}).
 * </ol>
 *
 * @since 1.0
 */
public class NoTransactionAssociatedException extends XAApplicationException {

    private static final long serialVersionUID = 1L;

    public NoTransactionAssociatedException() {
    }

    public NoTransactionAssociatedException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getMessage() {
        return "The method that was called can only be called with a transaction associated, but"
                + "there is no such transaction present.";
    }
}
