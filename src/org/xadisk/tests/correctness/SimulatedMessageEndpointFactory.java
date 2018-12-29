/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.tests.correctness;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

public class SimulatedMessageEndpointFactory implements MessageEndpointFactory {

    private AtomicInteger eventsReceived = new AtomicInteger(0);
    public GoTill goTill = GoTill.commit;

    public enum GoTill {

        consume, prepare, commit
    };

    public MessageEndpoint createEndpoint(XAResource xar) throws UnavailableException {
        SimulatedMessageEndpoint smep = new SimulatedMessageEndpoint(xar, this);
        smep.goTill = this.goTill;
        return smep;
    }

    public boolean isDeliveryTransacted(Method meth) throws NoSuchMethodException {
        return true;
    }

    public void incrementEventsReceivedCount() {
        eventsReceived.getAndIncrement();
    }

    public int getEventsReceivedCount() {
        return eventsReceived.get();
    }
}
