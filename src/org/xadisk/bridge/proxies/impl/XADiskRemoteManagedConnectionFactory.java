/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.impl;

import java.io.IOException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.security.auth.Subject;
import org.xadisk.connector.outbound.XADiskConnectionFactoryImpl;
import org.xadisk.connector.outbound.XADiskManagedConnectionFactory;
import org.xadisk.filesystem.NativeXAFileSystem;

public class XADiskRemoteManagedConnectionFactory extends XADiskManagedConnectionFactory {

    private static final long serialVersionUID = 1L;
    private String serverAddress;
    private Integer serverPort;

    public XADiskRemoteManagedConnectionFactory() {
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {
        try {
            return new XADiskRemoteManagedConnection(serverAddress, serverPort, NativeXAFileSystem.getXAFileSystem(super.getInstanceId()));
        } catch (IOException ioe) {
            throw new ResourceException(ioe);
        }
    }

    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        return new XADiskConnectionFactoryImpl(this, cm);
    }

    @Override
    public int hashCode() {
        return serverAddress.hashCode() + serverPort.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof XADiskRemoteManagedConnectionFactory) {
            XADiskRemoteManagedConnectionFactory that = (XADiskRemoteManagedConnectionFactory) obj;
            return (that.serverAddress == null ? this.serverAddress == null : that.serverAddress.equalsIgnoreCase(this.serverAddress)) && that.serverPort.equals(this.serverPort);
        }
        return false;
    }
}
