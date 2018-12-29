/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.impl;

import org.xadisk.bridge.proxies.facilitators.ByteArrayRemoteReference;
import org.xadisk.bridge.proxies.facilitators.RemoteMethodInvoker;
import org.xadisk.bridge.proxies.facilitators.RemoteObjectProxy;
import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;
import org.xadisk.filesystem.exceptions.ClosedStreamException;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;

public class RemoteXAFileInputStream extends RemoteObjectProxy implements XAFileInputStream {

    private static final long serialVersionUID = 1L;

    public RemoteXAFileInputStream(long objectId, RemoteMethodInvoker invoker) {
        super(objectId, invoker);
    }

    public int available() throws NoTransactionAssociatedException, ClosedStreamException {
        try {
            return (Integer) invokeRemoteMethod("available");
        } catch (NoTransactionAssociatedException tre) {
            throw tre;
        } catch (ClosedStreamException cse) {
            throw cse;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void close() throws NoTransactionAssociatedException {
        try {
            invokeRemoteMethod("close");
        } catch (NoTransactionAssociatedException tre) {
            throw tre;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public boolean isClosed() {
        try {
            return (Boolean) invokeRemoteMethod("isClosed");
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void position(long n) throws NoTransactionAssociatedException, ClosedStreamException {
        try {
            invokeRemoteMethod("position", n);
        } catch (NoTransactionAssociatedException tre) {
            throw tre;
        } catch (ClosedStreamException cse) {
            throw cse;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public long position() {
        try {
            return (Long) invokeRemoteMethod("position");
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public int read() throws ClosedStreamException, NoTransactionAssociatedException {
        try {
            return (Integer) invokeRemoteMethod("read");
        } catch (NoTransactionAssociatedException tre) {
            throw tre;
        } catch (ClosedStreamException cse) {
            throw cse;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public int read(byte[] b) throws ClosedStreamException, NoTransactionAssociatedException {
        try {
            ByteArrayRemoteReference ref = new ByteArrayRemoteReference(b, 0, b.length);
            return (Integer) invokeRemoteMethod("read", ref);
        } catch (NoTransactionAssociatedException tre) {
            throw tre;
        } catch (ClosedStreamException cse) {
            throw cse;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public int read(byte[] b, int off, int len) throws ClosedStreamException, NoTransactionAssociatedException {
        try {
            ByteArrayRemoteReference ref = new ByteArrayRemoteReference(b, off, len);
            return (Integer) invokeRemoteMethod("read", ref);
        } catch (NoTransactionAssociatedException tre) {
            throw tre;
        } catch (ClosedStreamException cse) {
            throw cse;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public long skip(long n) throws NoTransactionAssociatedException, ClosedStreamException {
        try {
            return (Long) invokeRemoteMethod("skip", n);
        } catch (NoTransactionAssociatedException tre) {
            throw tre;
        } catch (ClosedStreamException cse) {
            throw cse;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }
}
