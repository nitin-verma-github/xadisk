/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.connector.inbound;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import org.xadisk.filesystem.FileSystemStateChangeEvent;

public class XADiskActivationSpecImpl implements ActivationSpec, Serializable {

    private static final long serialVersionUID = 1L;
    private transient ResourceAdapter ra;
    private final HashMap<File, String> fileNamesAndInterests = new HashMap<File, String>(10);
    private static final String interestSymbol = "::";
    private static final String seperator = "\\|";
    private String fileNamesAndEventInterests;
    private Boolean areFilesRemote;
    private String remoteServerAddress;
    private Integer remoteServerPort;

    public XADiskActivationSpecImpl() {
    }

    public void setFileNamesAndEventInterests(String filesNamesAndEventInterests) {
        this.fileNamesAndEventInterests = filesNamesAndEventInterests;
        setupFileNamesAndEventInterests(filesNamesAndEventInterests.split(seperator));
    }

    public String getFileNamesAndEventInterests() {
        return fileNamesAndEventInterests;
    }

    //we needed to make these types Strings as the config properties in ra.xml don't have type for a-specs.
    public String getAreFilesRemote() {
        return areFilesRemote.toString();
    }

    public void setAreFilesRemote(String areFilesRemote) {
        this.areFilesRemote = Boolean.valueOf(areFilesRemote);
    }

    public String getRemoteServerAddress() {
        return remoteServerAddress;
    }

    public void setRemoteServerAddress(String remoteServerAddress) {
        this.remoteServerAddress = remoteServerAddress;
    }

    public String getRemoteServerPort() {
        return remoteServerPort.toString();
    }

    public void setRemoteServerPort(String remoteServerPort) {
        this.remoteServerPort = new Integer(remoteServerPort);
    }

    private void setupFileNamesAndEventInterests(String fileNamesAndEventInterest[]) {
        this.fileNamesAndInterests.clear();
        for (int i = 0; i < fileNamesAndEventInterest.length; i++) {
            String temp[] = fileNamesAndEventInterest[i].split(interestSymbol);
            fileNamesAndInterests.put(new File(temp[0]), temp[1]);
        }
    }

    public ResourceAdapter getResourceAdapter() {
        return this.ra;
    }

    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
        this.ra = ra;
    }

    public void validate() throws InvalidPropertyException {
        Iterator iter = fileNamesAndInterests.values().iterator();
        while (iter.hasNext()) {
            String interest = (String) iter.next();
            try {
                Integer.valueOf(interest);
            } catch (NumberFormatException nfe) {
                throw new InvalidPropertyException("Invalid event-interest specification : " + interest);
            }
        }
    }

    public boolean isEndpointInterestedIn(FileSystemStateChangeEvent event) {
        String interested = fileNamesAndInterests.get(event.getFile());
        if (interested != null) {
            byte interestedBits = Byte.parseByte(interested, 2);
            byte queriedInterest = event.getEventType().getByteValue();
            if ((interestedBits & queriedInterest) > 0) {
                return true;
            }
        }
        return false;
    }

    /*
     * From JCA Spec:
     "These objects, in general, should not override the default equals and hashCode methods. However,
     if these methods are overridden, they must preserve the equality
     constraints based on Java object identity; that is, no two objects are considered equal."
     * From Java API of Object class:
     * "As much as is reasonably practical, the hashCode method defined by class Object
     does return distinct integers for distinct objects."
     * We always compare the asSpec as part of Activation comparison, so by chance if
     * remote asSpec equals the local one, the MEF won't equal due to different classes.
     * But, anyway, for clarity keep a separate flag to know if remote.
     * No "anyway". This created an issue on weblogic as reported by someone from the community,
     * Weblogic reports a warning and good container citizens should not trigger warnings...so
     * reverting these equals/hashcode overridden methods. The "different classes" will have to serve
     * the purpose.
     */
    /*@Override
     public boolean equals(Object obj) {
     if (obj instanceof XADiskActivationSpecImpl) {
     XADiskActivationSpecImpl that = (XADiskActivationSpecImpl) obj;
     return this.originalObjectIsRemote == that.originalObjectIsRemote
     && this.originalObjectsHashCode == that.originalObjectsHashCode;
     }
     return false;
     }*/

    /*@Override
     public int hashCode() {
     return this.originalObjectsHashCode;
     }*/
}
