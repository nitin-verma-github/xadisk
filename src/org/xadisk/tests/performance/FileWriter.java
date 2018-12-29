/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.tests.performance;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;

/**
 * Note that these performance tests are "under construction". Your suggestions about
 * writing these tests, setting up the system and taking measurements are always welcome.
 * Thanks.
 */
public class FileWriter extends TimeMeasuredWork {

    private File filePath;
    private byte[] b = new byte[Appraiser.BUFFER_SIZE];

    public FileWriter(File filePath, AtomicLong timeTaken, boolean useXADisk) {
        super(timeTaken, useXADisk);
        this.filePath = filePath;
        Arrays.fill(b, (byte) 'a');
    }

    @Override
    protected void doWorkDirectly() throws Exception {
        FileChannel fc = new FileOutputStream(filePath, false).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(Appraiser.BUFFER_SIZE);
        buffer.put(b);
        buffer.limit(b.length);
        for (int i = 0; i < Appraiser.FILE_SIZE; i += Appraiser.BUFFER_SIZE) {
            buffer.position(0);
            fc.write(buffer);
        }
        fc.force(false);
        fc.close();
    }

    @Override
    protected void doWorkViaXADisk() throws Exception {
        XAFileSystem xafs = XAFileSystemProxy.getNativeXAFileSystemReference("");
        Session session = xafs.createSessionForLocalTransaction();
        XAFileOutputStream xafos = session.createXAFileOutputStream(filePath, true);

        for (int i = 0; i < Appraiser.FILE_SIZE; i += Appraiser.BUFFER_SIZE) {
            xafos.write(b);
        }
        session.commit();
    }
}
