/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.tests.performance;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicLong;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;

/**
 * Note that these performance tests are "under construction". Your suggestions about
 * writing these tests, setting up the system and taking measurements are always welcome.
 * Thanks.
 */
public class FileReader extends TimeMeasuredWork {

    private File filePath;
    private byte[] b = new byte[Appraiser.BUFFER_SIZE];

    public FileReader(File filePath, AtomicLong timeTaken, boolean useXADisk) {
        super(timeTaken, useXADisk);
        this.filePath = filePath;
    }

    @Override
    protected void doWorkDirectly() throws Exception {
        FileChannel fc = new FileInputStream(filePath).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(Appraiser.BUFFER_SIZE);
        for (int i = 0; i < Appraiser.FILE_SIZE; i += Appraiser.BUFFER_SIZE) {
            buffer.position(0);
            fc.read(buffer);
        }
        fc.close();
    }

    @Override
    protected void doWorkViaXADisk() throws Exception {
        XAFileSystem xafs = XAFileSystemProxy.getNativeXAFileSystemReference("");
        Session session = xafs.createSessionForLocalTransaction();
        XAFileInputStream xafis = session.createXAFileInputStream(filePath, true);
        while (xafis.read(b) != -1) {
        }
        session.commit();
    }
}
