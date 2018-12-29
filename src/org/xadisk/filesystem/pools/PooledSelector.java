/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.pools;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

public class PooledSelector implements PooledResource {

    private volatile long lastFreed = -1;
    private Selector selector;

    public PooledSelector() throws IOException {
        this.selector = Selector.open();
    }

    public long getLastFreed() {
        return lastFreed;
    }

    public Selector getSelector() {
        return selector;
    }

    public void markFree() {
        Set<SelectionKey> keys = selector.keys();
        for (SelectionKey key : keys) {
            key.cancel();
        }
        try {
            selector.selectNow();
        } catch (IOException ioe) {
            //ignore.
        }
        lastFreed = System.currentTimeMillis() / 1000;
    }
}
