/*
 Copyright © 2010-2014, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class LockTreeNode {

    private final File path;
    private NativeLock lock;
    private final ConcurrentHashMap<String, WeakReference<LockTreeNode>> children =
            new ConcurrentHashMap<String, WeakReference<LockTreeNode>>();
    private final AtomicReference<TransactionInformation> pinHolder = new AtomicReference<TransactionInformation>(null);
    private final LockTreeNode parentNode;//to keep a "strong" ref to all the ancestors to protect them from gc.

    LockTreeNode(File path, boolean withExclusiveLock, LockTreeNode parentNode) {
        this.path = path;
        this.lock = new NativeLock(withExclusiveLock, path, this);
        this.parentNode = parentNode;
    }

    LockTreeNode getChild(String name) {
        WeakReference<LockTreeNode> nodeWR = children.get(name);
        LockTreeNode node;
        if (nodeWR != null) {
            node = nodeWR.get();
            if (node != null) {
                return node;
            }
        }
        node = new LockTreeNode(new File(path, name), false, this);
        WeakReference<LockTreeNode> newNodeWR =
                new WeakReference<LockTreeNode>(node);
        boolean success;
        if (nodeWR == null) {
            success = children.putIfAbsent(name, newNodeWR) == null;
        } else {
            success = children.replace(name, nodeWR, newNodeWR);
            //replace wont work with args null, so needed if-else.
        }
        if (success) {
            return node;
        } else {
            return getChild(name);
        }
    }

    Collection<LockTreeNode> getAllChildren() {
        Collection<WeakReference<LockTreeNode>> childrenRef =
                children.values();
        ArrayList<LockTreeNode> childrenList =
                new ArrayList<LockTreeNode>(childrenRef.size());
        for (WeakReference<LockTreeNode> childRef : childrenRef) {
            LockTreeNode child = childRef.get();
            if (child != null) {
                childrenList.add(child);
            }
        }
        return childrenList;
    }

    boolean isPinnedByOtherTransaction(TransactionInformation thisTransaction) {
        return !(pinHolder.get() == null || pinHolder.get().equals(thisTransaction));
    }

    boolean attemptPinning(TransactionInformation requestor) {
        TransactionInformation holderTransaction = pinHolder.get();
        if (holderTransaction == null) {
            return pinHolder.compareAndSet(null, requestor);
        } else {
            return holderTransaction.equals(requestor);
        }
    }

    void releasePin() {
        pinHolder.set(null);
    }

    NativeLock getLock() {
        return lock;
    }

    File getPath() {
        return path;
    }
}
