/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.exceptions;

/**
 * This is an abstract super class of all exceptions which are thrown because of a problem
 * associated with the whole XADisk system, and may not be linked to (though, it could be
 * directly/indirectly caused by) the method call (which threw this exception) from the
 * application client.
 *
 * @since 1.0
 */
public abstract class XASystemException extends RuntimeException {

    public XASystemException(String msg) {
        super(msg);
    }

    public XASystemException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public XASystemException(Throwable cause) {
        super(cause);
    }

    public XASystemException() {
    }
}