/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.additional;

import java.io.IOException;
import java.io.InputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;
import org.xadisk.filesystem.exceptions.XAApplicationException;

/**
 * This class acts as a wrapper around the {@link XAFileInputStream} for the purpose of
 * providing the standard {@link InputStream} implementation
 * (because {@link XAFileInputStream} itself does not extend the {@link InputStream}).
 *
 * @since 1.0
 */
public class XAFileInputStreamWrapper extends InputStream {

    private XAFileInputStream xis;
    private long latestMarkPoint = -1;

    /**
     * The sole constructor which takes the {@link XAFileInputStream} to be wrapped.
     * @param xis the {@link XAFileInputStream} to be wrapped.
     */
    public XAFileInputStreamWrapper(XAFileInputStream xis) {
        this.xis = xis;
    }

    @Override
    public int available() throws IOException {
        try {
            return xis.available();
        } catch (XAApplicationException e) {
            throw Utilities.wrapWithIOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            xis.close();
        } catch (XAApplicationException e) {
            throw Utilities.wrapWithIOException(e);
        }
    }

    @Override
    public synchronized void mark(int readlimit) {
        this.latestMarkPoint = xis.position();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public int read() throws IOException {
        try {
            return xis.read();
        } catch (XAApplicationException e) {
            throw Utilities.wrapWithIOException(e);
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        try {
            return xis.read(b);
        } catch (XAApplicationException e) {
            throw Utilities.wrapWithIOException(e);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            return xis.read(b, off, len);
        } catch (XAApplicationException e) {
            throw Utilities.wrapWithIOException(e);
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        //why do the mark/reset methods are syncronous in the IS clases.
        if (latestMarkPoint == -1) {
            throw new IOException("No corresponding mark does exist.");
        }
        try {
            //we do not honor the readlimit; a more flexible approach, which IS spec also allows.
            xis.position(latestMarkPoint);
            this.latestMarkPoint = -1;
        } catch (XAApplicationException e) {
            throw Utilities.wrapWithIOException(e);
        }
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        try {
            return xis.skip(n);
        } catch (XAApplicationException e) {
            throw Utilities.wrapWithIOException(e);
        }
    }
}
