/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.workers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;
import org.xadisk.bridge.proxies.impl.RemoteTransactionInformation;
import org.xadisk.filesystem.NativeConcurrencyControl;
import org.xadisk.filesystem.NativeLock;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.ResourceDependencyGraph;
import org.xadisk.filesystem.TransactionInformation;

public class DeadLockDetector extends TimedWorker {

    private final NativeXAFileSystem nativeXAFileSystem;
    private final NativeConcurrencyControl nativeConcurrencyControl;
    private final ResourceDependencyGraph rdg;
    private ResourceDependencyGraph.Node[] nodes = new ResourceDependencyGraph.Node[0];
    private final ArrayList<ResourceDependencyGraph.Node> backEdges = new ArrayList<ResourceDependencyGraph.Node>(10);

    public DeadLockDetector(int frequency, ResourceDependencyGraph rdg, NativeXAFileSystem nativeXAFileSystem,
            NativeConcurrencyControl nativeConcurrencyControl) {
        super(frequency);
        this.rdg = rdg;
        this.nativeXAFileSystem = nativeXAFileSystem;
        this.nativeConcurrencyControl = nativeConcurrencyControl;
    }

    @Override
    void doWorkOnce() {
        try {
            while (true) {
                cleanup();
                takeSnapShotOfRDG();
                runDFS();
                if (backEdges.size() == 0) {
                    break;
                }
                breakCycles();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        } catch (Throwable t) {
            nativeXAFileSystem.notifySystemFailure(t);
        }
    }

    private void takeSnapShotOfRDG() {
        nodes = rdg.getNodes();
        for (int i = 0; i < nodes.length; i++) {
            ResourceDependencyGraph.Node node = nodes[i];
            NativeLock resource = node.getResourceWaitingFor();
            if (resource == null) {
                continue;
            }
            TransactionInformation holders[];
            try {
                resource.startSynchBlock();
                HashSet<TransactionInformation> holdersSet = resource.getHolders();
                holders = holdersSet.toArray(new TransactionInformation[0]);
            } finally {
                resource.endSynchBlock();
            }
            for (int j = 0; j < holders.length; j++) {
                ResourceDependencyGraph.Node neighbor;
                if (holders[j] instanceof RemoteTransactionInformation) {
                    neighbor = rdg.getNode(holders[j]);
                } else {
                    neighbor = holders[j].getNodeInResourceDependencyGraph();
                }
                if (neighbor != null && neighbor != node) {
                    node.addNeighbor(neighbor);
                }
            }
        }
    }

    private void runDFS() {
        int clock = 0;
        Stack<ResourceDependencyGraph.Node> nodesBeingExplored = new Stack<ResourceDependencyGraph.Node>();
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].setMark(1);
        }

        for (int i = 0; i < nodes.length; i++) {
            ResourceDependencyGraph.Node nodeBeingExplored = nodes[i];
            if (nodeBeingExplored.getMark() == 2) {
                continue;
            }
            nodesBeingExplored.push(nodeBeingExplored);
            while (!nodesBeingExplored.empty()) {
                nodeBeingExplored = (ResourceDependencyGraph.Node) nodesBeingExplored.peek();
                if (nodeBeingExplored.getMark() != 2) {
                    nodeBeingExplored.setMark(2);
                    nodeBeingExplored.setPreVisit(clock++);
                }
                ArrayList<ResourceDependencyGraph.Node> neighbors = nodeBeingExplored.getNeighbors();
                boolean exploredCompletely = true;
                for (int j = nodeBeingExplored.getNextNeighborToProcess(); j < neighbors.size(); j++) {
                    nodeBeingExplored.forwardNextNeighborToProcess();
                    ResourceDependencyGraph.Node nextToExplore = neighbors.get(j);
                    if (nextToExplore.getMark() == 2) {
                        detectBackEdge(nodeBeingExplored, nextToExplore);
                        continue;
                    }
                    nodesBeingExplored.push(nextToExplore);
                    nextToExplore.setParent(nodeBeingExplored);
                    exploredCompletely = false;
                    break;
                }
                if (exploredCompletely) {
                    nodesBeingExplored.pop();
                    nodeBeingExplored.setPostVisit(clock++);
                }
            }
        }
    }

    private void detectBackEdge(ResourceDependencyGraph.Node source, ResourceDependencyGraph.Node target) {
        if (target.getPrepostVisit()[0] < source.getPrepostVisit()[0]
                && target.getPrepostVisit()[1] == 0) {
            backEdges.add(source);
            backEdges.add(target);
            return;
        }
    }

    private void breakCycles() {
        for (int i = 0; i < backEdges.size() - 1; i += 2) {
            ResourceDependencyGraph.Node source = backEdges.get(i);
            ResourceDependencyGraph.Node target = backEdges.get(i + 1);
            ResourceDependencyGraph.Node victim = null;
            int minimumLocks = -1;
            while (source != target) {
                if (!source.isWaitingForResource()) {
                    break;
                }
                int currentLocksCount = source.getId().getNumOwnedExclusiveLocks();
                if (minimumLocks == -1) {
                    victim = source;
                    minimumLocks = currentLocksCount;
                } else if (currentLocksCount < minimumLocks) {
                    minimumLocks = currentLocksCount;
                    victim = source;
                }
                source = source.getParent();
            }

            if (victim != null && victim.isWaitingForResource()) {
                TransactionInformation victimXid = (TransactionInformation) victim.getId();
                nativeConcurrencyControl.interruptTransactionIfWaitingForResourceLock(victimXid,
                        ResourceDependencyGraph.Node.INTERRUPTED_DUE_TO_DEADLOCK);
            }
        }
    }

    private void cleanup() {
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].resetAlgorithmicData();
        }
        backEdges.clear();
    }

    @Override
    public void release() {
        super.release();
    }

    @Override
    public void run() {
        super.run();
    }
}
