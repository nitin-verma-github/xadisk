/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.interfaces;

import java.io.IOException;
import javax.transaction.xa.XAResource;
import org.xadisk.filesystem.exceptions.RecoveryInProgressException;
import org.xadisk.filesystem.exceptions.TransactionFailedException;
import org.xadisk.filesystem.exceptions.XASystemNoMoreAvailableException;

/**
 * This interface represents a reference to an XADisk instance.
 * <p> An implementation of this interface is obtained using any of these methods:
 * <ol>
 * <li> {@link XAFileSystemProxy#bootNativeXAFileSystem(StandaloneFileSystemConfiguration) bootNativeXAFileSystem}
 * (for XADisk instance in the same JVM, i.e. a native XADisk instance)
 * <li> {@link XAFileSystemProxy#getNativeXAFileSystemReference(String) getNativeXAFileSystemReference}
 * (for XADisk instance in the same JVM, i.e. a native XADisk instance)
 * <li> {@link XAFileSystemProxy#getRemoteXAFileSystemReference(String, int) getRemoteXAFileSystemReference}
 * (for connecting to an XADisk instance running on a separate JVM, i.e. remote XADisk instance).
 * </ol>
 *
 * @since 1.0
 */
public interface XAFileSystem {

    /**
     * Creates a new session and associates a local (non-XA) transaction with it.
     * @return the new session.
     */
    public Session createSessionForLocalTransaction();

    /**
     * Creates a new session capable of participating in an XA/JTA transaction.
     * <p> This method is useful for those applications which are running in a non-JCA environment
     * and want to use a JTA Transaction Manager with XADisk and other XA-enabled resources.
     * @return the new session.
     * @since 1.1
     */
    public XASession createSessionForXATransaction();

    /**
     * Returns an XAResource object which would enable a standalone JTA Transaction Manager
     * to perform transaction recovery after a crash. Typically, a Transaction Manager
     * would retrieve the list of transactions to be recovered using the method {@link XAResource#recover(int) recover()},
     * and then would complete each transaction using {@link XAResource#commit(javax.transaction.xa.Xid, boolean) commit()}
     * or {@link XAResource#rollback(javax.transaction.xa.Xid) rollback()}.
     * <p> This method is useful for those applications which are running in a non-JCA environment
     * and are using a standalone JTA Transaction Manager with XADisk and other XA-enabled resources.
     * @return the XAResource object.
     * @since 1.2
     */
    public XAResource getXAResourceForRecovery();

    /**
     * Waits for this XADisk instance to complete its booting and become ready to use. The timeout
     * specifies the maximum amount of time to wait.
     * <p> If the timeout expires before boot completion, the {@link RecoveryInProgressException}
     * is thrown.
     * <p> If a booting problem is encountered before timeout, an instance of
     * {@link XASystemNoMoreAvailableException} is thrown with appropriate <i>cause</i> set.
     * <p> Note that XADisk completes (rollback or commit) all of its <i>ongoing</i> (which were running during
     * last shutdown/crash of XADisk) local transactions and XA transactions as a part of its boot process.
     * For in-doubt XA transactions, an XADisk instance waits for the Transaction Manager
     * to inform it about the transaction decision; XADisk keeps boot-completion on hold
     * due to these in-doubt XA transactions.
     * @param timeout number of milliseconds to wait.
     * @throws InterruptedException
     */
    public void waitForBootup(long timeout) throws InterruptedException;

    /**
     * If this is a reference to a native XADisk instance, this method shuts down the XADisk instance
     * referenced by <i>this</i> object.
     * <p> If this is a reference to a remote XADisk instance, this method only disconnects from
     * the remote XADisk instance referenced by <i>this</i> object.
     * @throws IOException
     */
    public void shutdown() throws IOException;

    /**
     * Returns a two-dimensional array <i>arr</i> of bytes where each of <i>arr[0], arr[1], arr[2]...</i>
     * represents a transaction identifier in the byte[] form. These transactions have failed
     * during normal operations or during the recovery phase of XADisk.
     * <p> See {@link TransactionFailedException}.
     * @return a two-dimensional byte array containing the transaction identifiers.
     * @since 1.2.2
     */
    public byte[][] getIdentifiersForFailedTransactions();

    /**
     * Mark the transaction specified by the input <i>transactionIdentifier</i> as complete.
     * <p> See {@link TransactionFailedException}.
     * @param transactionIdentifier the identifier for the transaction to be marked as complete.
     * @since 1.2.2
     */
    public void declareTransactionAsComplete(byte[] transactionIdentifier);
}
