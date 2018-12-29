/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.exceptions;

/**
 * This exception can appear as a <i>cause</i> of a {@link TransactionRolledbackException}
 * and indicates that the reason for the rollback by XADisk System was transaction time out.
 *
 * @since 1.0
 */
public class TransactionTimeoutException extends XAApplicationException {

    private static final long serialVersionUID = 1L;

    @Override
    public String getMessage() {
        return "The transaction associated earlier was rolled back by XADisk because the transaction"
                + " was open for more than the transaction timeout value.";
    }
}
