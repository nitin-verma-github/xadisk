/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.impl;

import java.io.IOException;
import org.xadisk.bridge.proxies.facilitators.RemoteMethodInvoker;
import org.xadisk.bridge.proxies.facilitators.RemoteObjectProxy;
import java.lang.reflect.Method;
import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpoint;
import org.xadisk.bridge.proxies.facilitators.MethodSerializabler;
import org.xadisk.bridge.proxies.facilitators.SerializedMethod;
import org.xadisk.connector.inbound.FileSystemEventListener;
import org.xadisk.filesystem.FileSystemStateChangeEvent;

public class RemoteMessageEndpoint extends RemoteObjectProxy implements MessageEndpoint, FileSystemEventListener {

    private static final long serialVersionUID = 1L;

    public RemoteMessageEndpoint(long objectId, RemoteMethodInvoker invoker) {
        super(objectId, invoker);
    }

    public void beforeDelivery(Method method) throws NoSuchMethodException, ResourceException {
        try {
            SerializedMethod serializableMethod = new MethodSerializabler().serialize(method);
            invokeRemoteMethod("beforeDelivery", serializableMethod);
        } catch (NoSuchMethodException nsme) {
            throw nsme;
        } catch (ResourceException re) {
            throw re;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public void onFileSystemEvent(FileSystemStateChangeEvent event) {
        try {
            invokeRemoteMethod("onFileSystemEvent", event);
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public void afterDelivery() throws ResourceException {
        try {
            invokeRemoteMethod("afterDelivery");
        } catch (ResourceException re) {
            throw re;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public void release() {
        try {
            invokeRemoteMethod("release");
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public void shutdown() {
        disconnect();
    }
}
