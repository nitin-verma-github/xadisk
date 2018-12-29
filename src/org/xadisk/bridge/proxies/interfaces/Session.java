/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.interfaces;

import org.xadisk.connector.outbound.XADiskConnection;
import org.xadisk.filesystem.FileSystemConfiguration;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;

/**
 * This interface is used to invoke i/o operations on XADisk and to control the transaction 
 * associated with this session (use {@link XADiskConnection} instead of this interface when XADisk is 
 * used as a JCA Resource Adapter. For XA transactions in non-JCA environments, use {@link XASession}).
 * An instance of this interface can be obtained from
 * {@link XAFileSystem#createSessionForLocalTransaction() createSessionForLocalTransaction}.
 * <p> Before the session object is returned, a new transaction is associated with it. Once a
 * session completes its associated transaction using {@link #commit()} or
 * {@link #rollback()} or automatically via implicit rollback (e.g. due to transaction time out),
 * it can no more be used. To start another transaction, one needs to obtain another
 * session object.
 *
 * @since 1.0
 */
public interface Session extends XADiskBasicIOOperations {

    /**
     * Sets the transaction timeout value for the transaction associated with this session.
     * <p> Default value is obtained from the {@link FileSystemConfiguration#getTransactionTimeout()
     * global-configuration}.
     * @param transactionTimeout the new transaction timeout value, in seconds.
     * @return true, if the operation succeeds.
     */
    public boolean setTransactionTimeout(int transactionTimeout);

    /**
     * Returns the current transaction timeout value.
     * <p> Default value is obtained from the {@link FileSystemConfiguration#getTransactionTimeout()
     * global-configuration}.
     * @return the transaction timeout value, in seconds.
     */
    public int getTransactionTimeout();

    /**
     * Rolls back the transaction associated with this Session.
     * @throws NoTransactionAssociatedException
     */
    public void rollback() throws NoTransactionAssociatedException;

    /**
     * Commits the transaction associated with this Session.
     * @throws NoTransactionAssociatedException
     */
    public void commit() throws NoTransactionAssociatedException;
}
