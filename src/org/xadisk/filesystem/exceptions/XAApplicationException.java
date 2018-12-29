/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.exceptions;

/**
 * This is an abstract super class of all exceptions which are thrown when
 * a client-scoped (not system scoped) problem is encountered during an API call to XADisk.
 * <p> These exceptions are normal and require handling at the client application level.
 * In other words, these exceptions are not associated with XADisk system wide problems
 * indicated by {@link XASystemException}.
 *
 * @since 1.0
 */
public abstract class XAApplicationException extends Exception {

    private static final long serialVersionUID = 1L;

    public XAApplicationException() {
    }

    public XAApplicationException(String msg) {
        super(msg);
    }

    public XAApplicationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public XAApplicationException(Throwable cause) {
        super(cause);
    }
}
