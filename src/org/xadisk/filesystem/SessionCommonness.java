/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem;

import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;

public interface SessionCommonness extends Session {

    public void commit(boolean onePhase) throws NoTransactionAssociatedException;

    public void prepare() throws NoTransactionAssociatedException;

    public boolean isUsingReadOnlyOptimization();

    public void completeReadOnlyTransaction() throws NoTransactionAssociatedException;
}
