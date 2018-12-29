/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import javax.transaction.xa.Xid;
import org.xadisk.filesystem.ResourceDependencyGraph.Node;

public class TransactionInformation implements Xid, Serializable {

    private static final long serialVersionUID = 1L;
    private final byte[] gid;
    private final byte[] bqual;
    private final int formatId;
    private int numOwnedExclusiveLocks = 0;
    private transient volatile ResourceDependencyGraph.Node nodeInResourceDependencyGraph = null;
    private transient NativeSession owningSession;

    TransactionInformation(ByteBuffer buffer) {
        int gidLength = buffer.get();
        int bqualLength = buffer.get();
        this.formatId = buffer.getInt();
        this.gid = new byte[gidLength];
        this.bqual = new byte[bqualLength];
        buffer.get(gid);
        buffer.get(bqual);
    }

    public TransactionInformation(Xid xid) {
        gid = xid.getGlobalTransactionId();
        bqual = xid.getBranchQualifier();
        formatId = xid.getFormatId();
    }

    public TransactionInformation(byte[] gid, byte[] bqual, int formatId) {
        this.gid = gid;
        this.bqual = bqual;
        this.formatId = formatId;
    }

    public byte[] getBranchQualifier() {
        return bqual;
    }

    public int getFormatId() {
        return formatId;
    }

    public byte[] getGlobalTransactionId() {
        return gid;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof TransactionInformation) {
            TransactionInformation xid = (TransactionInformation) obj;
            if (xid.getFormatId() != formatId) {
                return false;
            }
            byte temp[] = xid.getGlobalTransactionId();
            for (int i = 0; i < temp.length; i++) {
                if (i >= gid.length || temp[i] != gid[i]) {
                    return false;
                }
            }
            temp = xid.getBranchQualifier();
            for (int i = 0; i < temp.length; i++) {
                if (i >= bqual.length || temp[i] != bqual[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        if (gid.length > 0) {
            hashCode += gid[0] + gid[gid.length / 2] + gid[gid.length - 1];
        }
        if (bqual.length > 0) {
            hashCode += bqual[0] + bqual[bqual.length / 2] + bqual[bqual.length - 1];
        }
        return hashCode;
    }

    public Node getNodeInResourceDependencyGraph() {
        return nodeInResourceDependencyGraph;
    }

    public void setNodeInResourceDependencyGraph(Node nodeInResourceDependencyGraph) {
        this.nodeInResourceDependencyGraph = nodeInResourceDependencyGraph;
    }

    public static TransactionInformation getXidInstanceForLocalTransaction(long localTransactionId) {
        ByteBuffer tidBuffer = ByteBuffer.allocate(20);
        tidBuffer.put((byte) 8);
        tidBuffer.put((byte) 0);
        tidBuffer.putInt(101);
        tidBuffer.putLong(localTransactionId);
        tidBuffer.flip();
        return new TransactionInformation(tidBuffer);
    }

    public byte[] getBytes() {
        ByteBuffer temp = ByteBuffer.allocate(1 + 1 + 4 + gid.length + bqual.length);
        temp.put((byte) gid.length);
        temp.put((byte) bqual.length);
        temp.putInt(formatId);
        temp.put(gid);
        temp.put(bqual);
        byte bytes[] = new byte[temp.capacity()];
        temp.flip();
        temp.get(bytes);
        return bytes;
    }

    public NativeSession getOwningSession() {
        return owningSession;
    }

    public void setOwningSession(NativeSession owningSession) {
        this.owningSession = owningSession;
    }

    public int getNumOwnedExclusiveLocks() {
        return numOwnedExclusiveLocks;
    }

    public void incrementNumOwnedExclusiveLocks() {
        this.numOwnedExclusiveLocks++;
    }

    @Override
    public String toString() {
        return "gid : \t" + getHexString(gid) + "\n"
                + "bqual : \t" + getHexString(bqual) + "\n"
                + "formatId : \t" + Integer.toHexString(formatId);
    }

    private String getHexString(byte[] bs) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bs) {
            sb.append(Integer.toHexString(b)).append(" ");
        }
        return sb.toString();
    }
}
