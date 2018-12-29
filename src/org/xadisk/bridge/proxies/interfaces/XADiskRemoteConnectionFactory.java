/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.interfaces;

import javax.resource.ResourceException;
import org.xadisk.connector.outbound.XADiskConnection;
import org.xadisk.connector.outbound.XADiskConnectionFactory;

/**
 * This interface is applicable only when invoking XADisk as a JCA Resource Adapter.
 * <p> This interface is a marker for connection factories to connect to remote (running on remote JVMs)
 * XADisk instances. Specifying name of this interface is normally required when creating a connection factory
 * in a JavaEE server to connect to remote XADisk instances.
 *
 * <p> It is recommended that code inside JavaEE application use the interface {@link XADiskConnectionFactory}
 * (and not this one) for interacting with instances of both {@link XADiskConnectionFactory} and
 * {@link XADiskRemoteConnectionFactory}.
 *
 * @since 1.0
 */
public interface XADiskRemoteConnectionFactory extends XADiskConnectionFactory {

    /**
     * Retrieves a new connection handle to interact with the remote XADisk instance.
     * @return a new connection handle.
     * @throws ResourceException
     */
    public XADiskConnection getConnection() throws ResourceException;
}
