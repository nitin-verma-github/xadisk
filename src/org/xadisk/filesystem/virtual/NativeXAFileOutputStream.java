/*
 Copyright © 2010-2014, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.virtual;

import org.xadisk.filesystem.workers.GatheringDiskWriter;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.filesystem.Buffer;
import org.xadisk.filesystem.NativeSession;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.TransactionLogEntry;
import org.xadisk.filesystem.TransactionInformation;
import org.xadisk.filesystem.exceptions.ClosedStreamException;
import org.xadisk.filesystem.exceptions.FileUnderUseException;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;

public class NativeXAFileOutputStream implements XAFileOutputStream {

    private final String destination;
    private ByteBuffer byteBuffer;
    private Buffer buffer;
    private final NativeXAFileSystem xaFileSystem;
    private final TransactionInformation xid;
    private final GatheringDiskWriter theGatheringDiskWriter;
    private long filePosition;
    private boolean closed = false;
    private final VirtualViewFile vvf;
    private final boolean heavyWrite;
    private final NativeSession owningSession;
    private final ReentrantLock asynchronousRollbackLock;

    public NativeXAFileOutputStream(VirtualViewFile vvf, TransactionInformation xid, boolean heavyWrite,
            NativeSession owningSession, NativeXAFileSystem xaFileSystem) {
        this.xaFileSystem = xaFileSystem;
        this.destination = vvf.getFileName().getAbsolutePath();
        this.xid = xid;
        this.theGatheringDiskWriter = this.xaFileSystem.getTheGatheringDiskWriter();
        this.vvf = vvf;
        this.filePosition = vvf.getLength();
        vvf.setBeingWritten(true);
        if (heavyWrite) {
            if (!vvf.isUsingHeavyWriteOptimization()) {
                try {
                    vvf.setUpForHeavyWriteOptimization();
                } catch (IOException ioe) {
                    xaFileSystem.notifySystemFailure(ioe);
                }
            }
        }
        this.heavyWrite = vvf.isUsingHeavyWriteOptimization();
        allocateByteBuffer();
        setUpNewBuffer();
        this.owningSession = owningSession;
        this.asynchronousRollbackLock = owningSession.getAsynchronousRollbackLock();
    }

    public void write(byte[] b) throws ClosedStreamException, NoTransactionAssociatedException {
        write(b, 0, b.length);
    }

    public void write(int b) throws ClosedStreamException, NoTransactionAssociatedException {
        byte b1[] = {(byte) b};
        write(b1, 0, 1);
    }

    public void write(byte[] b, int off, int len) throws ClosedStreamException, NoTransactionAssociatedException {
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            while (len > 0) {
                int lenToWriteNow = Math.min(byteBuffer.remaining(), len);
                byteBuffer.put(b, off, lenToWriteNow);
                filePosition += lenToWriteNow;
                if (byteBuffer.remaining() == 0) {
                    submitBuffer();
                    setUpNewBuffer();
                }
                off += lenToWriteNow;
                len -= lenToWriteNow;
            }
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    public void flush() throws ClosedStreamException, NoTransactionAssociatedException {
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            submitBuffer();
            setUpNewBuffer();
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    public void close() throws NoTransactionAssociatedException {
        if (closed) {
            return;
        }
        try {
            asynchronousRollbackLock.lock();
            owningSession.checkIfCanContinue();
            submitBuffer();
            vvf.setBeingWritten(false);
            closed = true;
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    private void allocateByteBuffer() {
        buffer = xaFileSystem.getBufferPool().checkOut();
        if (buffer != null) {
            this.byteBuffer = buffer.getBuffer();
        } else {
            this.buffer = new Buffer(xaFileSystem.getConfiguredBufferSize(), false, xaFileSystem);
            this.byteBuffer = buffer.getBuffer();
        }
        this.byteBuffer.clear();
    }

    private void setUpNewBuffer() {
        if (heavyWrite) {
            this.byteBuffer.clear();
        } else {
            allocateByteBuffer();
            byte temp[] = TransactionLogEntry.getLogEntry(xid, destination, filePosition, 21, TransactionLogEntry.FILE_APPEND);
            byteBuffer.put(temp);
            buffer.setFileContentPosition(filePosition);
            buffer.setHeaderLength(temp.length);
        }
    }

    private void submitBuffer() {
        try {
            if (heavyWrite) {
                byteBuffer.flip();
                vvf.appendContentBuffer(buffer);
            } else {
                int contentLength = byteBuffer.position() - buffer.getHeaderLength();
                TransactionLogEntry.updateContentLength(byteBuffer, contentLength);
                buffer.setFileContentLength(contentLength);
                byteBuffer.flip();
                vvf.appendContentBuffer(buffer);
                theGatheringDiskWriter.submitBuffer(buffer, xid);
            }
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailure(ioe);
        }
    }

    public File getDestinationFile() {
        return new File(destination);
    }

    private void checkIfCanContinue() throws NoTransactionAssociatedException, ClosedStreamException {
        owningSession.checkIfCanContinue();
    }

    public boolean isClosed() {
        return closed;
    }
}
