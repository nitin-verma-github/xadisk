/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem;

import java.io.Serializable;
import java.nio.ByteBuffer;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;
import org.xadisk.filesystem.exceptions.LockingTimedOutException;

/**
 * An object of this class encapsulates the configuration for the XADisk instance, and is used while
 * booting of the XADisk instance.
 * <ol>
 * <li> When deploying XADisk as a JCA Resource Adapter, this configuration object is automatically constructed
 * and initialized by the JavaEE container (based on the deployment descriptor).
 * <li> When booting the XADisk instance directly, an instance of subclass
 * {@link StandaloneFileSystemConfiguration} is used to set/override the various configuration
 * properties, and is subsequently passed to the
 * {@link XAFileSystemProxy#bootNativeXAFileSystem(StandaloneFileSystemConfiguration) bootup} method.
 * </ol>
 *
 * @since 1.0
 */
public class FileSystemConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;
    private Integer directBufferPoolSize = 1000;
    private Integer nonDirectBufferPoolSize = 1000;
    private Integer bufferSize = 4096;
    private String xaDiskHome;
    private String instanceId;
    private Long transactionLogFileMaxSize = 1000000000L;
    private Integer cumulativeBufferSizeForDiskWrite = 1000000;
    private Integer directBufferIdleTime = 100;
    private Integer nonDirectBufferIdleTime = 100;
    private Integer bufferPoolRelieverInterval = 60;
    private Long maxNonPooledBufferSize = 1000000L;
    private Integer deadLockDetectorInterval = 30;
    private Integer lockTimeOut = 10000;
    private Integer maximumConcurrentEventDeliveries = 20;
    private Integer transactionTimeout = 60;
    private Boolean enableRemoteInvocations = false;
    private String serverAddress = "127.0.0.1";
    private Integer serverPort = 9999;
    private Boolean synchronizeDirectoryChanges = true;
    private Boolean enableClusterMode = false;
    private String clusterMasterAddress;
    private Integer clusterMasterPort;

    /**
     * A constructor called by the JavaEE Container while deploying XADisk JCA Resource Adapter. The
     * container will subsequently call setter methods to set properties of this configuration object.
     * (<i>JavaBean approach</i>).
     */
    public FileSystemConfiguration() {
    }

    /**
     * A constructor called by constructor of subclass {@link StandaloneFileSystemConfiguration}.
     * @param xaDiskHome the XADisk System Directory path.
     * @param instanceId the instance-id for the new XADisk instance; must be unique within the JVM.
     */
    protected FileSystemConfiguration(String xaDiskHome, String instanceId) {
        this.xaDiskHome = xaDiskHome;
        this.instanceId = instanceId;
    }

    /**
     * Returns the value of bufferSize (a performance tuning property).
     * <p> The i/o streams of XADisk, {@link XAFileInputStream} and {@link XAFileOutputStream},
     * use {@link ByteBuffer byte-buffers} for holding file's contents. These byte-buffers
     * are either from buffer pool or normally allocated (if the pool is exhausted).
     * <p> This property decides the size of these byte-buffers, both for pooled and normal
     * cases.
     * <p> Default value is 4096.
     * @return value of bufferSize, in bytes.
     */
    public Integer getBufferSize() {
        return bufferSize;
    }

    /**
     * Sets the value of bufferSize (a performance tuning property).
     * <p> The i/o streams of XADisk, {@link XAFileInputStream} and {@link XAFileOutputStream},
     * use {@link ByteBuffer byte-buffers} for holding file's contents. These byte-buffers
     * are either from buffer pool or normally allocated (if the pool is exhausted).
     * <p> This property decides the size of these byte-buffers, both for pooled and normal
     * cases.
     * <p> Default value is 4096.
     * @param bufferSize new value of bufferSize, in bytes.
     */
    public void setBufferSize(Integer bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * Returns the value of directBufferPoolSize (a performance tuning property).
     * <p> It specifies the pool size for the 'direct' ({@link ByteBuffer#isDirect()})
     * byte buffers. Pooled buffers (direct or indirect) are used by i/o streams
     * {@link XAFileInputStream} and {@link XAFileOutputStream} for holding file's contents.
     * <p> Default value is 1000 (means, at most 1000 direct buffers can exist in the pool).
     * @return value of directBufferPoolSize.
     */
    public Integer getDirectBufferPoolSize() {
        return directBufferPoolSize;
    }

    /**
     * Sets the value of directBufferPoolSize (a performance tuning property).
     * <p> It specifies the pool size for the 'direct' ({@link ByteBuffer#isDirect()})
     * byte buffers. Pooled buffers (direct or indirect) are used by i/o streams
     * {@link XAFileInputStream} and {@link XAFileOutputStream} for holding file's contents.
     * <p> Default value is 1000 (means, at most 1000 direct buffers can exist in the pool).
     * @param directBufferPoolSize new value of directBufferPoolSize.
     */
    public void setDirectBufferPoolSize(Integer directBufferPoolSize) {
        this.directBufferPoolSize = directBufferPoolSize;
    }

    /**
     * Returns the value of nonDirectBufferPoolSize (a performance tuning property).
     * <p> It specifies the pool size for the 'nonDirect' ({@link ByteBuffer#isDirect()})
     * byte buffers. Pooled buffers (direct or indirect) are used by i/o streams
     * {@link XAFileInputStream} and {@link XAFileOutputStream} for holding file's contents.
     * <p> Default value is 1000 (means, at most 1000 nonDirect buffers can exist in the pool).
     * @return value of nonDirectBufferPoolSize.
     */
    public Integer getNonDirectBufferPoolSize() {
        return nonDirectBufferPoolSize;
    }

    /**
     * Sets the value of nonDirectBufferPoolSize (a performance tuning property).
     * <p> It specifies the pool size for the 'nonDirect' ({@link ByteBuffer#isDirect()})
     * byte buffers. Pooled buffers (direct or indirect) are used by i/o streams
     * {@link XAFileInputStream} and {@link XAFileOutputStream} for holding file's contents.
     * <p> Default value is 1000 (means, at most 1000 nonDirect buffers can exist in the pool).
     * @param nonDirectBufferPoolSize new value of nonDirectBufferPoolSize.
     */
    public void setNonDirectBufferPoolSize(Integer nonDirectBufferPoolSize) {
        this.nonDirectBufferPoolSize = nonDirectBufferPoolSize;
    }

    /**
     * Returns the value of xaDiskHome (also referred to as XADisk System Directory), a
     * mandatory configuration property.
     * <p> This is a directory where XADisk keeps all of its artifacts required for its functioning.
     * One of the important things XADisk keeps here is the transaction logs.
     * <p> The directory that you specify need not exist. If it exists, it must not be anything other than the one
     * you used for XADisk sometime earlier. If it does not exist, XADisk creates it during initialization.
     * <p> You should not do any kind of modifications inside this directory; as it may lead to failure of XADisk.
     * <p> No two XADisk instances should use the same system directory.
     * @return value of xaDiskHome.
     */
    public String getXaDiskHome() {
        return xaDiskHome;
    }

    /**
     * Sets the value of xaDiskHome (also referred to as XADisk System Directory), a
     * mandatory configuration property.
     * <p> This is a directory where XADisk keeps all of its artifacts required for its functioning.
     * One of the important things XADisk keeps here is the transaction logs.
     * <p> The directory that you specify need not exist. If it exists, it must not be anything other than the one
     * you used for XADisk sometime earlier. If it does not exist, XADisk creates it during initialization.
     * <p> You should not do any kind of modifications inside this directory; as it may lead to failure of XADisk.
     * <p> No two XADisk instances should use the same system directory.
     * @param xaDiskHome new value of xaDiskHome.
     */
    public void setXaDiskHome(String xaDiskHome) {
        this.xaDiskHome = xaDiskHome;
    }

    /**
     * Returns the value of instanceId, a mandatory configuration property.
     * <p> An instance-id uniquely identifies an XADisk instance within a JVM (as multiple XADisk
     * instances can be running within the same JVM).
     * <p> The new XADisk instance (booted directly or via deployment of XADisk JCA Resource Adapter)
     * will be linked to this instanceId.
     * <p> Value of instance-id cannot be null or empty string.
     * @return value of instanceId.
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Sets the value of instanceId, a mandatory configuration property.
     * <p> An instance-id uniquely identifies an XADisk instance within a JVM (as multiple XADisk
     * instances can be running within the same JVM).
     * <p> The new XADisk instance (booted directly or via deployment of XADisk JCA Resource Adapter)
     * will be linked to this instanceId.
     * <p> Value of instance-id cannot be null or empty string.
     * @param instanceId new value of instanceId.
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Returns the value of transactionLogFileMaxSize.
     * <p> This is the maximum size of a transaction log file (in bytes). XADisk maintains a transaction
     * log and rotates the current transaction log if its size exceeds this value.
     * <p> You should set this value according to the maximum file-size allowed by your file-system
     * (in which the XADisk System Directory resides).
     * <p> Default value is 1000000000 (1 GB).
     * @return value of transactionLogFileMaxSize, in bytes.
     */
    public Long getTransactionLogFileMaxSize() {
        return transactionLogFileMaxSize;
    }

    /**
     * Sets the value of transactionLogFileMaxSize.
     * <p> This is the maximum size of a transaction log file (in bytes). XADisk maintains a transaction
     * log and rotates the current transaction log if its size exceeds this value.
     * <p> You should set this value according to the maximum file-size allowed by your file-system
     * (in which the XADisk System Directory resides).
     * <p> Default value is 1000000000 (1 GB).
     * @param transactionLogFileMaxSize new value of transactionLogFileMaxSize.
     */
    public void setTransactionLogFileMaxSize(Long transactionLogFileMaxSize) {
        this.transactionLogFileMaxSize = transactionLogFileMaxSize;
    }

    /**
     * Returns the value of cumulativeBufferSizeForDiskWrite (a performance tuning property).
     * <p> XADisk doesn't write its transaction logs one-by-one separately, to the disk;
     * it does so in big enough batches. This property mentions total size of transaction logs, in bytes,
     * when such a disk write takes place.
     * <p> Default value is 1000000 bytes (1 MB).
     * @return value of cumulativeBufferSizeForDiskWrite, in bytes.
     */
    public Integer getCumulativeBufferSizeForDiskWrite() {
        return cumulativeBufferSizeForDiskWrite;
    }

    /**
     * Sets the value of cumulativeBufferSizeForDiskWrite (a performance tuning property).
     * <p> XADisk doesn't write its transaction logs one-by-one separately, to the disk;
     * it does so in big enough batches. This property mentions total size of transaction logs, in bytes,
     * when such a disk write takes place.
     * <p> Default value is 1000000 bytes (1 MB).
     * @param cumulativeBufferSizeForDiskWrite new value of cumulativeBufferSizeForDiskWrite.
     */
    public void setCumulativeBufferSizeForDiskWrite(Integer cumulativeBufferSizeForDiskWrite) {
        this.cumulativeBufferSizeForDiskWrite = cumulativeBufferSizeForDiskWrite;
    }

    /**
     * Returns the value of directBufferIdleTime (a performance tuning property).
     * <p> This is the number of seconds after which any 'direct' ({@link ByteBuffer#isDirect()})
     * pooled buffer is considered idle if not in use.
     * <p> An idle buffer is freed by a background thread which runs periodically. The frequency
     * of this thread is decided by a property called {@link #getBufferPoolRelieverInterval()
     * bufferPoolRelieverInterval}.
     * <p> Default value is 100 seconds.
     * @return value of directBufferIdleTime, in seconds.
     */
    public Integer getDirectBufferIdleTime() {
        return directBufferIdleTime;
    }

    /**
     * Sets the value of directBufferIdleTime (a performance tuning property).
     * <p> This is the number of seconds after which any 'direct' ({@link ByteBuffer#isDirect()})
     * pooled buffer is considered idle if not in use.
     * <p> An idle buffer is freed by a background thread which runs periodically. The frequency
     * of this thread is decided by a property called {@link #getBufferPoolRelieverInterval()
     * bufferPoolRelieverInterval}.
     * <p> Default value is 100 seconds.
     * @param directBufferIdleTime new value of directBufferIdleTime.
     */
    public void setDirectBufferIdleTime(Integer directBufferIdleTime) {
        this.directBufferIdleTime = directBufferIdleTime;
    }

    /**
     * Returns the value of nonDirectBufferIdleTime (a performance tuning property).
     * <p> This is the number of seconds after which any 'nonDirect' ({@link ByteBuffer#isDirect()})
     * pooled buffer is considered idle if not in use.
     * <p> An idle buffer is freed by a background thread which runs periodically. The frequency
     * of this thread is decided by a property called {@link #getBufferPoolRelieverInterval()
     * bufferPoolRelieverInterval}.
     * <p> Default value is 100 seconds.
     * @return value of nonDirectBufferIdleTime, in seconds.
     */
    public Integer getNonDirectBufferIdleTime() {
        return nonDirectBufferIdleTime;
    }

    /**
     * Sets the value of nonDirectBufferIdleTime (a performance tuning property).
     * <p> This is the number of seconds after which any 'nonDirect' ({@link ByteBuffer#isDirect()})
     * pooled buffer is considered idle if not in use.
     * <p> An idle buffer is freed by a background thread which runs periodically. The frequency
     * of this thread is decided by a property called {@link #getBufferPoolRelieverInterval()
     * bufferPoolRelieverInterval}.
     * <p> Default value is 100 seconds.
     * @param nonDirectBufferIdleTime new value of nonDirectBufferIdleTime.
     */
    public void setNonDirectBufferIdleTime(Integer nonDirectBufferIdleTime) {
        this.nonDirectBufferIdleTime = nonDirectBufferIdleTime;
    }

    /**
     * Returns the value of bufferPoolRelieverInterval.
     * <p> This property decides the time interval in which a service thread for
     * de-allocating the idle buffers in the buffer pool will get run.
     * <p> Default value is 60 seconds.
     * @return value of bufferPoolRelieverInterval, in seconds.
     */
    public Integer getBufferPoolRelieverInterval() {
        return bufferPoolRelieverInterval;
    }

    /**
     * Sets the value of bufferPoolRelieverInterval.
     * <p> This property decides the time interval in which a service thread for
     * de-allocating the idle buffers in the buffer pool will get run.
     * <p> Default value is 60 seconds.
     * @param bufferPoolRelieverInterval new value of bufferPoolRelieverInterval.
     */
    public void setBufferPoolRelieverInterval(Integer bufferPoolRelieverInterval) {
        this.bufferPoolRelieverInterval = bufferPoolRelieverInterval;
    }

    /**
     * Returns the value of maxNonPooledBufferSize (a performance tuning property).
     * <p> XADisk tries to hold its ongoing transactions' logs in memory
     * using {@link ByteBuffer byte-buffers}. Similarly, such {@link ByteBuffer byte-buffers}
     * are also used by i/o streams {@link XAFileInputStream} and {@link XAFileOutputStream}
     * for holding file's contents if the pooled buffers are all exhausted.
     * <p> As these byte-buffers add to the total memory consumption, but at the same
     * time do boost performance, this property can be used to put an upper limit on
     * the total size of these buffers.
     * <p> Default value is 1000000 (1 MB).
     * @return value of maxNonPooledBufferSize.
     */
    public Long getMaxNonPooledBufferSize() {
        return maxNonPooledBufferSize;
    }

    /**
     * Sets the value of maxNonPooledBufferSize (a performance tuning property).
     * <p> XADisk tries to hold its ongoing transactions' logs in memory
     * using {@link ByteBuffer byte-buffers}. Similarly, such {@link ByteBuffer byte-buffers}
     * are also used by i/o streams {@link XAFileInputStream} and {@link XAFileOutputStream}
     * for holding file's contents if the pooled buffers are all exhausted.
     * <p> As these byte-buffers add to the total memory consumption, but at the same
     * time do boost performance, this property can be used to put an upper limit on
     * the total size of these buffers.
     * <p> Default value is 1000000 (1 MB).
     * @param maxNonPooledBufferSize new value of maxNonPooledBufferSize.
     */
    public void setMaxNonPooledBufferSize(Long maxNonPooledBufferSize) {
        this.maxNonPooledBufferSize = maxNonPooledBufferSize;
    }

    /**
     * Returns the value of deadLockDetectorInterval.
     * <p> This is the time interval (in seconds) in which the deadlock detection mechanism gets
     * triggered to detect deadlocks in the system and to take appropriate action to remedy them.
     * <p> Default value is 30 seconds.
     * @return value of deadLockDetectorInterval, in seconds.
     */
    public Integer getDeadLockDetectorInterval() {
        return deadLockDetectorInterval;
    }

    /**
     * Sets the value of deadLockDetectorInterval.
     * <p> This is the time interval (in seconds) in which the deadlock detection mechanism gets
     * triggered to detect deadlocks in the system and to take appropriate action to remedy them.
     * <p> Default value is 30 seconds.
     * @param deadLockDetectorInterval new value of deadLockDetectorInterval.
     */
    public void setDeadLockDetectorInterval(Integer deadLockDetectorInterval) {
        this.deadLockDetectorInterval = deadLockDetectorInterval;
    }

    /**
     * Returns the value of lockTimeOut.
     * <p> This is the time duration (in milliseconds) for which a request to acquire a
     * lock (over a file/directory) will wait if the lock is not immediately available. If the wait
     * time exceeds this value, {@link LockingTimedOutException} is thrown to the application to let it know.
     * <p> Default value is 10000 (10 seconds).
     * @return value of lockTimeOut, in milliseconds.
     */
    public Integer getLockTimeOut() {
        return lockTimeOut;
    }

    /**
     * Sets the value of lockTimeOut.
     * <p> This is the time duration (in milliseconds) for which a request to acquire a
     * lock (over a file/directory) will wait if the lock is not immediately available. If the wait
     * time exceeds this value, {@link LockingTimedOutException} is thrown to the application to let it know.
     * <p> Default value is 10000 (10 seconds).
     * @param lockTimeOut new value of lockTimeOut.
     */
    public void setLockTimeOut(Integer lockTimeOut) {
        this.lockTimeOut = lockTimeOut;
    }

    /**
     * Returns the value of maximumConcurrentEventDeliveries.
     * <p> This is the maximum number of Message Driven Beans invoked by XADisk concurrently
     * to process the {@link FileSystemStateChangeEvent events} from XADisk.
     * <p> Default value is 20.
     * @return value of maximumConcurrentEventDeliveries.
     */
    public Integer getMaximumConcurrentEventDeliveries() {
        return maximumConcurrentEventDeliveries;
    }

    /**
     * Sets the value of maximumConcurrentEventDeliveries.
     * <p> This is the maximum number of Message Driven Beans invoked by XADisk concurrently
     * to process the {@link FileSystemStateChangeEvent events} from XADisk.
     * <p> Default value is 20.
     * @param maximumConcurrentEventDeliveries new value of maximumConcurrentEventDeliveries.
     */
    public void setMaximumConcurrentEventDeliveries(Integer maximumConcurrentEventDeliveries) {
        this.maximumConcurrentEventDeliveries = maximumConcurrentEventDeliveries;
    }

    /**
     * Returns the value of transactionTimeout.
     * <p> This is maximum time (in seconds) for which any transaction in the system will be
     * allowed to remain 'open'.
     * <p> If a transaction times out, it will be rolled-back by the transaction timeout
     * mechanism.
     * <p> Default value is 60 seconds.
     * @return value of transactionTimeout, in seconds.
     */
    public Integer getTransactionTimeout() {
        return transactionTimeout;
    }

    /**
     * Sets the value of transactionTimeout.
     * <p> This is maximum time (in seconds) for which any transaction in the system will be
     * allowed to remain 'open'.
     * <p> If a transaction times out, it will be rolled-back by the transaction timeout
     * mechanism.
     * <p> Default value is 60 seconds.
     * @param transactionTimeout new value of transactionTimeout.
     */
    public void setTransactionTimeout(Integer transactionTimeout) {
        this.transactionTimeout = transactionTimeout;
    }

    /**
     * Returns the value of serverAddress.
     * <p> See the description for {@link #getEnableRemoteInvocations()}.
     * <p> Default value is '127.0.0.1', which will allow the XADisk instance to serve only those applications
     * running on the same machine as the XADisk instance.
     * @return value of serverAddress.
     */
    public String getServerAddress() {
        return serverAddress;
    }

    /**
     * Sets the value of serverAddress.
     * <p> See the description for {@link #getEnableRemoteInvocations()}.
     * <p> Default value is '127.0.0.1', which will allow the XADisk instance to serve only those applications
     * running on the same machine as the XADisk instance.
     * @param serverAddress new value of serverAddress.
     */
    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    /**
     * Returns the value of serverPort.
     * <p> See the description for {@link #getEnableRemoteInvocations()}.
     * <p> Default value is 9999.
     * @return value of serverPort.
     */
    public Integer getServerPort() {
        return serverPort;
    }

    /**
     * Sets the value of serverPort.
     * <p> See the description for {@link #getEnableRemoteInvocations()}.
     * <p> Default value is 9999.
     * @param serverPort new value of serverPort.
     */
    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * Returns the value of enableRemoteInvocations.
     * <p> An XADisk instance can be configured to receive remote invocations. This allows remote application
     * clients to perform operations over the XADisk instance. This feature also allows XADisk JCA Resource
     * Adapter to facilitate inbound messaging from remote XADisk instances to the MDBs deployed in
     * the same JavaEE Server.
     * <p> The following configuration is required to enable an XADisk instance to accept such remote invocations:
     * <ol>
     * <li> enableRemoteInvocations flag must be set to true.
     * <li> {@link #getServerAddress() serverAddress} should be set such that the applications running
     * on the remote JVMs can contact this XADisk instance using this address.
     * <li> {@link #getServerPort() serverPort} should be set to a port available on the machine. The
     * XADisk instance would listen at the specified port to receive calls from remote applications.
     * </ol>
     * <p> Default value is false.
     * @return value of enableRemoteInvocations.
     */
    public Boolean getEnableRemoteInvocations() {
        return enableRemoteInvocations;
    }

    /**
     * Sets the value of enableRemoteInvocations.
     * <p> An XADisk instance can be configured to receive remote invocations. This allows remote application
     * clients to perform operations over the XADisk instance. This feature also allows XADisk JCA Resource
     * Adapter to facilitate inbound messaging from remote XADisk instances to the MDBs deployed in
     * the same JavaEE Server.
     * <p> The following configuration is required to enable an XADisk instance to accept such remote invocations:
     * <ol>
     * <li> enableRemoteInvocations flag must be set to true.
     * <li> {@link #getServerAddress() serverAddress} should be set such that the applications running
     * on the remote JVMs can contact this XADisk instance using this address.
     * <li> {@link #getServerPort() serverPort} should be set to a port available on the machine. The
     * XADisk instance would listen at the specified port to receive calls from remote applications.
     * </ol>
     * <p> Default value is false.
     * @param enableRemoteInvocations new value of enableRemoteInvocations.
     */
    public void setEnableRemoteInvocations(Boolean enableRemoteInvocations) {
        this.enableRemoteInvocations = enableRemoteInvocations;
    }

    /**
     * Returns the value of synchronizeDirectoryChanges.
     * <p> This flag can be used to specify whether the XADisk instance must
     * synchronize directory changes to the disk during transaction commit.
     * <p> If this flag is set to false, directory changes (create/delete children) done
     * inside a transaction may not get synchronized to the disk at commit.
     * <p> Default value is true.
     * @return value of synchronizeDirectoryChanges.
     * @since 1.1
     */
    public Boolean getSynchronizeDirectoryChanges() {
        return this.synchronizeDirectoryChanges;
    }

    /**
     * Sets the value of synchronizeDirectoryChanges.
     * <p> This flag can be used to specify whether the XADisk instance must
     * synchronize directory changes to the disk during transaction commit.
     * <p> If this flag is set to false, directory changes (create/delete children) done
     * inside a transaction may not get synchronized to the disk at commit.
     * <p> Default value is true.
     * @param synchronizeDirectoryChanges new value of synchronizeDirectoryChanges.
     * @since 1.1
     */
    public void setSynchronizeDirectoryChanges(Boolean synchronizeDirectoryChanges) {
        this.synchronizeDirectoryChanges = synchronizeDirectoryChanges;
    }

    /**
     * Returns the value of clusterMasterAddress.
     * <p> See the description for {@link #getEnableClusterMode()}.
     * <p> There is no default value.
     * @return value of clusterMasterAddress.
     * @since 1.2
     */
    public String getClusterMasterAddress() {
        return clusterMasterAddress;
    }

    /**
     * Sets the value of clusterMasterAddress.
     * <p> See the description for {@link #getEnableClusterMode()}.
     * <p> There is no default value.
     * @param clusterMasterAddress new value of clusterMasterAddress.
     * @since 1.2
     */
    public void setClusterMasterAddress(String clusterMasterAddress) {
        this.clusterMasterAddress = clusterMasterAddress;
    }

    /**
     * Returns the value of clusterMasterPort.
     * <p> See the description for {@link #getEnableClusterMode()}.
     * <p> There is no default value.
     * @return value of clusterMasterPort.
     * @since 1.2
     */
    public Integer getClusterMasterPort() {
        return clusterMasterPort;
    }

    /**
     * Sets the value of clusterMasterPort.
     * <p> See the description for {@link #getEnableClusterMode()}.
     * <p> There is no default value.
     * @param clusterMasterPort new value of clusterMasterPort.
     * @since 1.2
     */
    public void setClusterMasterPort(Integer clusterMasterPort) {
        this.clusterMasterPort = clusterMasterPort;
    }

    /**
     * Returns the value of enableClusterMode.
     * <p> An XADisk cluster consists of a set of XADisk instances with exactly one of them
     * being the master instance. To enable XADisk clustering, the following configuration
     * is required:
     * <ol>
     * <li> enableClusterMode flag must be set to true for all instances in the cluster.
     * <li> {@link #getServerAddress() serverAddress}/{@link #getServerPort() serverPort}
     * must be set for the master instance.
     * <li> {@link #getClusterMasterAddress() clusterMasterAddress}/{@link #getClusterMasterPort() clusterMasterPort}
     * must not be set for the master instance. For all other instances in the cluster these values must be
     * set as the values of {@link #getServerAddress() serverAddress}/{@link #getServerPort() serverPort}
     * configured in the master instance.
     * <li> (Optional) For instances running in the same JVM as the master instance,
     * {@link #getClusterMasterAddress() clusterMasterAddress} can optionally be set to "#masterInstanceId".
     * </ol>
     * <p> Default value is false.
     * @return value of enableClusterMode.
     * @since 1.2
     */
    public Boolean getEnableClusterMode() {
        return enableClusterMode;
    }

    /**
     * Sets the value of enableClusterMode.
     * <p> An XADisk cluster consists of a set of XADisk instances with exactly one of them
     * being the master instance. To enable XADisk clustering, the following configuration
     * is required:
     * <ol>
     * <li> enableClusterMode flag must be set to true for all instances in the cluster.
     * <li> {@link #getServerAddress() serverAddress}/{@link #getServerPort() serverPort}
     * must be set for the master instance.
     * <li> {@link #getClusterMasterAddress() clusterMasterAddress}/{@link #getClusterMasterPort() clusterMasterPort}
     * must not be set for the master instance. For all other instances in the cluster these values must be
     * set as the values of {@link #getServerAddress() serverAddress}/{@link #getServerPort() serverPort}
     * configured in the master instance.
     * <li> (Optional) For instances running in the same JVM as the master instance,
     * {@link #getClusterMasterAddress() clusterMasterAddress} can optionally be set to "#masterInstanceId".
     * </ol>
     * <p> Default value is false.
     * @param enableClusterMode new value of enableClusterMode.
     * @since 1.2
     */
    public void setEnableClusterMode(Boolean enableClusterMode) {
        this.enableClusterMode = enableClusterMode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FileSystemConfiguration) {
            FileSystemConfiguration that = (FileSystemConfiguration) obj;
            return this.instanceId.equals(that.instanceId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return instanceId.hashCode();
    }
}
