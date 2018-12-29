/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.connector.outbound;

import javax.resource.spi.ConnectionEvent;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;
import org.xadisk.filesystem.FileSystemConfiguration;

/**
 * This class is applicable only when invoking XADisk as a JCA Resource Adapter.
 * <p> This represents a transaction object which can be used by JavaEE applications
 * when they want to control the transaction on an {@link XADiskConnection} by themselves
 * (in a resource-local way, not using XA transaction). Applications need to call
 * {@link XADiskConnection#getUserLocalTransaction getUserLocalTransaction} to obtain a handle
 * to this transaction object.
 *
 * @since 1.0
 */
public class XADiskUserLocalTransaction {

    private final XADiskLocalTransaction localTxnImpl;
    private final XADiskManagedConnection mc;

    XADiskUserLocalTransaction(XADiskManagedConnection mc) {
        this.localTxnImpl = new XADiskLocalTransaction(mc);
        this.mc = mc;
    }

    /**
     * Starts a local transaction on the associated connection, and binds that
     * transaction to this object.
     */
    public void beginLocalTransaction() {
        localTxnImpl._begin();
        mc.raiseUserLocalTransactionEvent(ConnectionEvent.LOCAL_TRANSACTION_STARTED);
    }

    /**
     * Commits the local transaction bound to this object.
     * @throws NoTransactionAssociatedException
     */
    public void commitLocalTransaction() throws NoTransactionAssociatedException {
        localTxnImpl._commit();
        mc.raiseUserLocalTransactionEvent(ConnectionEvent.LOCAL_TRANSACTION_COMMITTED);
    }

    /**
     * Rolls back the local transaction bound to this object.
     * @throws NoTransactionAssociatedException
     */
    public void rollbackLocalTransaction() throws NoTransactionAssociatedException {
        localTxnImpl._rollback();
        mc.raiseUserLocalTransactionEvent(ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK);
    }

    /**
     * Gets the transaction timeout value for the current transaction.
     * <p> Default value is obtained from the {@link FileSystemConfiguration#getTransactionTimeout()
     * global-configuration}.
     * @return the transaction timeout value, in seconds.
     */
    public int getTransactionTimeOut() {
        return localTxnImpl.getTransactionTimeOut();
    }

    /**
     * Sets the transaction timeout value for the current transaction.
     * <p> Default value is obtained from the {@link FileSystemConfiguration#getTransactionTimeout()
     * global-configuration}.
     * @param transactionTimeOut the new transaction timeout value, in seconds.
     */
    public void setTransactionTimeOut(int transactionTimeOut) {
        localTxnImpl.setTransactionTimeOut(transactionTimeOut);
    }
}
