/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.workers.observers;

import java.util.concurrent.atomic.AtomicInteger;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

public class EventDispatchListener implements WorkListener {

    private final AtomicInteger ongoingConcurrentDeliveries = new AtomicInteger(0);

    public void workCompleted(WorkEvent we) {
        ongoingConcurrentDeliveries.decrementAndGet();
    }

    public void workStarted(WorkEvent we) {
        ongoingConcurrentDeliveries.incrementAndGet();
    }

    public int getOngoingConcurrentDeliveries() {
        return ongoingConcurrentDeliveries.get();
    }

    public void workAccepted(WorkEvent we) {
    }

    public void workRejected(WorkEvent we) {
    }
}
