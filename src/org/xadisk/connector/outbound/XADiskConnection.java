/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.connector.outbound;

import org.xadisk.bridge.proxies.interfaces.XADiskBasicIOOperations;

/**
 * This interface is applicable only when invoking XADisk as a JCA Resource Adapter.
 * <p> This interface represents the connection object used inside JavaEE applications
 * for calling I/O operations on XADisk. An instance of this can be obtained from
 * {@link XADiskConnectionFactory#getConnection() getConnection} method.
 *
 * @since 1.0
 */
public interface XADiskConnection extends XADiskBasicIOOperations {

    /**
     * Returns an instance of local transaction object which can be used by JavaEE applications
     * to control the transaction on this connection by themselves (in a resource-local way, not
     * using XA transaction).
     * @return a transaction object for demarcation of local transactions on this connection.
     */
    public XADiskUserLocalTransaction getUserLocalTransaction();

    /**
     * Closes this connection. This connection object can't be used after closing it.
     */
    public void close();
}
