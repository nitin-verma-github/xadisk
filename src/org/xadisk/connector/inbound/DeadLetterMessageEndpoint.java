/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.connector.inbound;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import org.xadisk.filesystem.DurableDiskSession;
import org.xadisk.filesystem.FileSystemStateChangeEvent;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.TransactionLogEntry;
import org.xadisk.filesystem.TransactionInformation;

public class DeadLetterMessageEndpoint {

    private final File deadLetterDir;
    private FileChannel deadLetterChannel;
    private int currentLetterIndex;
    private final NativeXAFileSystem xaFileSystem;

    public DeadLetterMessageEndpoint(File deadLetterDir, NativeXAFileSystem xaFileSystem) throws IOException {
        this.xaFileSystem = xaFileSystem;
        this.deadLetterDir = deadLetterDir;
        File existingLetters[] = deadLetterDir.listFiles();
        DurableDiskSession diskSession = xaFileSystem.createDurableDiskSession();
        currentLetterIndex = 0;
        for (File existingLetter : existingLetters) {
            if (existingLetter.length() == 0) {
                diskSession.deleteFile(existingLetter);
            } else {
                int letterIndex = Integer.valueOf(existingLetter.getName().substring(7));
                if (currentLetterIndex <= letterIndex) {
                    currentLetterIndex = letterIndex + 1;
                }
            }
        }
        this.deadLetterChannel = new FileOutputStream(new File(deadLetterDir, "letter_" + currentLetterIndex)).getChannel();
    }

    public void dumpAndCommitMessage(FileSystemStateChangeEvent event, XAResource xar) throws IOException,
            XAException {
        synchronized (this) {
            ensureDeadLetterCapacity();
            byte[] content = event.toString().getBytes(TransactionLogEntry.UTF8Charset);
            deadLetterChannel.write(ByteBuffer.wrap(content));
        }
        xar.commit(TransactionInformation.getXidInstanceForLocalTransaction(xaFileSystem.getNextLocalTransactionId()), true);
    }

    private void ensureDeadLetterCapacity() throws IOException {
        if (deadLetterChannel.size() > 10000) {
            File nextLetter = null;
            for (int i = currentLetterIndex + 1; i < Integer.MAX_VALUE; i++) {
                nextLetter = new File(deadLetterDir, "letter_" + i);
                if (nextLetter.exists()) {
                    continue;
                }
                deadLetterChannel.close();
                deadLetterChannel =
                        new FileOutputStream(nextLetter).getChannel();
                currentLetterIndex = i;
                break;
            }
            if (nextLetter == null) {
                throw new IOException("No more dead letters can be created...cannot proceed.");
            }
        }
    }

    public void release() {
        try {
            deadLetterChannel.close();
        } catch (IOException ioe) {
        }
    }
}
