/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.tests.correctness;

import java.lang.reflect.Method;
import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.connector.inbound.FileSystemEventListener;
import org.xadisk.filesystem.FileSystemStateChangeEvent;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.TransactionInformation;

public class SimulatedMessageEndpoint implements MessageEndpoint, FileSystemEventListener {

    private XAResource epXAR;
    private SimulatedMessageEndpointFactory owningFactory;
    private TransactionInformation generatedXid;
    public SimulatedMessageEndpointFactory.GoTill goTill;

    public SimulatedMessageEndpoint(XAResource epXAR, SimulatedMessageEndpointFactory owningFactory) {
        this.epXAR = epXAR;
        this.owningFactory = owningFactory;
    }

    public void beforeDelivery(Method meth) throws NoSuchMethodException, ResourceException {
        this.generatedXid = TransactionInformation.getXidInstanceForLocalTransaction(
                ((NativeXAFileSystem) XAFileSystemProxy.getNativeXAFileSystemReference("local")).getNextLocalTransactionId());
        try {
            epXAR.start(generatedXid, XAResource.TMNOFLAGS);
        } catch (XAException xae) {
            xae.printStackTrace();
            throw new ResourceException(xae);
        }
    }

    public void onFileSystemEvent(FileSystemStateChangeEvent event) {
        System.out.println("Received " + event.getEventType() + " event for file " + event.getFile());
        if (this.goTill == SimulatedMessageEndpointFactory.GoTill.consume) {
            throw new ArrayIndexOutOfBoundsException("intentional");
        }
        owningFactory.incrementEventsReceivedCount();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
        }
    }

    public void afterDelivery() throws ResourceException {
        try {
            epXAR.end(generatedXid, XAResource.TMSUCCESS);
            epXAR.prepare(generatedXid);
            if (this.goTill == SimulatedMessageEndpointFactory.GoTill.prepare) {
                return;
            }
            epXAR.commit(generatedXid, false);
        } catch (XAException xae) {
            throw new ResourceException(xae);
        }
    }

    public void release() {
    }
}
