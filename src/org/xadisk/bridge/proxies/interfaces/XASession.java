/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk http://xadisk.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.interfaces;

import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import org.xadisk.connector.outbound.XADiskConnection;

/**
 * This interface, like {@link Session}, is used by applications running in non-JCA
 * environments. While {@link Session} does not support XA/JTA Transactions, this
 * interface allows applications to use a JTA Transaction Manager to bind
 * XADisk and other XA-enabled resources with a single XA/JTA Transaction. This is
 * possible due to the {@link XAResource} implementation returned by the
 * {@link #getXAResource() getXAResource} method of this interface.
 * <p> An instance of this interface can be obtained from
 * {@link XAFileSystem#createSessionForXATransaction() createSessionForXATransaction}.
 * <p> For applications in JCA environments, use of {@link XADiskConnection} interface
 * should be preferable instead of this interface.
 * @since 1.1
 */
public interface XASession extends XADiskBasicIOOperations {

    /**
     * Returns the XADisk implementation of the standard {@link XAResource} interface.
     * <p> This {@link XAResource} can be enlisted into an XA/JTA Transaction
     * using {@link Transaction#enlistResource(XAResource) enlistResource} and hence allows the
     * {@link XASession} to participate in the XA/JTA Transaction.
     * <p> This {@link XAResource} implementation is fully compliant with JTA
     * and hence supports features like suspend/resume, one-phase commit
     * optimization, crash recovery, transaction time-out etc.
     * @return the XADisk implementation of {@link XAResource}.
     */
    public XAResource getXAResource();
}
