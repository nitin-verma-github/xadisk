/*
Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/


package org.xadisk.examples.jca;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import org.xadisk.connector.inbound.FileSystemEventListener;
import org.xadisk.filesystem.FileSystemStateChangeEvent;

/**
 * This example is applicable only for Message Driven Beans (JavaEE).
 */
/**
 * This is a very basic example which
 *      - declares the XADisk's ActivationSpec properties using annotations. These properties indicate
 *              the XADisk instance to connect to and also the kind of events the MDB is interested in.
 *      - specifies that this MDB should be invoked inside a transaction always.
 *      - prints a message on receiving an event from the XADisk instance.
 */
/**
 * How to run this example:
 *
 * 1) Change the various values inside the ActivationSpec properties to suit your environment.
 * 2) Put this MDB class inside an MDB jar file.
 * 3) Deploy XADisk as a Resource Adapter in the JavaEE (5.0 or above) server (independent of where is the
 *          XADisk instance this MDB is bound to).
 * 4) Deploy the MDB jar.
 * 5) Using any type of a Java application (JavaEE or non-JavaEE), perform some file operation on the
 *    XADisk instance (to which this MDB is bound to) so that an event get generated for this MDB. Remember to set the
 *    publish flag to true on the Session/XADiskConnection via setPublishFileStateChangeEventsOnCommit method.
 */
/**
 * Please refer to the XADisk JavaDoc and User Guide for knowing more about using XADisk.
 */
@MessageDriven(name = "XADiskListenerMDB1",
activationConfig = {
    @ActivationConfigProperty(propertyName = "fileNamesAndEventInterests", propertyValue = "C:\\::111|D:\\testDir\\::111"),
    @ActivationConfigProperty(propertyName = "areFilesRemote", propertyValue = "true"),
    @ActivationConfigProperty(propertyName = "remoteServerAddress", propertyValue = "localhost"),
    @ActivationConfigProperty(propertyName = "remoteServerPort", propertyValue = "2010")
})
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class EventListenerMDB implements FileSystemEventListener {

    public void onFileSystemEvent(FileSystemStateChangeEvent event) {
        System.out.println("RECEIVED AN EVENT OF TYPE : " + event.getEventType() + " FOR FILE : " + event.getFile());
    }
}
