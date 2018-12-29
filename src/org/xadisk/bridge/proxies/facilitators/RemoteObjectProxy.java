/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.facilitators;

import java.io.IOException;
import java.io.Serializable;

public class RemoteObjectProxy implements Serializable {

    private static final long serialVersionUID = 1L;
    protected final long remoteObjectId;
    protected RemoteMethodInvoker invoker;

    public RemoteObjectProxy(long remoteObjectId, RemoteMethodInvoker invoker) {
        this.remoteObjectId = remoteObjectId;
        this.invoker = invoker;
    }

    public void setInvoker(RemoteMethodInvoker invoker) {
        this.invoker = invoker;
    }

    protected RuntimeException assertExceptionHandling(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        } else {
            throw new AssertionError(t);
        }
    }

    protected Object invokeRemoteMethod(String methodName, Serializable... args) throws Throwable {
        Object response = invoker.invokeRemoteMethod(remoteObjectId, methodName, args);
        if (response instanceof RemoteObjectProxy) {
            ((RemoteObjectProxy) response).setInvoker(this.invoker);
        }
        return response;
    }

    public long getRemoteObjectId() {
        return remoteObjectId;
    }

    public void disconnect() {
        try {
            this.invoker.disconnect();
        } catch (IOException ioe) {
            //no-op.
        }
    }
}
