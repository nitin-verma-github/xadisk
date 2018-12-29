/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.interfaces;

import java.io.OutputStream;
import org.xadisk.additional.XAFileOutputStreamWrapper;
import org.xadisk.filesystem.exceptions.ClosedStreamException;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;

/**
 * Represents an output stream to a file. This stream always appends to the target file.
 * Such a stream can be opened through the
 * {@link XADiskBasicIOOperations#createXAFileOutputStream(File, boolean) createXAFileOutputStream} method.
 * <p> This stream can be further wrapped by a utility class {@link XAFileOutputStreamWrapper} to
 * get easy pluggability via the standard {@link OutputStream}.
 *
 * @since 1.0
 */
public interface XAFileOutputStream {

    /**
     * Writes all bytes from <i>b</i> into the file.
     * @param b the byte array to write.
     * @throws ClosedStreamException
     * @throws NoTransactionAssociatedException
     */
    public void write(byte[] b) throws ClosedStreamException, NoTransactionAssociatedException;

    /**
     * Writes the input byte into the file.
     * @param b the byte to write.
     * @throws ClosedStreamException
     * @throws NoTransactionAssociatedException
     */
    public void write(int b) throws ClosedStreamException, NoTransactionAssociatedException;

    /**
     * Writes bytes from array <i>b</i>, starting at offset <i>off</i>, upto
     * total <i>length</i> bytes, into the file.
     * @param b the byte array.
     * @param off offset in the byte array.
     * @param length number of bytes to write.
     * @throws ClosedStreamException
     * @throws NoTransactionAssociatedException
     */
    public void write(byte[] b, int off, int length) throws ClosedStreamException, NoTransactionAssociatedException;

    /**
     * Flushes the buffer of this stream. This does not imply that the data gets written
     * to the disk. A guarantee for the buffered data to get persisted is made only after
     * the current transaction gets committed successfully.
     * @throws ClosedStreamException
     * @throws NoTransactionAssociatedException
     */
    public void flush() throws ClosedStreamException, NoTransactionAssociatedException;

    /**
     * Closes this stream. After closing, this stream becomes invalid for any i/o operations.
     * @throws NoTransactionAssociatedException
     */
    public void close() throws NoTransactionAssociatedException;

    /**
     * Tells whether this stream has been closed.
     * @return true if the stream is closed; false otherwise.
     */
    public boolean isClosed();
}
