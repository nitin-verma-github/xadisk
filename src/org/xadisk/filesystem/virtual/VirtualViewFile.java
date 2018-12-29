/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.virtual;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import org.xadisk.filesystem.Buffer;
import org.xadisk.filesystem.DurableDiskSession;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.OnDiskInfo;
import org.xadisk.filesystem.TransactionLogEntry;
import org.xadisk.filesystem.TransactionInformation;

public class VirtualViewFile {

    private File fileName;
    private long length;
    private boolean beingWritten = false;
    private int beingReadBy = 0;
    private long mappedToThePhysicalFileTill = -1;
    private File mappedToPhysicalFile;
    private final long originalPhysicalFileSize;
    private long smallestTruncationPointInOriginalFile = -1;
    private boolean usingHeavyWriteOptimization = false;
    private FileChannel fileViewChannel;
    private RandomAccessFile fileViewStream;
    private final ArrayList<Buffer> virtualViewContentBuffers = new ArrayList<Buffer>(10);
    private final NativeXAFileSystem xaFileSystem;
    private final ArrayList<VirtualViewFile> fileCopies = new ArrayList<VirtualViewFile>(1);
    private final TransactionInformation xid;
    private final TransactionVirtualView transactionView;
    private boolean createdPhysicalFileInBackupDir = false;
    private File physicalFileNameInBackupDir = null;
    private boolean hasBeenDeleted = false;
    private final DurableDiskSession diskSession;

    VirtualViewFile(File fileName, long length, TransactionVirtualView transactionView,
            NativeXAFileSystem xaFileSystem, DurableDiskSession diskSession) {
        this.fileName = fileName;
        this.length = length;
        this.transactionView = transactionView;
        this.xid = this.transactionView.getOwningTransaction();
        this.xaFileSystem = xaFileSystem;
        this.originalPhysicalFileSize = -1;
        this.diskSession = diskSession;
    }

    VirtualViewFile(File fileName, long length, TransactionVirtualView transactionView,
            File mappedToPhysical, long mappedToThePhysicalFileTill, NativeXAFileSystem xaFileSystem,
            DurableDiskSession diskSession) {
        this.fileName = fileName;
        this.length = length;
        this.transactionView = transactionView;
        this.xid = this.transactionView.getOwningTransaction();
        this.xaFileSystem = xaFileSystem;
        this.originalPhysicalFileSize = mappedToThePhysicalFileTill;
        this.mappedToThePhysicalFileTill = mappedToThePhysicalFileTill;
        this.smallestTruncationPointInOriginalFile = mappedToThePhysicalFileTill;
        this.diskSession = diskSession;
    }

    public long getLength() {
        return length;
    }

    void setLength(long length) {
        this.length = length;
    }

    public File getFileName() {
        return fileName;
    }

    boolean isBeingWritten() {
        return beingWritten;
    }

    void setBeingWritten(boolean beingWritten) {
        this.beingWritten = beingWritten;
    }

    boolean isBeingRead() {
        return beingReadBy > 0;
    }

    void addBeingRead() {
        beingReadBy++;
    }

    void reduceBeingRead() {
        beingReadBy--;
    }

    long getMappedToThePhysicalFileTill() {
        return mappedToThePhysicalFileTill;
    }

    void setMappedToThePhysicalFileTill(long mappedToThePhysicalFileTill) {
        this.mappedToThePhysicalFileTill = mappedToThePhysicalFileTill;
    }

    boolean isMappedToAPhysicalFile() {
        return this.mappedToThePhysicalFileTill != -1;
    }

    File getMappedToPhysicalFile() {
        return mappedToPhysicalFile;
    }

    void setMappedToPhysicalFile(File mappedToPhysicalFile) {
        this.mappedToPhysicalFile = mappedToPhysicalFile;
    }

    public boolean isUsingHeavyWriteOptimization() {
        return usingHeavyWriteOptimization;
    }

    private void safeSetupForPhysicalFileExistence() throws IOException {
        if (fileName.exists()) {
            if (!isMappedToAPhysicalFile()) {
                physicalFileNameInBackupDir = getBackupFileName();
                diskSession.createFile(physicalFileNameInBackupDir);
                createdPhysicalFileInBackupDir = true;
                transactionView.hasCreatedFileInBackDir(this);
                submitRedoLogForMove(physicalFileNameInBackupDir, fileName);
            } else {
                safePhysicalAppend();
            }
        } else {
            physicalFileNameInBackupDir = getBackupFileName();
            diskSession.createFile(physicalFileNameInBackupDir);
            createdPhysicalFileInBackupDir = true;
            transactionView.hasCreatedFileInBackDir(this);
            submitRedoLogForMove(physicalFileNameInBackupDir, fileName);
        }
    }

    void setUpForHeavyWriteOptimization() throws IOException {
        if (usingHeavyWriteOptimization) {
            return;
        }
        transactionView.beingUsedInHeavyWriteMode(this);
        safeSetupForPhysicalFileExistence();
        if (createdPhysicalFileInBackupDir) {
            fileViewStream = new RandomAccessFile(physicalFileNameInBackupDir, "rw");
            fileViewChannel = fileViewStream.getChannel();
        } else {
            fileViewStream = new RandomAccessFile(fileName, "rw");
            fileViewChannel = fileViewStream.getChannel();
        }
        usingHeavyWriteOptimization = true;
        safePhysicalTruncate(mappedToThePhysicalFileTill);
        ByteBuffer temp;
        HashMap<Integer, FileChannel> logChannels = new HashMap<Integer, FileChannel>(2);
        long positionOfAppending = fileViewChannel.size();
        for (Buffer vvCB : virtualViewContentBuffers) {
            Buffer srcClone = vvCB.createReadOnlyClone();
            temp = srcClone.getBuffer();
            if (temp == null) {
                OnDiskInfo onDiskInfo = srcClone.getOnDiskInfo();
                int logIndex = onDiskInfo.getLogIndex();
                FileChannel logFileChannel = logChannels.get(logIndex);
                if (logFileChannel == null) {
                    logFileChannel = new FileInputStream(xaFileSystem.getTransactionLogFileBaseName() + "_"
                            + srcClone.getOnDiskInfo().getLogIndex()).getChannel();
                    logChannels.put(logIndex, logFileChannel);
                }
                logFileChannel.position(onDiskInfo.getLocation() + srcClone.getHeaderLength());
                fileViewChannel.transferFrom(logFileChannel, srcClone.getFileContentPosition(),
                        NativeXAFileSystem.maxTransferToChannel(srcClone.getFileContentLength()));
            } else {
                temp.position(srcClone.getHeaderLength());
                int sizeToWrite = temp.remaining();
                int num = 0;
                while (num < sizeToWrite) {
                    num += fileViewChannel.write(temp);
                }
            }
            positionOfAppending += srcClone.getFileContentLength();
            fileViewChannel.position(positionOfAppending);
        }
        virtualViewContentBuffers.clear();
        mappedToThePhysicalFileTill = -1;
    }

    void updatePhysicalContents(Buffer originalContents, long filePosition) {
        if (mappedToThePhysicalFileTill > filePosition) {
            mappedToThePhysicalFileTill = filePosition;
            virtualViewContentBuffers.add(0, originalContents);
        }
    }

    private void safePhysicalAppend() throws IOException {
        if (!fileName.exists()) {
            return;
        }
        ByteBuffer logEntryHeader = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, fileName.getAbsolutePath(),
                originalPhysicalFileSize, TransactionLogEntry.UNDOABLE_FILE_APPEND));
        xaFileSystem.getTheGatheringDiskWriter().forceUndoLogAndData(xid, logEntryHeader, null, -1, -1);
    }

    private void safePhysicalTruncate(long newLength) throws IOException {
        if (!usingHeavyWriteOptimization) {
            return;
        }
        if (createdPhysicalFileInBackupDir) {
            if (newLength >= 0) {
                fileViewChannel.truncate(newLength);
            }
            return;
        }
        int lengthOfContentToBackUp = (int) (smallestTruncationPointInOriginalFile - newLength);
        if (lengthOfContentToBackUp > 0) {
            smallestTruncationPointInOriginalFile = newLength;
            ByteBuffer logEntryHeader = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, fileName.getAbsolutePath(),
                    newLength, lengthOfContentToBackUp,
                    TransactionLogEntry.UNDOABLE_FILE_TRUNCATE));
            long logInfo[] = xaFileSystem.getTheGatheringDiskWriter().forceUndoLogAndData(xid, logEntryHeader, fileViewChannel,
                    newLength, lengthOfContentToBackUp);
            OnDiskInfo truncatedContentsFromLogs = new OnDiskInfo((int) logInfo[0], logInfo[1]);
            Buffer buffer = new Buffer(xaFileSystem);
            buffer.setFileContentPosition(newLength);
            buffer.setFileContentLength(lengthOfContentToBackUp);
            buffer.setHeaderLength(logInfo.length);
            buffer.makeOnDisk(truncatedContentsFromLogs);
            for (VirtualViewFile fileCopy : fileCopies) {
                fileCopy.updatePhysicalContents(buffer, newLength);
            }
        }
        fileViewChannel.truncate(newLength);
    }

    private void takeSnapshotFromPhysicalSource(FileChannel sourceChannel) throws IOException {
        safeSetupForPhysicalFileExistence();
        if (createdPhysicalFileInBackupDir) {
            fileViewStream = new RandomAccessFile(physicalFileNameInBackupDir, "rw");
            fileViewChannel = fileViewStream.getChannel();
        } else {
            fileViewStream = new RandomAccessFile(fileName, "rw");
            fileViewChannel = fileViewStream.getChannel();
        }
        mappedToThePhysicalFileTill = -1;
        virtualViewContentBuffers.clear();
        long fileLength = sourceChannel.size();
        long n = 0;
        long sourceOriginalPosition = sourceChannel.position();
        sourceChannel.position(0);
        while (n < fileLength) {
            n += fileViewChannel.transferFrom(sourceChannel, n, NativeXAFileSystem.maxTransferToChannel(fileLength - n));
        }
        sourceChannel.position(sourceOriginalPosition);
        fileViewChannel.position(0);
        length = fileViewChannel.size();
    }

    public void takeSnapshotInto(VirtualViewFile target) {
        try {
            target.usingHeavyWriteOptimization = usingHeavyWriteOptimization;
            if (this.usingHeavyWriteOptimization) {
                target.takeSnapshotFromPhysicalSource(fileViewChannel);
                transactionView.beingUsedInHeavyWriteMode(target);
            } else {
                if (!fileCopies.contains(target)) {
                    fileCopies.add(target);
                }
                target.setMappedToPhysicalFile(this.mappedToPhysicalFile);
                target.setMappedToThePhysicalFileTill(this.mappedToThePhysicalFileTill);
                for (Buffer vvCB : virtualViewContentBuffers) {
                    target.appendContentBuffer(vvCB);
                }
            }
            target.setLength(this.getLength());
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailure(ioe);
        }
    }

    public void truncate(long newLength) {
        if (newLength < 0 || newLength > this.length) {
            throw new IllegalArgumentException("New length should not be negative or more than file size.");
        }
        this.length = newLength;
        if (usingHeavyWriteOptimization) {
            try {
                safePhysicalTruncate(newLength);
            } catch (IOException ioe) {
                xaFileSystem.notifySystemFailure(ioe);
            }
            return;
        }
        if (this.mappedToThePhysicalFileTill > newLength) {
            this.mappedToThePhysicalFileTill = newLength;
        }
        boolean needToTruncatePartially = false;
        int removeCompleteBuffersFromIndex = -1;
        for (int i = 0; i < virtualViewContentBuffers.size(); i++) {
            Buffer buffer = virtualViewContentBuffers.get(i);
            if (newLength <= buffer.getFileContentPosition()) {
                removeCompleteBuffersFromIndex = i;
                break;
            }
            long fileLengthUptoThisBuffer = buffer.getFileContentPosition() + buffer.getFileContentLength();
            if (newLength < fileLengthUptoThisBuffer) {
                needToTruncatePartially = true;
                removeCompleteBuffersFromIndex = i + 1;
                break;
            }
        }
        if (needToTruncatePartially) {
            Buffer partiallyTruncatedBuffer = virtualViewContentBuffers.get(removeCompleteBuffersFromIndex - 1);
            Buffer virtualCopy = partiallyTruncatedBuffer.createReadOnlyClone();
            int effectiveContentLengthInBuffer = (int) (newLength - virtualCopy.getFileContentPosition());
            virtualCopy.setFileContentLength(effectiveContentLengthInBuffer);
            virtualViewContentBuffers.set(removeCompleteBuffersFromIndex - 1, virtualCopy);
        }
        if (virtualViewContentBuffers.size() > 0) {
            for (int j = virtualViewContentBuffers.size() - 1; j >= removeCompleteBuffersFromIndex; j--) {
                virtualViewContentBuffers.remove(j);
            }
        }
    }

    int fillUpContentsFromChannel(ByteBuffer buffer, long filePosition) throws IOException {
        long position = fileViewChannel.position();
        fileViewChannel.position(filePosition);
        int n = fileViewChannel.read(buffer);
        fileViewChannel.position(position);
        return n;
    }

    Buffer getInMemoryContentBuffer(long position) {
        for (Buffer buffer : virtualViewContentBuffers) {
            long beginIndex = buffer.getFileContentPosition();
            long endIndex = beginIndex + buffer.getFileContentLength() - 1;
            if (beginIndex <= position && endIndex >= position) {
                return buffer;
            }
        }
        return null;
    }

    void appendContentBuffer(Buffer buffer) throws IOException {
        if (usingHeavyWriteOptimization) {
            fileViewChannel.position(length);
            ByteBuffer content = buffer.getBuffer();
            content.position(0);
            int num = 0;
            while (num < content.limit()) {
                num += fileViewChannel.write(content);
            }
            length = fileViewChannel.position();
        } else {
            virtualViewContentBuffers.add(buffer);
            length += buffer.getFileContentLength();
        }
    }

    private void submitRedoLogForMove(File sourceFile, File destFile) {
        ByteBuffer logEntryHeader = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, sourceFile.getAbsolutePath(),
                destFile.getAbsolutePath(),
                TransactionLogEntry.FILE_SPECIAL_MOVE));
        xaFileSystem.getTheGatheringDiskWriter().submitBuffer(new Buffer(logEntryHeader, xaFileSystem), xid);
    }

    public void forceAndFreePhysicalChannel() {
        try {
            if (usingHeavyWriteOptimization && !hasBeenDeleted) {
                fileViewChannel.force(true);
                fileViewStream.close();
            }
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailure(ioe);
        }
    }

    public void freePhysicalChannel() {
        try {
            if (usingHeavyWriteOptimization && !hasBeenDeleted) {
                fileViewStream.close();
            }
        } catch (IOException ioe) {
            //though, it is rollback, but such failures need to be notified and are signs of bug instead.
            xaFileSystem.notifySystemFailure(ioe);
        }
    }

    public void cleanupBackup() {
        try {
            if (createdPhysicalFileInBackupDir) {
                fileViewStream.close();
                diskSession.deleteFile(physicalFileNameInBackupDir);
            }
        } catch (IOException ioe) {
            //no-op.
        }
    }

    void propagatedDeleteCall() {
        if (usingHeavyWriteOptimization) {
            try {
                if (createdPhysicalFileInBackupDir) {
                    submitRedoLogForMove(fileName, physicalFileNameInBackupDir);
                } else {
                    submitRedoLogForMove(fileName, getBackupFileName());
                }
                fileViewStream.close();
            } catch (IOException ioe) {
                xaFileSystem.notifySystemFailure(ioe);
            }
            hasBeenDeleted = true;
        }
    }

    void propagatedMoveCall(File targetFileName) {
        if (usingHeavyWriteOptimization) {
            submitRedoLogForMove(fileName, targetFileName);
        }
        this.fileName = targetFileName;
    }

    void propagatedAncestorMoveCall(File targetFileName) {
        this.fileName = targetFileName;
    }

    void markDeleted() {
        this.hasBeenDeleted = true;
    }

    private File getBackupFileName() throws IOException {
        File backFile = xaFileSystem.getNextBackupFileName();
        return backFile;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VirtualViewFile) {
            VirtualViewFile objVVF = (VirtualViewFile) obj;
            return objVVF.fileName.equals(this.fileName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return fileName.hashCode();
    }

    @Override
    protected void finalize() throws Throwable {
        if (usingHeavyWriteOptimization) {
            try {
                //TODO - remove this check. it is for debugging file deletion/renaming failure in rare cases.
                if (fileViewChannel.isOpen()) {
                    //throw new IOException("The File Channel was left open.");
                    fileViewStream.close();
                }
            } catch (IOException ioe) {
                xaFileSystem.notifySystemFailure(ioe);
            }
        }
    }
}
