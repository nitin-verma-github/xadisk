/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.workers;

import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.pools.ResourcePool;

public class ObjectPoolReliever extends TimedWorker {

    private final ResourcePool objectPool;
    private final NativeXAFileSystem xaFileSystem;

    public ObjectPoolReliever(ResourcePool objectPool, int frequency, NativeXAFileSystem xaFileSystem) {
        super(frequency);
        this.objectPool = objectPool;
        this.xaFileSystem = xaFileSystem;
    }

    @Override
    void doWorkOnce() {
        try {
            objectPool.freeIdleMembers();
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
