/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.impl;

import java.io.File;
import org.xadisk.bridge.proxies.facilitators.RemoteObjectProxy;
import org.xadisk.filesystem.Lock;

public class RemoteLock extends RemoteObjectProxy implements Lock {

    private static final long serialVersionUID = 1L;
    private final File resource;
    private boolean exclusive;

    public RemoteLock(long objectId, File resource, boolean exclusive) {
        super(objectId, null);
        this.resource = resource;
        this.exclusive = exclusive;
    }

    public File getResource() {
        return resource;
    }

    public boolean isExclusive() {
        return exclusive;
    }
}