/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.tests.correctness;

import java.io.InputStream;
import java.io.OutputStream;

class ChildProcessOutputStreamReader implements Runnable {

    private InputStream is;
    private OutputStream os;
    private static final int BUFFER_SIZE = 1000;
    private byte buffer[] = new byte[BUFFER_SIZE];

    public ChildProcessOutputStreamReader(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;
    }

    public void run() {
        try {
            while (true) {
                int numRead = is.read(buffer);
                if (numRead == -1) {
                    return;
                }
                os.write(buffer, 0, numRead);
                os.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
