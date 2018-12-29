/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.exceptions;

/**
 * This is an unchecked exception thrown by any of the XADisk APIs to indicate
 * that the XADisk instance has encountered a critical issue and is no more available.
 *
 * @since 1.0
 */
public class XASystemNoMoreAvailableException extends XASystemException {

    private static final long serialVersionUID = 1L;
    private final String message;

    public XASystemNoMoreAvailableException(Throwable cause) {
        super(cause);
        this.message = "The XADisk instance has encoutered a critial issue and is no more available."
                + " Such a condition is very rare. If you think you have setup everything right for"
                + " XADisk to work, please consider discussing in XADisk forums, or raising a bug"
                + " with details";
    }

    public XASystemNoMoreAvailableException() {
        this.message = "The XADisk instance has been shutdown and is no more available for use.";
    }

    @Override
    public String getMessage() {
        return message;
    }
}
