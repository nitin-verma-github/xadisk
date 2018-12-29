/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk http://xadisk.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.additional;

import java.io.IOException;

public class Utilities {

    static IOException wrapWithIOException(Throwable t) {
        return (IOException) new IOException().initCause(t);
    }
}
