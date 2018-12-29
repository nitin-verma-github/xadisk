/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceDependencyGraph {

    private final ConcurrentHashMap<TransactionInformation, Node> nodes = new ConcurrentHashMap<TransactionInformation, Node>(1000);

    ResourceDependencyGraph() {
    }

    void addDependency(TransactionInformation dependent, NativeLock resource) {
        Node node = new Node(dependent, 0, resource, Thread.currentThread());
        nodes.put(dependent, node);
        dependent.setNodeInResourceDependencyGraph(node);
    }

    void removeDependency(TransactionInformation dependent) {
        Node source = dependent.getNodeInResourceDependencyGraph();
        source.setResourceWaitingFor(null);
        nodes.remove(dependent);
        dependent.setNodeInResourceDependencyGraph(null);
    }

    public Node[] getNodes() {
        return nodes.values().toArray(new Node[0]);
    }

    public Node getNode(TransactionInformation dependent) {
        return nodes.get(dependent);
    }

    /*public Node generateNodeForTesting() {
     Node n = new Node(nodes.size(), 0);
     nodes.put(XidImpl.getXidInstanceForLocalTransaction(nodes.size()), n);
     return n;
     }*/
    public static class Node {

        public static final byte INTERRUPTED_DUE_TO_DEADLOCK = 1;
        public static final byte INTERRUPTED_DUE_TO_TIMEOUT = 2;
        private final TransactionInformation id;
        private final ArrayList<Node> neighbors = new ArrayList<Node>(10);
        private final Thread threadWaitingForLock;
        private volatile byte interruptCause = 0;
        private final Object interruptFlagLock = new ArrayList<Object>(0);//making it transient means it would be seen as null
        //in the remote xadisk, and was giving NPE. We also made this String as Object is not serializable. Doesn't
        //matter what is the actual Object anyway.
        private int mark;
        private int prepostVisit[] = new int[2];
        private Node parent = null;
        private int nextNeighborToProcess = 0;
        private volatile NativeLock resourceWaitingFor;

        private Node(TransactionInformation id, int defaultMark, NativeLock resourceWaitingFor, Thread threadWaitingForLock) {
            this.id = id;
            this.mark = defaultMark;
            this.resourceWaitingFor = resourceWaitingFor;
            this.threadWaitingForLock = threadWaitingForLock;
        }

        public void addNeighbor(Node n) {
            neighbors.add(n);
        }

        public ArrayList<Node> getNeighbors() {
            return neighbors;
        }

        public void setMark(int mark) {
            this.mark = mark;
        }

        public int getMark() {
            return mark;
        }

        public void setPreVisit(int preVisit) {
            prepostVisit[0] = preVisit;
        }

        public void setPostVisit(int postVisit) {
            prepostVisit[1] = postVisit;
        }

        public int[] getPrepostVisit() {
            return prepostVisit;
        }

        public Node getParent() {
            return parent;
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        public TransactionInformation getId() {
            return id;
        }

        public void resetAlgorithmicData() {
            mark = 0;
            parent = null;
            prepostVisit[0] = 0;
            prepostVisit[1] = 0;
            nextNeighborToProcess = 0;
            neighbors.clear();
        }

        public boolean isWaitingForResource() {
            return resourceWaitingFor != null;
        }

        public int getNextNeighborToProcess() {
            return nextNeighborToProcess;
        }

        public void forwardNextNeighborToProcess() {
            nextNeighborToProcess++;
        }

        void setResourceWaitingFor(NativeLock resourceWaitingFor) {
            this.resourceWaitingFor = resourceWaitingFor;
        }

        public NativeLock getResourceWaitingFor() {
            return resourceWaitingFor;
        }

        public Thread getThreadWaitingForLock() {
            return threadWaitingForLock;
        }

        public byte getInterruptCause() {
            return interruptCause;
        }

        public void setInterruptCause(byte interruptCause) {
            this.interruptCause = interruptCause;
        }

        public Object getInterruptFlagLock() {
            return interruptFlagLock;
        }
    }
}
