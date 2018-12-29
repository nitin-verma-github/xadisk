/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.exceptions;

import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.connector.outbound.XADiskConnectionFactory;

/**
 * This exception is an unchecked exception and can be thrown during :
 * <ol>
 * <li> {@link XAFileSystem#waitForBootup(long) waitForBootup} when the XAFileSystem is referring to
 * a native (running in the same JVM) or a remote XADisk instance.
 * <li> {@link XAFileSystem#createSessionForLocalTransaction() createSessionForLocalTransaction} when
 * the XAFileSystem is referring to a native (running in the same JVM) or remote XADisk instance.
 * <li> {@link XAFileSystem#createSessionForXATransaction() createSessionForXATransaction} when
 * the XAFileSystem is referring to a native (running in the same JVM) or remote XADisk instance.
 * <li> when using XADisk as a JCA Resource Adapter, during the connection creation steps upto
 * and including {@link XADiskConnectionFactory#getConnection() getConnection}.
 * </ol>
 * 
 * Occurrence of this exception indicates that the XADisk instance has not yet completed its bootup process;
 * the bootup process also involves crash recovery.
 * 
 * <p> Note that XADisk completes (rollback or commit) all of its <i>ongoing</i> (which were running during
 * last shutdown/crash of XADisk) local transactions and XA transactions as a part of its crash
 * recovery. For in-doubt XA transactions, an XADisk instance waits for the Transaction Manager
 * to inform it about the transaction decision; XADisk keeps boot-completion on hold
 * due to these in-doubt XA transactions.
 *
 * @since 1.0
 */
public class RecoveryInProgressException extends XASystemException {

    private static final long serialVersionUID = 1L;

    @Override
    public String getMessage() {
        return "This XADisk instance has not yet completed its crash recovery process.";
    }
}
