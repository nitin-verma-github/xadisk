/*
 Copyright © 2010-2014, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem;

import java.io.File;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class NativeLock implements Lock {

    private static final long serialVersionUID = 1L;
    private final File resource;
    private boolean exclusive = false;
    private boolean upgraded = false;
    private int numHolders = 0;
    private final HashSet<TransactionInformation> holders = new HashSet<TransactionInformation>(10);
    private final ReentrantLock synchLock = new ReentrantLock(false);
    private final Condition mayBeReadable = synchLock.newCondition();
    private final Condition mayBeWritable = synchLock.newCondition();
    private final LockTreeNode node;//to keep a "strong" ref to this node, session->allAcquiredLocks->node.

    NativeLock(boolean exclusive, File resource, LockTreeNode node) {
        this.exclusive = exclusive;
        this.resource = resource;
        this.node = node;
    }

    int getNumHolders() {
        return numHolders;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    void addHolder(TransactionInformation xid) {
        numHolders++;
        holders.add(xid);
    }

    void removeHolder(TransactionInformation xid) {
        numHolders--;
        holders.remove(xid);
    }

    public HashSet<TransactionInformation> getHolders() {
        assert synchLock.isHeldByCurrentThread();
        return holders;
    }

    boolean isAHolder(TransactionInformation xid) {
        return holders.contains(xid);
    }

    void reset() {
        exclusive = false;
        upgraded = false;
        numHolders = 0;
        holders.clear();
    }

    void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public File getResource() {
        return resource;
    }

    void markUpgraded() {
        this.upgraded = true;
    }

    boolean isUpgraded() {
        return upgraded;
    }

    public void startSynchBlock() {
        synchLock.lock();
    }

    public void endSynchBlock() {
        synchLock.unlock();
    }

    void waitTillReadable(long time) throws InterruptedException {
        if (time > 0) {
            mayBeReadable.await(time, TimeUnit.MILLISECONDS);
        } else {
            mayBeReadable.await();
        }
    }

    void waitTillWritable(long time) throws InterruptedException {
        if (time > 0) {
            mayBeWritable.await(time, TimeUnit.MILLISECONDS);
        } else {
            mayBeWritable.await();
        }
    }

    void notifyWritable() {
        mayBeWritable.signal();
    }

    void notifyReadWritable() {
        mayBeReadable.signalAll();
        mayBeWritable.signal();
    }
}
