/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.standalone;

import java.util.concurrent.ThreadPoolExecutor;
import org.xadisk.filesystem.FileSystemConfiguration;
import javax.resource.spi.work.WorkManager;

/**
 * This class represents the configuration object required for booting an XADisk instance.
 * <p >When XADisk is not used as a JCA Resource Adapter, XADisk uses its own thin implementation of
 * {@link WorkManager} (which is otherwise available from the JavaEE Server due to JCA contract).
 * This WorkManager implementation relies on a JDK utility called {@link ThreadPoolExecutor}.
 * 
 * <p> All of the three properties of this class are optional performance tuning properties and
 * are used as-is to set the properties {@link ThreadPoolExecutor#corePoolSize corePoolSize},
 * {@link ThreadPoolExecutor#maximumPoolSize maximumPoolSize} and {@link ThreadPoolExecutor#keepAliveTime
 * keepAliveTime} of the {@link ThreadPoolExecutor}.
 *
 * @since 1.0
 */
public class StandaloneFileSystemConfiguration extends FileSystemConfiguration {

    private static final long serialVersionUID = 1L;
    private int workManagerCorePoolSize = 10;
    private int workManagerMaxPoolSize = Integer.MAX_VALUE;
    private long workManagerKeepAliveTime = 60; //in seconds.

    /**
     * This is the sole constructor for this configuration object, and accepts
     * the two mandatory configuration properties for XADisk.
     * @param xaDiskHome path of the XADisk System directory. See the description of
     * {@link FileSystemConfiguration#setXaDiskHome(java.lang.String) setXaDiskHome}.
     * @param instanceId instance-id of the XADisk instance. See the description of
     * {@link FileSystemConfiguration#setInstanceId(java.lang.String)}.
     */
    public StandaloneFileSystemConfiguration(String xaDiskHome, String instanceId) {
        super(xaDiskHome, instanceId);
    }

    /**
     * Returns the value of workManagerCorePoolSize.
     * <p> Please refer to the description of {@link ThreadPoolExecutor#getCorePoolSize()
     * ThreadPoolExecutor.corePoolSize}.
     * <p> Default value is 10.
     * @return value of workManagerCorePoolSize.
     */
    public int getWorkManagerCorePoolSize() {
        return workManagerCorePoolSize;
    }

    /**
     * Sets the value of workManagerCorePoolSize.
     * <p> Please refer to the description of {@link ThreadPoolExecutor#getCorePoolSize()
     * ThreadPoolExecutor.corePoolSize}.
     * <p> Default value is 10.
     * @param workManagerCorePoolSize new value of workManagerCorePoolSize.
     */
    public void setWorkManagerCorePoolSize(int workManagerCorePoolSize) {
        this.workManagerCorePoolSize = workManagerCorePoolSize;
    }

    /**
     * Returns the value of workManagerMaxPoolSize.
     * <p> Please refer to the description of {@link ThreadPoolExecutor#getMaximumPoolSize()
     * ThreadPoolExecutor.maximumPoolSize}.
     * <p> Default value is {@link Integer#MAX_VALUE}.
     * @return value of the workManagerMaxPoolSize.
     */
    public int getWorkManagerMaxPoolSize() {
        return workManagerMaxPoolSize;
    }

    /**
     * Sets the value of workManagerMaxPoolSize.
     * <p> Please refer to the description of {@link ThreadPoolExecutor#getMaximumPoolSize()
     * ThreadPoolExecutor.maximumPoolSize}.
     * <p> Default value is {@link Integer#MAX_VALUE}.
     * @param workManagerMaxPoolSize new value of workManagerMaxPoolSize.
     */
    public void setWorkManagerMaxPoolSize(int workManagerMaxPoolSize) {
        this.workManagerMaxPoolSize = workManagerMaxPoolSize;
    }

    /**
     * Returns the value of workManagerKeepAliveTime.
     * <p> Please refer to the description of {@link ThreadPoolExecutor#getKeepAliveTime(java.util.concurrent.TimeUnit)
     * ThreadPoolExecutor.keepAliveTime}.
     * <p> Default value is 60 seconds.
     * @return value of workManagerKeepAliveTime.
     */
    public long getWorkManagerKeepAliveTime() {
        return workManagerKeepAliveTime;
    }

    /**
     * Sets the value of workManagerKeepAliveTime.
     * <p> Please refer to the description of {@link ThreadPoolExecutor#getKeepAliveTime(java.util.concurrent.TimeUnit)
     * ThreadPoolExecutor.keepAliveTime}.
     * <p> Default value is 60 seconds.
     * @param workManagerKeepAliveTime new value of workManagerKeepAliveTime.
     */
    public void setWorkManagerKeepAliveTime(long workManagerKeepAliveTime) {
        this.workManagerKeepAliveTime = workManagerKeepAliveTime;
    }
}
