/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.exceptions;

/**
 * This exception is thrown by any of the XADisk API methods when the target XADisk instance is a remote one
 * (running on a remote JVM) and a communication error (most likely, network related) is encountered
 * during the method call.
 * <p> This is a subclass of {@link XASystemException} and hence is a {@link RuntimeException}.
 *
 * @since 1.0
 */
public class ConnectionException extends XASystemException {

    private static final long serialVersionUID = 1L;

    public ConnectionException(Throwable cause) {
        super(cause);
    }
}
