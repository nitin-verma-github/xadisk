/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.tests.performance;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Note that these performance tests are "under construction". Your suggestions about
 * writing these tests, setting up the system and taking measurements are always welcome.
 * Thanks.
 */
public abstract class TimeMeasuredWork implements Runnable {

    private AtomicLong timeTaken;
    private boolean useXADisk;

    public TimeMeasuredWork(AtomicLong timeTaken, boolean useXADisk) {
        this.timeTaken = timeTaken;
        this.useXADisk = useXADisk;
    }

    public void run() {
        try {
            Thread.sleep(1000);//to allow other threads to "start".
            long startTime = System.currentTimeMillis();
            if (useXADisk) {
                doWorkViaXADisk();
            } else {
                doWorkDirectly();
            }
            long endTime = System.currentTimeMillis();
            timeTaken.addAndGet(endTime - startTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected abstract void doWorkDirectly() throws Exception;

    protected abstract void doWorkViaXADisk() throws Exception;
}
