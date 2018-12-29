/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.exceptions;

import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;

/**
 * This exception is thrown by the i/o Streams {@link XAFileInputStream} and
 * {@link XAFileOutputStream}, when an operation is invoked on the stream, but it
 * could not be performed because the stream is already closed.
 *
 * @since 1.0
 */
public class ClosedStreamException extends XAApplicationException {

    private static final long serialVersionUID = 1L;
}
