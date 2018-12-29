/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.exceptions;

import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;

/**
 * This is an unchecked exception thrown by
 * {@link XAFileSystemProxy#bootNativeXAFileSystem(StandaloneFileSystemConfiguration)
 * bootNativeXAFileSystem} and boot-up process of a XADisk JCA Resource Adapter to indicate that the
 * XADisk instance has encountered a critical issue and could not complete its booting process.
 *
 * @since 1.0
 */
public class XASystemBootFailureException extends XASystemException {

    private static final long serialVersionUID = 1L;
    private String reason;

    public XASystemBootFailureException(Throwable cause) {
        super(cause);
    }

    public XASystemBootFailureException(String reason) {
        this.reason = reason;
    }

    @Override
    public String getMessage() {
        if (reason != null) {
            return reason;
        }
        return "The XADisk instance has encoutered a critial issue and could not be booted."
                + " Such a condition is very rare. If you think you have setup everything right for"
                + " XADisk to work, please consider discussing in XADisk forums, or raising a bug"
                + " with details.";
    }
}
