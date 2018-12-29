/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.connector.outbound;

import java.io.Serializable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;

public class ConnectionManagerImpl implements ConnectionManager, Serializable {

    private static final long serialVersionUID = 1L;

    public Object allocateConnection(ManagedConnectionFactory arg0, ConnectionRequestInfo arg1) throws ResourceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
