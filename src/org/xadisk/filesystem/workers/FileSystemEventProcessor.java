/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.workers;

import java.lang.reflect.Method;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import javax.transaction.Status;
import org.xadisk.bridge.proxies.impl.RemoteMessageEndpoint;
import org.xadisk.bridge.proxies.impl.RemoteMessageEndpointFactory;
import org.xadisk.bridge.server.conversation.HostedContext;
import org.xadisk.connector.inbound.LocalEventProcessingXAResource;
import org.xadisk.connector.inbound.FileSystemEventListener;
import org.xadisk.filesystem.FileSystemStateChangeEvent;
import org.xadisk.filesystem.NativeXAFileSystem;

public class FileSystemEventProcessor implements Work {

    private final MessageEndpointFactory mef;
    private final FileSystemStateChangeEvent event;
    private final NativeXAFileSystem xaFileSystem;

    FileSystemEventProcessor(MessageEndpointFactory mef, FileSystemStateChangeEvent event, NativeXAFileSystem xaFileSystem) {
        this.mef = mef;
        this.event = event;
        this.xaFileSystem = xaFileSystem;
    }

    public void release() {
    }

    public void run() {
        LocalEventProcessingXAResource epXAR = new LocalEventProcessingXAResource(xaFileSystem, event);
        MessageEndpoint mep = null;
        try {
            while (mep == null) {
                try {
                    mep = mef.createEndpoint(epXAR);
                } catch (UnavailableException uae) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            Method methodToInvoke = FileSystemEventListener.class.getMethod("onFileSystemEvent",
                    FileSystemStateChangeEvent.class);
            mep.beforeDelivery(methodToInvoke);
            methodToInvoke.invoke(mep, event);
            mep.afterDelivery();

            if (mef instanceof RemoteMessageEndpointFactory) {
                HostedContext globalCallbackContext = xaFileSystem.getGlobalCallbackContext();
                globalCallbackContext.deHostObject(epXAR);
            }

            mep.release();
            if (mep instanceof RemoteMessageEndpoint) {
                RemoteMessageEndpoint remoteMEP = (RemoteMessageEndpoint) mep;
                remoteMEP.shutdown();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (epXAR.getTransactionOutcome() != Status.STATUS_COMMITTED) {
                try {
                    xaFileSystem.getDeadLetter().dumpAndCommitMessage(event, epXAR);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }
}
