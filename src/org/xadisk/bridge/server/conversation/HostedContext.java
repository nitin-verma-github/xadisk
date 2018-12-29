/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.server.conversation;

public interface HostedContext {

    public long hostObject(Object target);

    public void deHostObjectWithId(long objectId);

    public long deHostObject(Object target);

    public Object getHostedObjectWithId(long objectId);
}
