/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.facilitators;

import java.io.IOException;
import java.io.Serializable;

public interface Serializabler<T1, T2 extends Serializable> {

    public T1 reconstruct(T2 t2) throws IOException;

    public T2 serialize(T1 t1);
}
