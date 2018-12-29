/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.pools;

import java.nio.ByteBuffer;
import org.xadisk.filesystem.Buffer;
import org.xadisk.filesystem.NativeXAFileSystem;

public class PooledBuffer extends Buffer implements PooledResource {

    private volatile long lastFreed = -1;

    PooledBuffer(int bufferSize, boolean isDirect, NativeXAFileSystem xaFileSystem) {
        super(bufferSize, isDirect, xaFileSystem);
    }

    public void markFree() {
        buffer.clear();
        lastFreed = System.currentTimeMillis() / 1000;
    }

    @Override
    public ByteBuffer getBuffer() {
        return buffer;
    }

    public long getLastFreed() {
        return lastFreed;
    }
}
