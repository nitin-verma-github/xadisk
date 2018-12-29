/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.impl;

import java.io.IOException;
import java.util.ArrayList;
import org.xadisk.bridge.proxies.facilitators.RemoteMethodInvoker;
import org.xadisk.bridge.proxies.facilitators.RemoteObjectProxy;
import java.util.concurrent.ConcurrentHashMap;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.filesystem.TransactionInformation;

public class RemoteEventProcessingXAResource extends RemoteObjectProxy implements XAResource {

    private static final long serialVersionUID = 1L;
    private transient ConcurrentHashMap<Xid, TransactionInformation> internalXids = new ConcurrentHashMap<Xid, TransactionInformation>();
    private final Object lockOnInternalXids = new ArrayList<Object>(0);//just needed something that can
    //be made final (for synch block) and is serializable ( so that it is not null on the remote side;
    //because to avoid that readObject needs the line to re-assign the value making it not eligible for final.

    private void readObject(java.io.ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.internalXids = new ConcurrentHashMap<Xid, TransactionInformation>();
    }

    public RemoteEventProcessingXAResource(long objectId, RemoteMethodInvoker invoker) {
        super(objectId, invoker);
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        try {
            TransactionInformation xidImpl = mapToInternalXid(xid);
            invokeRemoteMethod("commit", xidImpl, onePhase);
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        } finally {
            this.disconnect();
        }
    }

    public void end(Xid xid, int flags) throws XAException {
        try {
            TransactionInformation xidImpl = mapToInternalXid(xid);
            invokeRemoteMethod("end", xidImpl, flags);
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public void forget(Xid xid) throws XAException {
        try {
            TransactionInformation xidImpl = mapToInternalXid(xid);
            invokeRemoteMethod("forget", xidImpl);
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public int getTransactionTimeout() throws XAException {
        try {
            return (Integer) invokeRemoteMethod("getTransactionTimeout");
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public boolean isSameRM(XAResource xar) throws XAException {
        //it was dangerous (wrong) in the earlier impl to compare the invoker and say "same" if
        //the remoteAddr/Port was same. Note that the remote object, mapped using objectId,
        //might be release anytime, so we should be careful in so that these pointers (IDs)
        //to be referenced. For example, we have would be removing the remote XAR after say commit,
        //the object ID of the "sameRM"'s XAR would be pointing to a non-existing object.
        //Now, for this example, this was not the case as we didn't removed the remote LocalXAR.
        //But the main problem was that the objects of LocalEventProcessingXAResource are
        //use-once only as is visible from their field variables.
        return false;
    }

    public int prepare(Xid xid) throws XAException {
        try {
            TransactionInformation xidImpl = mapToInternalXid(xid);
            return (Integer) invokeRemoteMethod("prepare", xidImpl);
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public Xid[] recover(int flag) throws XAException {
        try {
            return (Xid[]) invokeRemoteMethod("recover", flag);
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public void rollback(Xid xid) throws XAException {
        try {
            TransactionInformation xidImpl = mapToInternalXid(xid);
            invokeRemoteMethod("rollback", xidImpl);
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        } finally {
            //an assumption that this XAR would be used only for a single txn in lifetime. Though its not always true.
            //But correctness is anyway retained, because after disconnection, it would automatically re-connect in case of further method invocations.
            this.disconnect();
        }
    }

    public boolean setTransactionTimeout(int timeout) throws XAException {
        try {
            return (Boolean) invokeRemoteMethod("setTransactionTimeout", timeout);
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public void start(Xid xid, int flag) throws XAException {
        try {
            TransactionInformation xidImpl = mapToInternalXid(xid);
            invokeRemoteMethod("start", xidImpl, flag);
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    private TransactionInformation mapToInternalXid(Xid xid) {
        synchronized (lockOnInternalXids) {
            TransactionInformation internalXid = internalXids.get(xid);
            if (internalXid == null) {
                internalXid = new TransactionInformation(xid);
                internalXids.put(xid, internalXid);
            }
            return internalXid;
        }
    }
}
