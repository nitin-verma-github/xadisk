/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.tests.performance;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicLong;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;

/**
 * Note that these performance tests are "under construction". Your suggestions about
 * writing these tests, setting up the system and taking measurements are always welcome.
 * Thanks.
 */
public class FileTruncater extends TimeMeasuredWork {

    private File filePath;

    public FileTruncater(File filePath, AtomicLong timeTaken, boolean useXADisk) {
        super(timeTaken, useXADisk);
        this.filePath = filePath;
    }

    @Override
    protected void doWorkDirectly() throws Exception {
        FileChannel fc = new FileOutputStream(filePath, true).getChannel();
        fc.truncate((long) (Appraiser.FILE_SIZE * 0.9));
        fc.force(false);
        fc.close();
    }

    @Override
    protected void doWorkViaXADisk() throws Exception {
        XAFileSystem xafs = XAFileSystemProxy.getNativeXAFileSystemReference("");
        Session session = xafs.createSessionForLocalTransaction();
        session.truncateFile(filePath, (long) (Appraiser.FILE_SIZE * 0.9));
        session.commit();
    }
}
