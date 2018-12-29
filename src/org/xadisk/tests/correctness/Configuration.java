/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.tests.correctness;

import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Properties;

public class Configuration {

    private static final String xadiskSystemDirectory;
    private static final String testRootDirectory;
    private static int nextServerPort;

    static {
        Properties properties = new Properties();
        try {
            InputStream configurationStream = ClassLoader.getSystemResourceAsStream("org/xadisk/tests/correctness/configuration.properties");
            properties.load(configurationStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        xadiskSystemDirectory = properties.getProperty("XADISK_SYSTEM_DIRECTORY");
        testRootDirectory = properties.getProperty("TEST_ROOT_DIRECTORY");
        nextServerPort = Integer.valueOf(properties.getProperty("FIRST_SERVER_PORT"));
    }

    public static String getTestRootDirectory() {
        return testRootDirectory;
    }

    public static String getXADiskSystemDirectory() {
        return xadiskSystemDirectory;
    }

    public static int getNextServerPort() {
        while (true) {
            try {
                ServerSocket serverSocket = new ServerSocket(nextServerPort);
                serverSocket.close();
                break;
            } catch (Exception e) {
                nextServerPort++;
            }
        }
        return nextServerPort++;
    }
}
