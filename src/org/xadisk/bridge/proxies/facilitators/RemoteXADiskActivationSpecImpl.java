/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.facilitators;

import org.xadisk.connector.inbound.XADiskActivationSpecImpl;

public class RemoteXADiskActivationSpecImpl extends XADiskActivationSpecImpl {

    private static final long serialVersionUID = 1L;
    private int originalActivationSpecObjectsHashCode = this.hashCode();//this will get serialized too.

    public RemoteXADiskActivationSpecImpl() {
    }

    public RemoteXADiskActivationSpecImpl(XADiskActivationSpecImpl copyFrom) {
        this.setFileNamesAndEventInterests(copyFrom.getFileNamesAndEventInterests());
        this.originalActivationSpecObjectsHashCode = copyFrom.hashCode();
        //no need to set other values like areFilesRemote, remoteAddr, remotePort as these values
        //were meant to be used by local xadisk instance only to decide where to send the activation to.
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RemoteXADiskActivationSpecImpl) {
            RemoteXADiskActivationSpecImpl that = (RemoteXADiskActivationSpecImpl) obj;
            return this.originalActivationSpecObjectsHashCode == that.originalActivationSpecObjectsHashCode;
            //no need to check for the remote xadisk's "system id" because this equals method is always called
            //alongwith that of RemoteMEPF's equals which itself checks for "system id". And these two equals
            //are called during equals of the enclosing entity called epActivation.
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.originalActivationSpecObjectsHashCode;
    }

    public int getOriginalActivationSpecObjectsHashCode() {
        return originalActivationSpecObjectsHashCode;
    }

    public void setOriginalActivationSpecObjectsHashCode(int originalActivationSpecObjectsHashCode) {
        this.originalActivationSpecObjectsHashCode = originalActivationSpecObjectsHashCode;
    }
}
