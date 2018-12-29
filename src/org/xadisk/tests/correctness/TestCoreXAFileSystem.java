/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.tests.correctness;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.Connector.StringArgument;
import com.sun.jdi.connect.LaunchingConnector;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

public class TestCoreXAFileSystem {

    private static final String SEPERATOR = File.separator;
    private static final String topLevelTestDirectory = Configuration.getTestRootDirectory();
    private static final String XADiskSystemDirectory = Configuration.getXADiskSystemDirectory();
    private static final String forRunningTests = "forRunningTests";
    private static final String transactionDemarcatingThread = "TestThreadObservedByJDI";
    static boolean testCrashRecovery = false;
    static int concurrencyLevel = 1;
    static int numberOfCrashes = 100;
    static int maxConcurrentDeliveries = 1;

    public static void main(String args[]) {
        try {
            if (args.length > 0 && args[0].equals(forRunningTests)) {
                System.out.println("Entered into the main of childJVM " + forRunningTests);
                test(false);
                System.out.println("Exit...");
            } else {
                if (testCrashRecovery) {
                    System.out.println("_____________Start-CrashRecoveryTests______________");
                    for (int i = 1; i <= numberOfCrashes; i++) {
                        TestUtility.cleanupDirectory(new File(XADiskSystemDirectory));
                        TestUtility.cleanupDirectory(new File(topLevelTestDirectory));
                        System.out.println("Raising child JVM for controlled crash...");
                        Process controlledJVM = powerOnJVMAsDebugeeForCrashes(forRunningTests, i);
                        int status = controlledJVM.waitFor();
                        if (status == 0) {
                            break;
                        }
                        System.out.println("Crashed!! Status=" + status);
                        test(true);
                        System.out.println("_______________Recovered Successfully______________");
                    }
                } else {
                    TestUtility.cleanupDirectory(new File(XADiskSystemDirectory));
                    TestUtility.cleanupDirectory(new File(topLevelTestDirectory));
                    test(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static VirtualMachine powerOnJVMAsDebugee(String purpose)
            throws Exception {
        LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
        Map<String, ? extends Argument> connectorArguments = connector.defaultArguments();
        StringArgument mainArgument = (StringArgument) connectorArguments.get("main");
        mainArgument.setValue(org.xadisk.tests.correctness.TestCoreXAFileSystem.class.getName() + " " + purpose);
        StringArgument optionsArgument = (StringArgument) connectorArguments.get("options");
        optionsArgument.setValue("-classpath build" + SEPERATOR + "classes;"
                + "connector-api-1.5.jar;javaee-api-5.jar");
        return connector.launch(connectorArguments);
    }

    private static void initiateOutputProcessing(Process jvmProcess) {
        new Thread(new ChildProcessOutputStreamReader(jvmProcess.getErrorStream(),
                System.err)).start();
        new Thread(new ChildProcessOutputStreamReader(jvmProcess.getInputStream(),
                System.out)).start();
    }

    private static Process powerOnJVMAsDebugeeForCrashes(String purpose, int crashAfterBreakpoint)
            throws Exception {
        VirtualMachine vm = powerOnJVMAsDebugee(purpose);
        new Thread(new JVMCrashTrigger(vm, transactionDemarcatingThread, crashAfterBreakpoint)).start();
        Process jvmProcess = vm.process();
        initiateOutputProcessing(jvmProcess);
        return jvmProcess;
    }

    private static void test(boolean postCrash) {
        try {
            StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration(XADiskSystemDirectory, "local");
            configuration.setWorkManagerCorePoolSize(10);
            configuration.setWorkManagerMaxPoolSize(10000);
            configuration.setMaximumConcurrentEventDeliveries(maxConcurrentDeliveries);
            configuration.setTransactionTimeout(Integer.MAX_VALUE);
            configuration.setDeadLockDetectorInterval(2);
            configuration.setLockTimeOut(10 * 1000);
            configuration.setServerAddress("localhost");
            configuration.setServerPort(Configuration.getNextServerPort());
            configuration.setEnableRemoteInvocations(true);
            if (postCrash) {
                //the cleanup of xadisk system dir was failing due to the loaded native-lib from the system dir.
                configuration.setSynchronizeDirectoryChanges(false);
            }
            NativeXAFileSystem nativeXAFileSystem = NativeXAFileSystem.bootXAFileSystemStandAlone(configuration);
            nativeXAFileSystem.waitForBootup(-1);

            System.out.println("Recovery over.");
            Class.forName(org.xadisk.filesystem.NativeSession.class.getCanonicalName());
            Class.forName(org.xadisk.filesystem.workers.GatheringDiskWriter.class.getName());
            ArrayList<Thread> allThreads = new ArrayList<Thread>();
            Thread tests[] = new Thread[4];

            for (int testReplica = 1; testReplica <= concurrencyLevel; testReplica++) {
                int threadIndex = 0;
                for (CoreXAFileSystemTests.testNames testName : CoreXAFileSystemTests.testNames.values()) {
                    if (testName.name().contains("Crash") && postCrash
                            || !testName.name().contains("Crash") && !postCrash) {
                        String testDirectory = testName.toString();
                        if (postCrash) {
                            testDirectory = testDirectory.substring(0, testDirectory.length() - 9);
                        }
                        tests[threadIndex++] = new Thread(new RunnableTest(testName,
                                topLevelTestDirectory + SEPERATOR + testDirectory + testReplica));
                    }
                }
                for (int i = 0; i < 4; i++) {
                    if (i != 0) {
                        continue;
                    }
                    tests[i].setName(transactionDemarcatingThread);
                    tests[i].start();
                    allThreads.add(tests[i]);
                }
            }
            TestUtility.waitForAllAtHeaven(allThreads);
            System.out.println("Testing threads completed...Will shutdown the NativeXAFS now.");
            XAFileSystemProxy.getNativeXAFileSystemReference("local").shutdown();

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
