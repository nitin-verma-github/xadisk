/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.connector.outbound;

import java.io.Serializable;
import javax.resource.Referenceable;
import javax.resource.ResourceException;

/**
 * This interface is applicable only when invoking XADisk as a JCA Resource Adapter.
 * <p> This interface represents a connection factory used by JavaEE applications to obtain
 * connections to a native (running in the same JVM) XADisk instance.
 * <p> Specifying name of this interface is normally required when creating a connection factory
 * in a JavaEE server to connect to a native XADisk instance.
 *
 * @since 1.0
 */
public interface XADiskConnectionFactory extends Serializable, Referenceable {

    /**
     * Retrieves a new connection handle to interact with the target native XADisk instance.
     * @return a new connection handle.
     * @throws ResourceException
     */
    public XADiskConnection getConnection() throws ResourceException;
}
