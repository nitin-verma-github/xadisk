/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.connector.outbound;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionMetaData;

public class XADiskManagedConnectionMetaData implements ManagedConnectionMetaData {

    public String getEISProductName() throws ResourceException {
        return "XADisk";
    }

    public String getEISProductVersion() throws ResourceException {
        return "1.0";
    }

    public int getMaxConnections() throws ResourceException {
        return 0;
    }

    public String getUserName() throws ResourceException {
        return "irrelevant";
    }
}
