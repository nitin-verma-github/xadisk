/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.connector.inbound;

import java.io.Serializable;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import org.xadisk.bridge.proxies.impl.RemoteMessageEndpointFactory;
import org.xadisk.filesystem.NativeXAFileSystem;

public class EndPointActivation implements Serializable {

    private static final long serialVersionUID = 1L;
    private final MessageEndpointFactory messageEndpointFactory;
    private final XADiskActivationSpecImpl activationSpecImpl;

    public EndPointActivation(MessageEndpointFactory messageEndpointFactory, XADiskActivationSpecImpl activationSpecImpl) {
        this.messageEndpointFactory = messageEndpointFactory;
        this.activationSpecImpl = activationSpecImpl;
    }

    public MessageEndpointFactory getMessageEndpointFactory() {
        return messageEndpointFactory;
    }

    public XADiskActivationSpecImpl getActivationSpecImpl() {
        return activationSpecImpl;
    }

    public void setLocalXAFileSystemForRemoteMEF(NativeXAFileSystem localXAFileSystem) {
        if (messageEndpointFactory instanceof RemoteMessageEndpointFactory) {
            ((RemoteMessageEndpointFactory) messageEndpointFactory).setLocalXAFileSystem(localXAFileSystem);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EndPointActivation) {
            EndPointActivation epActivation = (EndPointActivation) obj;
            return epActivation.activationSpecImpl.equals(this.activationSpecImpl)
                    && epActivation.messageEndpointFactory.equals(this.messageEndpointFactory);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return messageEndpointFactory.hashCode() + activationSpecImpl.hashCode();
    }
}
