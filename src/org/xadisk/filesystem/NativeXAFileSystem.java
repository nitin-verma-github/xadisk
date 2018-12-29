/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem;

import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XASession;
import org.xadisk.filesystem.pools.BufferPool;
import org.xadisk.filesystem.utilities.Logger;
import org.xadisk.filesystem.workers.observers.CriticalWorkersListener;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;
import org.xadisk.filesystem.standalone.StandaloneWorkManager;
import org.xadisk.filesystem.workers.CrashRecoveryWorker;
import org.xadisk.filesystem.workers.FileSystemEventDelegator;
import org.xadisk.filesystem.workers.GatheringDiskWriter;
import org.xadisk.filesystem.workers.ObjectPoolReliever;
import org.xadisk.filesystem.workers.TransactionTimeoutDetector;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.connector.inbound.EndPointActivation;
import org.xadisk.filesystem.exceptions.RecoveryInProgressException;
import org.xadisk.filesystem.exceptions.XASystemException;
import org.xadisk.bridge.proxies.facilitators.RemoteMethodInvoker;
import org.xadisk.bridge.proxies.impl.RemoteConcurrencyControl;
import org.xadisk.bridge.proxies.impl.RemoteMessageEndpointFactory;
import org.xadisk.bridge.proxies.impl.RemoteXAFileSystem;
import org.xadisk.bridge.server.conversation.GlobalHostedContext;
import org.xadisk.bridge.server.PointOfContact;
import org.xadisk.connector.XAResourceImpl;
import org.xadisk.connector.inbound.DeadLetterMessageEndpoint;
import org.xadisk.connector.inbound.LocalEventProcessingXAResource;
import org.xadisk.filesystem.exceptions.XASystemBootFailureException;
import org.xadisk.filesystem.exceptions.XASystemNoMoreAvailableException;
import org.xadisk.filesystem.pools.SelectorPool;
import org.xadisk.filesystem.utilities.FileIOUtility;

public class NativeXAFileSystem implements XAFileSystemCommonness {

    private static ConcurrentHashMap<String, NativeXAFileSystem> allXAFileSystems = new ConcurrentHashMap<String, NativeXAFileSystem>();
    private final AtomicLong lastTransactionId = new AtomicLong(System.currentTimeMillis() / 1000);
    private final BufferPool bufferPool;
    private final SelectorPool selectorPool;
    private final String transactionLogFileBaseName;
    private Logger logger;
    private final String transactionLogsDir;
    private final DeadLetterMessageEndpoint deadLetter;
    private final FileSystemConfiguration configuration;
    private final ConcurrentHashMap<TransactionInformation, NativeSession> transactionAndSession =
            new ConcurrentHashMap<TransactionInformation, NativeSession>(1000);
    private HashSet<TransactionInformation> transactionsPreparedPreCrash;
    private boolean returnedAllPreparedTransactions = false;
    private final WorkManager workManager;
    private final GatheringDiskWriter gatheringDiskWriter;
    private final CrashRecoveryWorker recoveryWorker;
    private final ObjectPoolReliever bufferPoolReliever;
    private final ObjectPoolReliever selectorPoolReliever;
    private final FileSystemEventDelegator fileSystemEventDelegator;
    private final TransactionTimeoutDetector transactionTimeoutDetector;
    private final PointOfContact pointOfContact;
    private boolean recoveryComplete = false;
    private final LinkedBlockingQueue<FileSystemStateChangeEvent> fileSystemEventQueue;
    private volatile boolean systemHasFailed = false;
    private volatile Throwable systemFailureCause = null;
    private volatile boolean systemShuttingDown = false;
    private final CriticalWorkersListener workListener;
    private final File backupDirRoot;
    private final int[] backupDirPathNumbers = new int[BACKUP_DIR_PATH_MAX_DEPTH];
    private File currentBackupDirPath;
    private final AtomicInteger currentBackupFileName = new AtomicInteger(0);
    private final GlobalHostedContext globalCallbackContext = new GlobalHostedContext();
    private final AtomicLong totalNonPooledBufferSize = new AtomicLong(0);
    private final ConcurrencyControl concurrencyControl;
    private final boolean handleGeneralRemoteInvocations;
    private final boolean handleClusterRemoteInvocations;
    private final ConcurrentLinkedQueue<TransactionInformation> failedTransactions =
            new ConcurrentLinkedQueue<TransactionInformation>();
    //fix for bug XADISK-85 and potentially similar ones.
    public static final int FILE_CHANNEL_MAX_TRANSFER = 1024 * 1024 * 8;
    private static final int BACKUP_DIR_PATH_MAX_DEPTH = 5;
    private static final int BACKUP_DIR_PATH_MAX_BREADTH = 65000;
    private static final int BACKUP_DIR_MAX_FILES = BACKUP_DIR_PATH_MAX_BREADTH;

    private NativeXAFileSystem(FileSystemConfiguration configuration,
            WorkManager workManager) {
        this.configuration = configuration;
        this.workManager = workManager;
        try {
            File xaDiskHome = new File(configuration.getXaDiskHome()).getAbsoluteFile();
            String xaDiskHomePath = xaDiskHome.getPath();
            FileIOUtility.createDirectoriesIfRequired(xaDiskHome);
            backupDirRoot = new File(xaDiskHome, "backupDir");
            logger = new Logger(new File(xaDiskHome, "xadisk.log"), (byte) 3);

            if (configuration.getSynchronizeDirectoryChanges()) {
                boolean success = DurableDiskSession.setupDirectorySynchronization(xaDiskHome);
                if (!success) {
                    logger.logWarning("XADisk has failed to load its native library "
                            + "required for directory-synchronization.\n"
                            + "Now, it will override the configuration property \"synchronizeDirectoryChanges\" "
                            + "and set it to false; but please note that this would turn-off directory-synchronization i.e. "
                            + "directory modifications may not get synchronized to the disk at transaction commit.\n"
                            + "If you have any questions or think this exception is not expected, please "
                            + "consider discussing in XADisk forums, or raising a bug with details.");
                    configuration.setSynchronizeDirectoryChanges(false);
                }
            }

            DurableDiskSession diskSession = createDurableDiskSession();

            if (!backupDirRoot.isDirectory()) {
                diskSession.createDirectory(backupDirRoot);
            }
            transactionLogsDir = xaDiskHomePath + File.separator + "txnlogs";
            diskSession.createDirectoriesIfRequired(new File(transactionLogsDir));
            transactionLogFileBaseName = transactionLogsDir + File.separator + "xadisk.log";
            bufferPool = new BufferPool(configuration.getDirectBufferPoolSize(), configuration.getNonDirectBufferPoolSize(),
                    configuration.getBufferSize(), configuration.getDirectBufferIdleTime(),
                    configuration.getNonDirectBufferIdleTime(), this);
            selectorPool = new SelectorPool(1000);
            gatheringDiskWriter = new GatheringDiskWriter(configuration.getCumulativeBufferSizeForDiskWrite(),
                    configuration.getTransactionLogFileMaxSize(), configuration.getMaxNonPooledBufferSize(),
                    transactionLogFileBaseName, this);
            recoveryWorker = new CrashRecoveryWorker(this);
            bufferPoolReliever = new ObjectPoolReliever(bufferPool, configuration.getBufferPoolRelieverInterval(), this);
            selectorPoolReliever = new ObjectPoolReliever(selectorPool, 1000, this);
            transactionTimeoutDetector = new TransactionTimeoutDetector(1, this);
            this.fileSystemEventQueue = new LinkedBlockingQueue<FileSystemStateChangeEvent>();
            this.fileSystemEventDelegator = new FileSystemEventDelegator(this, configuration.getMaximumConcurrentEventDeliveries());
            this.workListener = new CriticalWorkersListener(this);
            File deadLetterDir = new File(xaDiskHome, "deadletter");
            diskSession.createDirectoriesIfRequired(deadLetterDir);
            this.deadLetter = new DeadLetterMessageEndpoint(deadLetterDir, this);

            diskSession.forceToDisk();

            if (configuration.getEnableClusterMode()) {
                if (isValidString(configuration.getClusterMasterAddress())) {
                    handleClusterRemoteInvocations = false;
                    String clusterMasterAddress = configuration.getClusterMasterAddress();
                    if (clusterMasterAddress.charAt(0) == '#') {
                        String clusterMasterInstanceId = clusterMasterAddress.substring(1);
                        concurrencyControl = getXAFileSystem(clusterMasterInstanceId).getConcurrencyControl();
                    } else {
                        Integer clusterMasterPort = configuration.getClusterMasterPort();
                        concurrencyControl = new RemoteConcurrencyControl(clusterMasterAddress, clusterMasterPort);
                    }
                } else {
                    handleClusterRemoteInvocations = true;
                    concurrencyControl = new NativeConcurrencyControl(configuration, workManager, workListener, this);
                }
            } else {
                handleClusterRemoteInvocations = false;
                concurrencyControl = new NativeConcurrencyControl(configuration, workManager, workListener, this);
            }

            workManager.startWork(bufferPoolReliever, WorkManager.INDEFINITE, null, workListener);
            workManager.startWork(selectorPoolReliever, WorkManager.INDEFINITE, null, workListener);
            workManager.startWork(fileSystemEventDelegator, WorkManager.INDEFINITE, null, workListener);
            workManager.startWork(transactionTimeoutDetector, WorkManager.INDEFINITE, null, workListener);

            handleGeneralRemoteInvocations = configuration.getEnableRemoteInvocations();
            if (handleClusterRemoteInvocations || handleGeneralRemoteInvocations) {
                pointOfContact = new PointOfContact(this, configuration.getServerPort());
                workManager.startWork(pointOfContact, WorkManager.INDEFINITE, null, workListener);
            } else {
                pointOfContact = null;
            }

            recoveryWorker.collectRecoveryData();
            gatheringDiskWriter.initialize();
            workManager.startWork(gatheringDiskWriter, WorkManager.INDEFINITE, null, workListener);
            workManager.startWork(recoveryWorker, WorkManager.INDEFINITE, null, workListener);

        } catch (Exception e) {
            XASystemBootFailureException xasbfe = new XASystemBootFailureException(e);
            if (logger != null) {
                logger.logThrowable(xasbfe, Logger.ERROR);
            }
            throw xasbfe;
        }
    }

    public static NativeXAFileSystem bootXAFileSystem(FileSystemConfiguration configuration,
            WorkManager workManager) {
        doBasicValidationForConfiguration(configuration);
        String instanceId = configuration.getInstanceId();
        if (allXAFileSystems.get(instanceId) != null) {
            throw new XASystemBootFailureException("An instance of XADisk with instance-id [" + instanceId + "] is already"
                    + " running in this JVM.");
        }
        NativeXAFileSystem newXAFileSystem = new NativeXAFileSystem(configuration, workManager);
        allXAFileSystems.put(configuration.getInstanceId(), newXAFileSystem);
        newXAFileSystem.logger.logInfo("Successfully booted the XADisk instance.");
        return newXAFileSystem;
    }

    public static NativeXAFileSystem bootXAFileSystemStandAlone(StandaloneFileSystemConfiguration configuration) {
        doBasicValidationForConfiguration(configuration);
        String instanceId = configuration.getInstanceId();
        if (allXAFileSystems.get(instanceId) != null) {
            throw new XASystemBootFailureException("An instance of XADisk with instance-id [" + instanceId + "] is already"
                    + " running in this JVM.");
        }
        WorkManager workManager = new StandaloneWorkManager(
                configuration.getWorkManagerCorePoolSize(), configuration.getWorkManagerMaxPoolSize(),
                configuration.getWorkManagerKeepAliveTime());
        NativeXAFileSystem newXAFileSystem = new NativeXAFileSystem(configuration, workManager);
        allXAFileSystems.put(configuration.getInstanceId(), newXAFileSystem);
        newXAFileSystem.logger.logInfo("Successfully booted the XADisk instance.");
        return newXAFileSystem;
    }

    public static NativeXAFileSystem getXAFileSystem(String instanceId) {
        return allXAFileSystems.get(instanceId);
    }

    public boolean pointToSameXAFileSystem(XAFileSystem xaFileSystem) {
        if (xaFileSystem instanceof NativeXAFileSystem && !(xaFileSystem instanceof RemoteXAFileSystem)) {
            NativeXAFileSystem that = (NativeXAFileSystem) xaFileSystem;
            return this.configuration.getInstanceId().equals(that.configuration.getInstanceId());
        } else {
            return false;
        }
    }

    private static void doBasicValidationForConfiguration(FileSystemConfiguration configuration) {
        if (!isValidString(configuration.getXaDiskHome())) {
            throw new XASystemBootFailureException("Invalid value of configuration property [xaDiskHome]");
        }
        if (!isValidString(configuration.getInstanceId())) {
            throw new XASystemBootFailureException("Invalid value of configuration property [instanceId]");
        }
    }

    private static boolean isValidString(String s) {
        return s != null && s.trim().length() > 0;
    }

    public void notifyRecoveryComplete() throws IOException {
        fileSystemEventQueue.addAll(recoveryWorker.getEventsEnqueueCommittedNotDequeued());
        DurableDiskSession diskSession = createDurableDiskSession();
        diskSession.deleteDirectoryRecursively(backupDirRoot);
        diskSession.createDirectory(backupDirRoot);
        for (int i = 0; i < BACKUP_DIR_PATH_MAX_DEPTH; i++) {
            backupDirPathNumbers[i] = 0;
        }
        currentBackupFileName.set(0);
        setBackupDirPath();
        diskSession.createDirectoriesIfRequired(currentBackupDirPath);
        diskSession.forceToDisk();
        recoveryComplete = true;
    }

    private void setBackupDirPath() {
        StringBuilder dirPath = new StringBuilder(backupDirRoot.getAbsolutePath());
        for (int i = 0; i < BACKUP_DIR_PATH_MAX_DEPTH; i++) {
            dirPath.append(File.separator).append(backupDirPathNumbers[i]);
        }
        currentBackupDirPath = new File(dirPath.toString());
    }

    private void createNextBackupDirPath() throws IOException {
        for (int i = BACKUP_DIR_PATH_MAX_DEPTH - 1; i >= 0; i--) {
            if (++backupDirPathNumbers[i] == BACKUP_DIR_PATH_MAX_BREADTH) {
                backupDirPathNumbers[i] = 0;
            } else {
                break;
            }
        }
        setBackupDirPath();
        DurableDiskSession durableDiskSession = createDurableDiskSession();
        durableDiskSession.createDirectoriesIfRequired(currentBackupDirPath);
        durableDiskSession.forceToDisk();
    }

    public NativeSession createSessionForLocalTransaction() {
        checkIfCanContinue();
        NativeSession session = new NativeSession(TransactionInformation.getXidInstanceForLocalTransaction(getNextLocalTransactionId()), false, this);
        return session;
    }

    public NativeSession createSessionForXATransaction(Xid xid) {
        checkIfCanContinue();
        NativeSession session = new NativeSession((TransactionInformation) xid, false, this);
        return session;
    }

    public XASession createSessionForXATransaction() {
        checkIfCanContinue();
        return new NativeXASession(this, configuration.getInstanceId());
    }

    public XAResource getXAResourceForRecovery() {
        return new XAResourceImpl(new NativeXASession(this, configuration.getInstanceId()));
    }

    public NativeSession getSessionForTransaction(Xid xid) {
        NativeSession session;
        session = transactionAndSession.get((TransactionInformation) xid);
        if (session != null) {
            return session;
        }
        if (transactionsPreparedPreCrash != null
                && transactionsPreparedPreCrash.contains((TransactionInformation) xid)) {
            ArrayList<FileSystemStateChangeEvent> events =
                    recoveryWorker.getEventsFromPreparedTransaction((TransactionInformation) xid);
            if (events != null) {
                session = new NativeSession((TransactionInformation) xid, events, this);
            } else {
                session = new NativeSession((TransactionInformation) xid, true, this);
            }
            if (session != null) {
                return session;
            }
        }
        return null;
    }

    void removeTransactionSessionEntry(TransactionInformation xid) {
        transactionAndSession.remove(xid);
    }

    void assignSessionToTransaction(TransactionInformation xid, NativeSession session) {
        transactionAndSession.put(xid, session);
    }

    public NativeSession[] getAllSessions() {
        Collection<NativeSession> sessions = transactionAndSession.values();
        return sessions.toArray(new NativeSession[0]);
    }

    public NativeSession createRecoverySession(TransactionInformation xid, ArrayList<FileSystemStateChangeEvent> events) {
        if (events == null) {
            return new NativeSession(xid, true, this);
        } else {
            return new NativeSession(xid, events, this);
        }
    }

    //todo : confirm that recover on XAR/here will be called only by one thread and not in parallel by more
    //then one thread.
    public Xid[] recover(int flag) throws XAException {
        if (flag == XAResource.TMSTARTRSCAN) {
            returnedAllPreparedTransactions = false;
        }
        if (flag == (XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN)) {
            returnedAllPreparedTransactions = false;
        }
        if (returnedAllPreparedTransactions) {
            return new Xid[0];
        }
        this.transactionsPreparedPreCrash = recoveryWorker.getPreparedInDoubtTransactions();
        Xid xids[];
        xids = transactionsPreparedPreCrash.toArray(new Xid[0]);
        returnedAllPreparedTransactions = true;
        return xids;
    }

    public void notifyTransactionFailure(TransactionInformation xid) {
        failedTransactions.add(xid);
    }

    public byte[][] getIdentifiersForFailedTransactions() {
        TransactionInformation identifiers[] = failedTransactions.toArray(new TransactionInformation[0]);
        byte identifiersBytes[][] = new byte[identifiers.length][];
        int i = 0;
        for (TransactionInformation identifier : identifiers) {
            identifiersBytes[i++] = identifier.getBytes();
        }
        return identifiersBytes;
    }

    public void declareTransactionAsComplete(byte[] transactionIdentifier) {
        try {
            TransactionInformation xid = new TransactionInformation(ByteBuffer.wrap(transactionIdentifier));
            gatheringDiskWriter.transactionCompletes(xid, true);
            NativeSession session = transactionAndSession.get(xid);
            if (session != null) {
                //the xadisk has not gone down after failure.
                session.completeTheTransaction();
            } else {
                //case of recovery going on.
                recoveryWorker.cleanupTransactionInfo(xid);
            }
            failedTransactions.remove(xid);
        } catch (IOException ioe) {
            notifySystemFailure(ioe);
        }
    }

    public BufferPool getBufferPool() {
        return bufferPool;
    }

    public SelectorPool getSelectorPool() {
        return selectorPool;
    }

    public GlobalHostedContext getGlobalCallbackContext() {
        return globalCallbackContext;
    }

    public GatheringDiskWriter getTheGatheringDiskWriter() {
        return gatheringDiskWriter;
    }

    public String getTransactionLogFileBaseName() {
        return transactionLogFileBaseName;
    }

    public String getTransactionLogsDir() {
        return transactionLogsDir;
    }

    public CrashRecoveryWorker getRecoveryWorker() {
        return recoveryWorker;
    }

    public int getConfiguredBufferSize() {
        return configuration.getBufferSize();
    }

    public WorkManager getWorkManager() {
        return workManager;
    }

    public ArrayList<EndPointActivation> getAllActivations() {
        return fileSystemEventDelegator.getAllActivations();
    }

    public void startWork(Work work) throws WorkException {
        workManager.startWork(work, WorkManager.INDEFINITE, null, workListener);
    }

    public ConcurrencyControl getConcurrencyControl() {
        if (concurrencyControl instanceof RemoteConcurrencyControl) {
            return ((RemoteConcurrencyControl) concurrencyControl).getNewInstance();
        }
        return concurrencyControl;
    }

    public long getNextLocalTransactionId() {
        return lastTransactionId.getAndIncrement();
    }

    Logger getLogger() {
        return logger;
    }

    public void shutdown() throws IOException {
        logger.logInfo("Shutting down the XADisk instance...");
        systemShuttingDown = true;
        NativeSession allSessions[];
        Collection<NativeSession> sessionsCollection = transactionAndSession.values();
        allSessions = sessionsCollection.toArray(new NativeSession[0]);
        for (int i = 0; i < allSessions.length; i++) {
            allSessions[i].notifySystemShutdown();
        }

        bufferPoolReliever.release();
        selectorPoolReliever.release();
        concurrencyControl.shutdown();
        recoveryWorker.release();
        gatheringDiskWriter.release();
        gatheringDiskWriter.deInitialize();
        fileSystemEventDelegator.release();
        transactionTimeoutDetector.release();
        if (getHandleGeneralRemoteInvocations() || getHandleClusterRemoteInvocations()) {
            pointOfContact.release();
        }
        deadLetter.release();
        if (workManager instanceof StandaloneWorkManager) {
            ((StandaloneWorkManager) workManager).shutdown();
        }
        allXAFileSystems.remove(this.configuration.getInstanceId());
        logger.logInfo("Successfully shutdown the XADisk instance.");
        logger.release();
    }

    int getLockTimeOut() {
        return configuration.getLockTimeOut();
    }

    public File getNextBackupFileName() throws IOException {
        File savedCurrentBackupDir = this.currentBackupDirPath;

        int nextBackupFileName = currentBackupFileName.getAndIncrement();
        if (nextBackupFileName >= BACKUP_DIR_MAX_FILES - 1) {
            if (nextBackupFileName == BACKUP_DIR_MAX_FILES - 1) {
                createNextBackupDirPath();
                currentBackupFileName.set(0);
            } else {
                while (currentBackupFileName.get() >= BACKUP_DIR_MAX_FILES - 1) {
                    Thread.yield();
                }
                return getNextBackupFileName();
            }
        }
        return new File(savedCurrentBackupDir, nextBackupFileName + "");
    }

    public LinkedBlockingQueue<FileSystemStateChangeEvent> getFileSystemEventQueue() {
        return fileSystemEventQueue;
    }

    public final DurableDiskSession createDurableDiskSession() {
        return new DurableDiskSession(configuration.getSynchronizeDirectoryChanges());
    }

    public void registerEndPointActivation(EndPointActivation activation) throws IOException {
        boolean notADuplicateActivation = fileSystemEventDelegator.registerActivation(activation);
        if (notADuplicateActivation && activation.getMessageEndpointFactory() instanceof RemoteMessageEndpointFactory) {
            gatheringDiskWriter.recordEndPointActivation(activation);
            ((RemoteMessageEndpointFactory) activation.getMessageEndpointFactory()).setLocalXAFileSystem(this);
        }
    }

    public void deRegisterEndPointActivation(EndPointActivation activation) throws IOException {
        fileSystemEventDelegator.deRegisterActivation(activation);
        if (activation.getMessageEndpointFactory() instanceof RemoteMessageEndpointFactory) {
            gatheringDiskWriter.recordEndPointDeActivation(activation);
        }
    }

    FileSystemEventDelegator getFileSystemEventDelegator() {
        return fileSystemEventDelegator;
    }

    public void notifySystemFailure(Throwable systemFailureCause) {
        this.systemHasFailed = true;
        this.systemFailureCause = systemFailureCause;
        NativeSession allSessions[];
        Collection<NativeSession> sessionsCollection = transactionAndSession.values();
        allSessions = sessionsCollection.toArray(new NativeSession[0]);
        for (int i = 0; i < allSessions.length; i++) {
            allSessions[i].notifySystemFailure(systemFailureCause);
        }
        throw new XASystemNoMoreAvailableException(systemFailureCause);
    }

    public void notifySystemFailureAndContinue(Throwable systemFailureCause) {
        try {
            notifySystemFailure(systemFailureCause);
        } catch (XASystemException xase) {
        }
    }

    private void checkIfCanContinue() {
        if (systemHasFailed) {
            throw new XASystemNoMoreAvailableException(systemFailureCause);
        }
        if (!recoveryComplete) {
            throw new RecoveryInProgressException();
        }
        if (systemShuttingDown) {
            throw new XASystemNoMoreAvailableException();
        }
    }

    public void waitForBootup(long timeout) throws InterruptedException {
        if (timeout < 0) {
            while (true) {
                try {
                    checkIfCanContinue();
                    break;
                } catch (RecoveryInProgressException ripe) {
                    Thread.sleep(1000);
                }
            }
        } else {
            long timer = timeout;
            while (timer > 0) {
                try {
                    checkIfCanContinue();
                    break;
                } catch (RecoveryInProgressException ripe) {
                    Thread.sleep(1000);
                    timer -= 1000;
                }
            }
            checkIfCanContinue(); //to let the exception propagate to caller.
        }
    }

    public int getDefaultTransactionTimeout() {
        return configuration.getTransactionTimeout();
    }

    public String getXADiskSystemId() {
        return configuration.getServerAddress() + "_" + configuration.getServerPort();
    }

    public RemoteMethodInvoker createRemoteMethodInvokerToSelf() {
        return new RemoteMethodInvoker(configuration.getServerAddress(), configuration.getServerPort());
    }

    public XAResource getEventProcessingXAResourceForRecovery() {
        return new LocalEventProcessingXAResource(this);
    }

    public DeadLetterMessageEndpoint getDeadLetter() {
        return deadLetter;
    }

    public void changeTotalNonPooledBufferSize(int changeAmount) {
        totalNonPooledBufferSize.addAndGet(changeAmount);
    }

    public long getTotalNonPooledBufferSize() {
        return totalNonPooledBufferSize.get();
    }

    public static long maxTransferToChannel(long upperLimitOnBytes) {
        return Math.min(upperLimitOnBytes, FILE_CHANNEL_MAX_TRANSFER);
    }

    public boolean getHandleClusterRemoteInvocations() {
        return handleClusterRemoteInvocations;
    }

    public boolean getHandleGeneralRemoteInvocations() {
        return handleGeneralRemoteInvocations;
    }
}
