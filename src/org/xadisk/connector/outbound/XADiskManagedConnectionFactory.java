/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.connector.outbound;

import java.io.PrintWriter;
import java.util.Set;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;
import org.xadisk.bridge.proxies.impl.XADiskRemoteManagedConnectionFactory;
import org.xadisk.filesystem.NativeXAFileSystem;

public class XADiskManagedConnectionFactory implements ManagedConnectionFactory {

    private static final long serialVersionUID = 1L;
    private transient volatile PrintWriter logWriter;
    private String instanceId;

    public XADiskManagedConnectionFactory() {
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Object createConnectionFactory() throws ResourceException {
        throw new NotSupportedException("Works only in managed environments.");
    }

    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        return new XADiskConnectionFactoryImpl(this, cm);
    }

    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {
        return new XADiskManagedConnection(NativeXAFileSystem.getXAFileSystem(instanceId), instanceId);
    }

    public ManagedConnection matchManagedConnections(Set candidates, Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {
        boolean glassFish = false;
        /*Throwing NSE doesn't work for glassfish and JBoss atleast. So, one workaround for
         developers was to disable connection pooling when working with glassfish. But JBoss doeesn't
         seem to have an option for disabling pooling. Looking
         broadly, not "all" j2ee server implementations may support disabling of pooling; so i am
         now implementing a hack "inside". I will return the first connection blindly for "local connection"
         cases, and for remote connections, no need to check for address/port as an MCF will get only
         those in the set which are from that MCF itself (i trust).
         */
        //throw new NotSupportedException("Please don't pool connections to this EIS");
        if (candidates.size() == 0) {
            return null;
        }
        Object mc = candidates.iterator().next();
        if (mc instanceof XADiskManagedConnection) {
            return (XADiskManagedConnection) mc;
        }
        return null;
    }

    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

    public void setLogWriter(PrintWriter logWriter) throws ResourceException {
        this.logWriter = logWriter;
    }

    @Override
    public int hashCode() {
        return instanceId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof XADiskManagedConnectionFactory
                && !(obj instanceof XADiskRemoteManagedConnectionFactory)) {
            XADiskManagedConnectionFactory that = (XADiskManagedConnectionFactory) obj;
            return this.instanceId.equals(that.getInstanceId());
        }
        return false;
    }
}
