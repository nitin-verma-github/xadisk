/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.impl;

import org.xadisk.bridge.proxies.facilitators.RemoteMethodInvoker;
import org.xadisk.bridge.proxies.facilitators.RemoteObjectProxy;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.filesystem.exceptions.ClosedStreamException;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;

public class RemoteXAFileOutputStream extends RemoteObjectProxy implements XAFileOutputStream {

    private static final long serialVersionUID = 1L;

    public RemoteXAFileOutputStream(long objectId, RemoteMethodInvoker invoker) {
        super(objectId, invoker);
    }

    public void write(int b) throws ClosedStreamException, NoTransactionAssociatedException {
        try {
            invokeRemoteMethod("write", b);
        } catch (NoTransactionAssociatedException tre) {
            throw tre;
        } catch (ClosedStreamException cse) {
            throw cse;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void write(byte[] b) throws ClosedStreamException, NoTransactionAssociatedException {
        try {
            invokeRemoteMethod("write", b);
        } catch (NoTransactionAssociatedException tre) {
            throw tre;
        } catch (ClosedStreamException cse) {
            throw cse;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void write(byte[] b, int off, int len) throws ClosedStreamException, NoTransactionAssociatedException {
        try {
            byte[] onWire = new byte[len];
            System.arraycopy(b, off, onWire, 0, len);
            invokeRemoteMethod("write", b, off, len);
        } catch (NoTransactionAssociatedException tre) {
            throw tre;
        } catch (ClosedStreamException cse) {
            throw cse;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void flush() throws ClosedStreamException, NoTransactionAssociatedException {
        try {
            invokeRemoteMethod("flush");
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
}
