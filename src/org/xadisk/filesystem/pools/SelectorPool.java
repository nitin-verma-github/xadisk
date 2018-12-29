/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.pools;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SelectorPool implements ResourcePool<PooledSelector> {

    private final ConcurrentLinkedQueue<PooledSelector> freeSelectors;
    private final int idleTime;

    public SelectorPool(int idleTime) {
        this.idleTime = idleTime;
        this.freeSelectors = new ConcurrentLinkedQueue<PooledSelector>();
    }

    public PooledSelector checkOut() {
        PooledSelector temp = lookIntoCurrentPool();
        if (temp != null) {
            return temp;
        }
        temp = allocateNewInCurrentPool();
        if (temp != null) {
            return temp;
        }
        return null;
    }

    private PooledSelector lookIntoCurrentPool() {
        PooledSelector freeSelector = freeSelectors.poll();
        return freeSelector;
    }

    private PooledSelector allocateNewInCurrentPool() {
        PooledSelector newSelector = null;
        try {
            newSelector = new PooledSelector();
            return newSelector;
        } catch (IOException ioe) {
            //allocation failed...return null.
            return null;
        }
    }

    public void checkIn(PooledSelector selector) {
        selector.markFree();
        freeSelectors.offer(selector);
    }

    public void freeIdleMembers() {
        long now = System.currentTimeMillis() / 1000;
        while (true) {
            PooledSelector selector = freeSelectors.peek();
            if (selector == null) {
                break;
            }
            if (now - selector.getLastFreed() > idleTime) {
                freeSelectors.remove(selector);
            } else {
                break;
            }
        }
    }
}
