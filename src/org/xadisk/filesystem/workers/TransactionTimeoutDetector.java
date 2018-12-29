/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.workers;

import org.xadisk.filesystem.NativeSession;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.ResourceDependencyGraph.Node;
import org.xadisk.filesystem.TransactionInformation;
import org.xadisk.filesystem.exceptions.TransactionTimeoutException;

public class TransactionTimeoutDetector extends TimedWorker {

    private final NativeXAFileSystem xaFileSystem;

    public TransactionTimeoutDetector(int frequency, NativeXAFileSystem xaFileSystem) {
        super(frequency);
        this.xaFileSystem = xaFileSystem;
    }

    @Override
    void doWorkOnce() {
        try {
            NativeSession sessions[] = xaFileSystem.getAllSessions();
            for (int i = 0; i < sessions.length; i++) {
                NativeSession session = sessions[i];
                long timeoutValue = session.getTransactionTimeout() * 1000;
                long birthTime = session.getTimeOfEntryToTransaction();
                long timeNow = System.currentTimeMillis();
                if (timeoutValue > 0 && timeNow - birthTime > timeoutValue) {
                    xaFileSystem.getConcurrencyControl().interruptTransactionIfWaitingForResourceLock(session.getXid(),
                            Node.INTERRUPTED_DUE_TO_TIMEOUT);
                    session.rollbackAsynchronously(new TransactionTimeoutException());
                }
            }
        } catch (Throwable t) {
            xaFileSystem.notifySystemFailure(t);
        }
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
