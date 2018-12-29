/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.server.conversation;

import java.util.ArrayList;

public class ConversationalHostedContext implements HostedContext {

    private final ArrayList<Object> remoteInvocationTargets = new ArrayList<Object>();

    public long hostObject(Object target) {
        remoteInvocationTargets.add(target);
        return remoteInvocationTargets.size() - 1;
    }

    public Object getHostedObjectWithId(long objectId) {
        return remoteInvocationTargets.get((int) objectId);//safe to case as conversations start from
        //id 0 instead of a long value as in case of global contex.
    }

    public long deHostObject(Object target) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void deHostObjectWithId(long objectId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
