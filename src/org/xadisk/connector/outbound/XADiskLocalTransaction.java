/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.connector.outbound;

import javax.resource.ResourceException;
import javax.resource.spi.LocalTransaction;
import org.xadisk.filesystem.XAFileSystemCommonness;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;

public class XADiskLocalTransaction implements LocalTransaction {

    private final XADiskManagedConnection mc;
    private int transactionTimeOut;

    public XADiskLocalTransaction(XADiskManagedConnection mc) {
        this.mc = mc;
        XAFileSystemCommonness xaFileSystem = (XAFileSystemCommonness) mc.getUnderlyingXAFileSystem();
        this.transactionTimeOut = xaFileSystem.getDefaultTransactionTimeout();
    }

    void _begin() {
        mc.setTypeOfCurrentTransaction(XADiskManagedConnection.LOCAL_TRANSACTION);
        mc.refreshSessionForBeginLocalTransaction().setTransactionTimeout(transactionTimeOut);
    }

    public void begin() throws ResourceException {
        _begin();
    }

    void _rollback() throws NoTransactionAssociatedException {
        mc.setTypeOfCurrentTransaction(XADiskManagedConnection.NO_TRANSACTION);
        mc.getSessionOfLocalTransaction().rollback();
    }

    public void rollback() throws ResourceException {
        try {
            _rollback();
        } catch (NoTransactionAssociatedException note) {
            throw new ResourceException(note);
        }
    }

    void _commit() throws NoTransactionAssociatedException {
        mc.setTypeOfCurrentTransaction(XADiskManagedConnection.NO_TRANSACTION);
        mc.getSessionOfLocalTransaction().commit();
    }

    public void commit() throws ResourceException {
        try {
            _commit();
        } catch (NoTransactionAssociatedException note) {
            throw new ResourceException(note);
        }
    }

    int getTransactionTimeOut() {
        return transactionTimeOut;
    }

    void setTransactionTimeOut(int transactionTimeOut) {
        this.transactionTimeOut = transactionTimeOut;
    }
}
