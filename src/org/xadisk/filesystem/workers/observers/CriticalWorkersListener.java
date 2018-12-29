/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.workers.observers;

import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import org.xadisk.filesystem.NativeXAFileSystem;

public class CriticalWorkersListener implements WorkListener {

    private final NativeXAFileSystem xaFileSystem;

    public CriticalWorkersListener(NativeXAFileSystem xaFileSystem) {
        this.xaFileSystem = xaFileSystem;
    }

    public void workAccepted(WorkEvent we) {
    }

    public void workCompleted(WorkEvent we) {
        if (we.getType() == WorkEvent.WORK_COMPLETED) {
            WorkException workException = we.getException();
            if (workException != null) {
                xaFileSystem.notifySystemFailure(workException);
            }
        }
    }

    public void workRejected(WorkEvent we) {
        //if we decide to use "scheduleWork" for any case with zero OR non-zero startTimeout,
        //(in both cases) the work-rejected-due-to-starttimeout exception can come on this
        //listener (which does not come to a listener when using startWork, because startWork returns only
        //after starting the work, and so such starttimeout exception would directly come to the caller.
    }

    public void workStarted(WorkEvent we) {
    }
}
