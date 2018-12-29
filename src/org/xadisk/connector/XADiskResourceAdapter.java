/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.connector;

import org.xadisk.connector.inbound.EndPointActivation;
import org.xadisk.connector.inbound.XADiskActivationSpecImpl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;
import org.xadisk.bridge.proxies.impl.RemoteXAFileSystem;
import org.xadisk.filesystem.FileSystemConfiguration;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.XAFileSystemCommonness;
import org.xadisk.filesystem.exceptions.XASystemException;

public class XADiskResourceAdapter extends FileSystemConfiguration implements ResourceAdapter {

    private static final long serialVersionUID = 1L;
    private transient NativeXAFileSystem xaFileSystem;

    public void start(BootstrapContext bsContext) throws ResourceAdapterInternalException {
        try {
            this.xaFileSystem = NativeXAFileSystem.bootXAFileSystem(this, bsContext.getWorkManager());
        } catch (XASystemException xase) {
            throw new ResourceAdapterInternalException(xase);
        }
    }

    public void stop() {
        try {
            xaFileSystem.shutdown();
        } catch (IOException ioe) {
        }
    }

    public void endpointActivation(MessageEndpointFactory mef, ActivationSpec as) throws ResourceException {
        try {
            XADiskActivationSpecImpl xadiskAS = (XADiskActivationSpecImpl) as;
            EndPointActivation epActivation = new EndPointActivation(mef, xadiskAS);
            if (Boolean.valueOf(xadiskAS.getAreFilesRemote())) {
                String serverAddress = xadiskAS.getRemoteServerAddress();
                Integer serverPort = Integer.valueOf(xadiskAS.getRemoteServerPort());
                RemoteXAFileSystem remoteXAFS = new RemoteXAFileSystem(serverAddress, serverPort, xaFileSystem);
                remoteXAFS.registerEndPointActivation(epActivation);
                remoteXAFS.shutdown();
            } else {
                xaFileSystem.registerEndPointActivation(epActivation);
            }
        } catch (IOException ioe) {
            throw new ResourceException(ioe);
        }
    }

    public void endpointDeactivation(MessageEndpointFactory mef, ActivationSpec as) {
        try {
            XADiskActivationSpecImpl xadiskAS = (XADiskActivationSpecImpl) as;
            EndPointActivation epActivation = new EndPointActivation(mef, xadiskAS);
            if (Boolean.valueOf(xadiskAS.getAreFilesRemote())) {
                String serverAddress = xadiskAS.getRemoteServerAddress();
                Integer serverPort = Integer.valueOf(xadiskAS.getRemoteServerPort());
                RemoteXAFileSystem remoteXAFS = new RemoteXAFileSystem(serverAddress, serverPort, xaFileSystem);
                remoteXAFS.deRegisterEndPointActivation(epActivation);
                remoteXAFS.shutdown();
            } else {
                xaFileSystem.deRegisterEndPointActivation(epActivation);
            }
        } catch (IOException ioe) {
            /* JCA Spec : "Any exception thrown by the endpointDeactivation method call must be
             ignored. After this method call the endpoint is deemed inactive." */
            ioe.printStackTrace();
        }
    }

    public XAResource[] getXAResources(ActivationSpec[] as) throws ResourceException {
        //as we now can have connectivity to multiple remote xadisk instances, so modifying this method.
        List<XAResource> xars = new ArrayList<XAResource>();
        //though we could not have same LocalEPXAR because of "binding" with "event"
        //variable; we can easily do this during recovery; both for local and remote xadisk
        //instances.
        Set<String> uniqueXADiskInstances = new HashSet<String>();
        for (int i = 0; i < as.length; i++) {
            XADiskActivationSpecImpl xadiskAS = (XADiskActivationSpecImpl) as[i];
            if (Boolean.valueOf(xadiskAS.getAreFilesRemote())) {
                String serverAddress = xadiskAS.getRemoteServerAddress();
                Integer serverPort = Integer.valueOf(xadiskAS.getRemoteServerPort());
                uniqueXADiskInstances.add(serverAddress + ":" + serverPort);
            } else {
                uniqueXADiskInstances.add("_");
            }
        }
        for (String xadiskLocations : uniqueXADiskInstances) {
            XAFileSystemCommonness uniqueXAFileSystem;
            String location[] = xadiskLocations.split(":");
            if (location.length == 2) {
                uniqueXAFileSystem = new RemoteXAFileSystem(location[0], Integer.valueOf(location[1]), xaFileSystem);
            } else {
                uniqueXAFileSystem = this.xaFileSystem;
            }
            xars.add(uniqueXAFileSystem.getEventProcessingXAResourceForRecovery());
        }
        return xars.toArray(new XAResource[0]);
    }
}
