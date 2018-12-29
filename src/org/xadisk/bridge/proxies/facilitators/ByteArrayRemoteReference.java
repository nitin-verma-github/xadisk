/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.facilitators;

public class ByteArrayRemoteReference extends OptimizedRemoteReference<byte[]> {

    private static final long serialVersionUID = 1L;
    private transient byte[] originalByteArray;
    private int lengthForUpdate;
    private transient int offsetForUpdate;
    private byte[] resultBytes;

    public ByteArrayRemoteReference(byte[] b, int offset, int length) {
        this.originalByteArray = b;
        this.lengthForUpdate = length;
        this.offsetForUpdate = offset;
    }

    public byte[] regenerateRemoteObject() {
        return new byte[lengthForUpdate];
    }

    public void setResultObject(byte[] b) {
        resultBytes = b;
    }

    public void mergeWithRemoteObject(byte[] resultBytes) {
        if (resultBytes == null) {
            return;//case when -1 is returned.
        } else {
            byte[] result = resultBytes;
            System.arraycopy(result, 0, originalByteArray, offsetForUpdate, result.length);
        }
    }

    public byte[] getResultObject() {
        return resultBytes;
    }
}
