/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.impl;

import java.io.IOException;
import javax.resource.ResourceException;
import org.xadisk.connector.outbound.XADiskManagedConnection;
import org.xadisk.filesystem.NativeXAFileSystem;

public class XADiskRemoteManagedConnection extends XADiskManagedConnection {

    public XADiskRemoteManagedConnection(String serverAddress, Integer serverPort, NativeXAFileSystem localXAFileSystem) throws IOException {
        super(new RemoteXAFileSystem(serverAddress, serverPort, localXAFileSystem), "dummy-value");
    }

    @Override
    public void cleanup() throws ResourceException {
        super.cleanup();
        ((RemoteXAFileSystem) theXAFileSystem).shutdown();
        //we shouldn't switch the xaFS object because the container might still
        //be using the "older" xaResource object which further points to the older
        //xaFS object. So, we rather keep using the "older" "xaFS" and don't reset it.
        //super.theXAFileSystem = new RemoteXAFileSystem(serverAddress, serverPort);
    }

    @Override
    public void destroy() throws ResourceException {
        super.destroy();
        ((RemoteXAFileSystem) theXAFileSystem).shutdown();
    }
}
