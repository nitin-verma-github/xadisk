/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.interfaces;

import org.xadisk.connector.outbound.XADiskConnection;
import org.xadisk.connector.outbound.XADiskConnectionFactory;

/**
 * This interface is applicable only when invoking XADisk as a JCA Resource Adapter.
 * <p> This interface is a marker for connections to remote XADisk instances and is required/used
 * only by the JavaEE container itself. It should never be used inside JavaEE application code;
 * instead {@link XADiskConnection} interface should be used to refer to connection objects
 * obtained from both {@link XADiskConnectionFactory} and {@link XADiskRemoteConnectionFactory}.
 * (<i>In fact, {@link XADiskRemoteConnectionFactory} does not generate instances of this interface.</i>)
 *
 * @since 1.0
 */
public interface XADiskRemoteConnection extends XADiskConnection {
}
