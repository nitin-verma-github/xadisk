/*
 Copyright © 2010-2014, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.connector.inbound;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.transaction.Status;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.filesystem.FileSystemStateChangeEvent;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.TransactionLogEntry;
import org.xadisk.filesystem.TransactionInformation;
import org.xadisk.filesystem.utilities.MiscUtils;

public class LocalEventProcessingXAResource implements XAResource {

    private final ConcurrentHashMap<Xid, TransactionInformation> internalXids = new ConcurrentHashMap<Xid, TransactionInformation>(1000);
    private final Object lockOnInternalXids = new ArrayList<Object>(0);
    private final NativeXAFileSystem xaFileSystem;
    private final FileSystemStateChangeEvent event;
    private volatile boolean returnedAllPreparedTransactions = false;
    private final boolean isCreatedForRecovery;
    private volatile HashMap<TransactionInformation, FileSystemStateChangeEvent> dequeuingTransactionsPreparedPreCrash;
    private byte transactionOutcome = Status.STATUS_NO_TRANSACTION;

    public LocalEventProcessingXAResource(NativeXAFileSystem xaFileSystem, FileSystemStateChangeEvent event) {
        this.xaFileSystem = xaFileSystem;
        this.event = event;
        this.isCreatedForRecovery = false;
    }

    public LocalEventProcessingXAResource(NativeXAFileSystem xaFileSystem) {
        this.xaFileSystem = xaFileSystem;
        this.isCreatedForRecovery = true;
        this.event = null;
    }

    public void start(Xid xid, int flags) throws XAException {
        //XidImpl xidImpl = mapToInternalXid(xid);
        if (flags == XAResource.TMNOFLAGS) {
        } else {
            //unexpected.
        }
    }

    public void end(Xid xid, int flags) throws XAException {
        //XidImpl xidImpl = mapToInternalXid(xid);
    }

    public int prepare(Xid xid) throws XAException {
        TransactionInformation xidImpl = mapToInternalXid(xid);
        if (isCreatedForRecovery) {
            //not expected.
        }
        try {
            xaFileSystem.getTheGatheringDiskWriter().transactionPrepareCompletesForEventDequeue(xidImpl, event);
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailureAndContinue(ioe);
            throw MiscUtils.createXAExceptionWithCause(XAException.XAER_RMFAIL, ioe);
        }
        return XAResource.XA_OK;
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        TransactionInformation xidImpl = mapToInternalXid(xid);
        FileSystemStateChangeEvent eventForTransaction = null;
        try {
            if (isCreatedForRecovery) {
                eventForTransaction = dequeuingTransactionsPreparedPreCrash.get(xidImpl);
            } else {
                eventForTransaction = this.event;
            }
            ArrayList<FileSystemStateChangeEvent> events = new ArrayList<FileSystemStateChangeEvent>(1);
            events.add(eventForTransaction);
            ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xidImpl, events,
                    TransactionLogEntry.EVENT_DEQUEUE));
            xaFileSystem.getTheGatheringDiskWriter().forceLog(xidImpl, logEntryBytes);
            xaFileSystem.getTheGatheringDiskWriter().transactionCompletes(xidImpl, true);
            if (isCreatedForRecovery) {
                xaFileSystem.getRecoveryWorker().cleanupTransactionInfo(xidImpl);
            }
            this.transactionOutcome = Status.STATUS_COMMITTED;
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailureAndContinue(ioe);
            throw MiscUtils.createXAExceptionWithCause(XAException.XAER_RMFAIL, ioe);
        } finally {
            releaseFromInternalXidMap(xid);
        }
    }

    public void rollback(Xid xid) throws XAException {
        TransactionInformation xidImpl = mapToInternalXid(xid);
        try {
            xaFileSystem.getTheGatheringDiskWriter().transactionCompletes(xidImpl, false);
            if (isCreatedForRecovery) {
                xaFileSystem.getRecoveryWorker().cleanupTransactionInfo(xidImpl);
            }
            this.transactionOutcome = Status.STATUS_ROLLEDBACK;
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailureAndContinue(ioe);
            throw MiscUtils.createXAExceptionWithCause(XAException.XAER_RMFAIL, ioe);
        } finally {
            releaseFromInternalXidMap(xid);
        }
    }

    public void forget(Xid xid) throws XAException {
        //XidImpl xidImpl = mapToInternalXid(xid);
    }

    public Xid[] recover(int flag) throws XAException {
        if (flag == XAResource.TMSTARTRSCAN) {
            returnedAllPreparedTransactions = false;
        }
        if (flag == (XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN)) {
            returnedAllPreparedTransactions = false;
        }
        if (returnedAllPreparedTransactions) {
            return new Xid[0];
        }

        dequeuingTransactionsPreparedPreCrash = xaFileSystem.getRecoveryWorker().
                getPreparedInDoubtTransactionsOfDequeue();

        Xid xids[];
        Set<TransactionInformation> xidsSet = dequeuingTransactionsPreparedPreCrash.keySet();
        xids = xidsSet.toArray(new Xid[0]);
        returnedAllPreparedTransactions = true;
        return xids;
    }

    public int getTransactionTimeout() throws XAException {
        return 0;
    }

    public boolean setTransactionTimeout(int arg0) throws XAException {
        return false;
    }

    public boolean isSameRM(XAResource obj) throws XAException {
        //never ever return true : note that this object is use-once; mainly the
        //"event" object is being used in prepare/commit; so imagine the disaster
        //if the TM keeps calling commit on the same XAR because it thinks that
        //the 2 local XARs are same ; though they are actually same "RM" here but
        //the consequence is that the "call commit on any of the sameRM xar" created
        //problem.
        return false;
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

    private void releaseFromInternalXidMap(Xid xid) {
        internalXids.remove(xid);
    }

    public byte getTransactionOutcome() {
        return transactionOutcome;
    }
}
