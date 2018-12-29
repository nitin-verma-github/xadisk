/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.additional;

import java.io.IOException;
import java.io.OutputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.filesystem.exceptions.XAApplicationException;

/**
 * This class acts as a wrapper around the {@link XAFileOutputStream} for the purpose of
 * providing the standard {@link OutputStream} implementation (because {@link XAFileOutputStream}
 * itself does not extend the {@link OutputStream}).
 *
 * @since 1.0
 */
public class XAFileOutputStreamWrapper extends OutputStream {

    private XAFileOutputStream xos;

    /**
     * The sole constructor which takes the {@link XAFileOutputStream} to be wrapped.
     * @param xos the {@link XAFileOutputStream} to be wrapped.
     */
    public XAFileOutputStreamWrapper(XAFileOutputStream xos) {
        this.xos = xos;
    }

    @Override
    public void close() throws IOException {
        try {
            xos.close();
        } catch (XAApplicationException e) {
            throw Utilities.wrapWithIOException(e);
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            xos.flush();
        } catch (XAApplicationException e) {
            throw Utilities.wrapWithIOException(e);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        try {
            xos.write(b);
        } catch (XAApplicationException e) {
            throw Utilities.wrapWithIOException(e);
        }
    }

    @Override
    public void write(int b) throws IOException {
        try {
            xos.write(b);
        } catch (XAApplicationException e) {
            throw Utilities.wrapWithIOException(e);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            xos.write(b, off, len);
        } catch (XAApplicationException e) {
            throw Utilities.wrapWithIOException(e);
        }
    }
}
