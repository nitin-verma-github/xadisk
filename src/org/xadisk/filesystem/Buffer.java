/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem;

import org.xadisk.filesystem.pools.PooledBuffer;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class Buffer {

    protected ByteBuffer buffer;
    private final boolean hasItsOwnBytes;
    public final boolean isDirect;
    private volatile OnDiskInfo onDiskInfo = null;
    private long fileContentPosition;
    private int fileContentLength;
    private int headerLength;
    private FileChannel logFileChannel;
    private int logChannelIndex = -1;
    private final NativeXAFileSystem xaFileSystem;
    private volatile boolean memorySynchTrigger = true;

    public Buffer(int bufferSize, boolean isDirect, NativeXAFileSystem xaFileSystem) {
        this.xaFileSystem = xaFileSystem;
        if (isDirect) {
            this.buffer = ByteBuffer.allocateDirect(bufferSize);
        } else {
            this.buffer = ByteBuffer.allocate(bufferSize);
        }
        this.hasItsOwnBytes = true;
        this.isDirect = isDirect;
        if (!(this instanceof PooledBuffer)) {
            xaFileSystem.changeTotalNonPooledBufferSize(bufferSize);
        }
    }

    public Buffer(ByteBuffer buffer, NativeXAFileSystem xaFileSystem) {
        this.xaFileSystem = xaFileSystem;
        this.buffer = buffer;
        this.isDirect = false;
        this.hasItsOwnBytes = true;
        if (!(this instanceof PooledBuffer)) {
            xaFileSystem.changeTotalNonPooledBufferSize(buffer.capacity());
        }
    }

    public Buffer(NativeXAFileSystem xaFileSystem) {
        this.xaFileSystem = xaFileSystem;
        this.hasItsOwnBytes = false;
        this.isDirect = false;
    }

    public void flushByteBufferChanges() {
        memorySynchTrigger = true;
    }

    public void invalidateByteBufferFromCache() {
        boolean dummyRead = memorySynchTrigger;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    boolean isDirect() {
        return isDirect;
    }

    public void makeOnDisk(OnDiskInfo onDiskInfo) {
        if (onDiskInfo == null) {
            return;
        }
        this.onDiskInfo = onDiskInfo;
        if (!(this instanceof PooledBuffer)) {
            if (hasItsOwnBytes) {
                xaFileSystem.changeTotalNonPooledBufferSize(-buffer.capacity());
            }
            buffer = null;
        }
    }

    public void setOnDiskInfo(OnDiskInfo onDiskInfo) {
        this.onDiskInfo = onDiskInfo;
    }

    @Override
    protected void finalize() throws Throwable {
        if (!(this instanceof PooledBuffer)) {
            if (buffer == null) {
            } else {
                if (hasItsOwnBytes) {
                    xaFileSystem.changeTotalNonPooledBufferSize(-buffer.capacity());
                }
            }
        }
    }

    public OnDiskInfo getOnDiskInfo() {
        return onDiskInfo;
    }

    public int getFileContentLength() {
        return fileContentLength;
    }

    public void setFileContentLength(int fileContentLength) {
        this.fileContentLength = fileContentLength;
    }

    public long getFileContentPosition() {
        return fileContentPosition;
    }

    public void setFileContentPosition(long fileContentPosition) {
        this.fileContentPosition = fileContentPosition;
    }

    public int getHeaderLength() {
        return headerLength;
    }

    public void setHeaderLength(int headerLength) {
        this.headerLength = headerLength;
    }

    public Buffer createReadOnlyClone() {
        Buffer clone = new Buffer(this.xaFileSystem);
        ByteBuffer referenceToByteBuffer = this.buffer;
        if (referenceToByteBuffer == null) {
            clone.onDiskInfo = this.onDiskInfo;
        } else {
            clone.buffer = referenceToByteBuffer.asReadOnlyBuffer();
        }
        clone.setFileContentLength(fileContentLength);
        clone.setFileContentPosition(fileContentPosition);
        clone.setHeaderLength(headerLength);
        return clone;
    }

    public int regenerateContentFromDisk(ByteBuffer target, int offsetToReadFrom) throws IOException {
        int logIndex = onDiskInfo.getLogIndex();
        if (logChannelIndex != logIndex) {
            if (logFileChannel != null) {
                logFileChannel.close();
            }
            FileInputStream logIS = new FileInputStream(xaFileSystem.getTransactionLogFileBaseName() + "_" + logIndex);
            logFileChannel = logIS.getChannel();
            logChannelIndex = logIndex;
        }
        logFileChannel.position(onDiskInfo.getLocation() + headerLength + offsetToReadFrom);
        target.limit(fileContentLength - offsetToReadFrom);
        int numRead = 0;
        while (numRead == 0) {
            numRead = logFileChannel.read(target);
        }
        target.flip();
        return numRead;
    }
}
