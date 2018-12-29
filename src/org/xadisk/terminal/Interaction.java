/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.terminal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import org.xadisk.bridge.proxies.impl.RemoteXAFileSystem;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.SessionCommonness;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

public class Interaction {

    private static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    private static String currentWorkingDirectory = System.getProperty("user.dir");
    private static Session session = null;
    private static XAFileSystem xaFileSystem = null;
    private static String NEW_LINE = System.getProperty("line.separator");
    private static String STARS = "****************";

    public static void main(String args[]) throws Exception {
        printFormatted(NEW_LINE + STARS + "Welcome to the XADisk Terminal Utility" + STARS + NEW_LINE);
        while (true) {
            try {
                prompt("Connect to an already running XADisk on a local or remote machine (y/n) ? : ");
                boolean isRemote = readBoolean();
                if (isRemote) {
                    prompt("Enter the Address of the machine on which the remote XADisk is running : ");
                    String serverAddress = readNonEmptyString();
                    prompt("Enter the Port Number on which the remote XADisk is listening : ");
                    int serverPort = readInteger();
                    xaFileSystem = new RemoteXAFileSystem(serverAddress, serverPort);
                } else {
                    System.out.println("You have chosen to boot a new XADisk.");
                    printFormatted("Please enter a directory path which can be used by XADisk internally to "
                            + "keep the files required for its operations. Note that this directory should not "
                            + "be used by any other program or any other instance of XADisk.");
                    prompt("Specify the directory : ");
                    String xaSystemDirectory = readNonEmptyString();
                    printFormatted("Enter the IP-Address/Host-Name and Port of this machine for use by XADisk. "
                            + "This information will be used during conversation of this XADisk instance with "
                            + "remote client applications.");
                    prompt("Specify the IP-Address/Host-Name : ");
                    String localAddress = readNonEmptyString();
                    prompt("And, the Port : ");
                    int localPort = readInteger();
                    StandaloneFileSystemConfiguration configuration =
                            new StandaloneFileSystemConfiguration(xaSystemDirectory, "");
                    configuration.setServerAddress(localAddress);
                    configuration.setServerPort(localPort);
                    configuration.setTransactionTimeout(Integer.MAX_VALUE);
                    xaFileSystem = NativeXAFileSystem.bootXAFileSystemStandAlone(configuration);
                }
                System.out.println(NEW_LINE + "Waiting for the initialization to complete...");
                xaFileSystem.waitForBootup(-1);
                break;
            } catch (Throwable t) {
                printFormatted(errorToTerminal(t));
                continue;
            }
        }
        System.out.println("Ready.");
        displayHelp();
        while (true) {
            try {
                prompt("$XADisk " + currentWorkingDirectory + "$ ");
                executeNextCommand();
            } catch (Throwable t) {
                printFormatted(errorToTerminal(t));
            }
        }
    }

    private static String normalizeInput(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }

    private static void executeNextCommand() throws Exception {
        String s = normalizeInput(br.readLine());
        if (s.equals("")) {
            return;
        }
        if (s.equals("?") || s.equalsIgnoreCase("help")) {
            displayHelp();
            return;
        }
        ArrayList<String> argsArr = new ArrayList<String>();
        String quoted[] = s.split("\"");
        for (int i = 0; i < quoted.length; i++) {
            if (i % 2 == 0) {
                String spaced[] = quoted[i].split("\\s+");
                argsArr.addAll(Arrays.asList(spaced));
            } else {
                argsArr.add(quoted[i]);
            }
        }
        String args[] = argsArr.toArray(new String[0]);
        //coming here means args.length > 0.
        if (args[0].equalsIgnoreCase("cd")) {
            File f;
            if (args.length == 1) {
                f = getAbsoluteFile(System.getProperty("user.dir"));
            } else {
                f = getAbsoluteFile(args[1]);
            }
            boolean exists = false;
            if (session == null) {
                exists |= f.isDirectory();
            } else {
                exists |= session.fileExistsAndIsDirectory(f, false);
            }
            //because it won't make any sense to switch to dir which doesn't exist in session
            //or system, because all relative path under that directory are going to be non-existent
            //and non-creatable.
            if (!exists) {
                throw new InvalidInputException("No such directory on the file-system", null);
            }
            currentWorkingDirectory = f.getCanonicalPath();
        } else if (args[0].equalsIgnoreCase("create")) {
            assertSession();
            assertArgumentsCount(args, 3);
            boolean isDir = false;
            if (args[1].equalsIgnoreCase("file")) {
                isDir = false;
            } else if (args[1].equalsIgnoreCase("dir")) {
                isDir = true;
            } else {
                throw new InvalidInputException("Valid options for CREATE are FILE and DIR.", null);
            }
            session.createFile(getAbsoluteFile(args[2]), isDir);
        } else if (args[0].equalsIgnoreCase("delete")) {
            assertSession();
            assertArgumentsCount(args, 2);
            session.deleteFile(getAbsoluteFile(args[1]));
        } else if (args[0].equalsIgnoreCase("list")) {
            assertSession();
            File f;
            if (args.length == 1) {
                f = new File(currentWorkingDirectory);
            } else {
                f = getAbsoluteFile(args[1]);
            }
            String files[] = session.listFiles(f, true);
            for (int i = 0; i < files.length; i++) {
                System.out.println(files[i]);
            }
        } else if (args[0].equalsIgnoreCase("copy")) {
            assertSession();
            assertArgumentsCount(args, 3);
            session.copyFile(getAbsoluteFile(args[1]), getAbsoluteFile(args[2]));
        } else if (args[0].equalsIgnoreCase("move")) {
            assertSession();
            assertArgumentsCount(args, 3);
            session.moveFile(getAbsoluteFile(args[1]), getAbsoluteFile(args[2]));
        } else if (args[0].equalsIgnoreCase("exists")) {
            assertSession();
            assertArgumentsCount(args, 2);
            System.out.println(session.fileExists(getAbsoluteFile(args[1]), true) + "");
        } else if (args[0].equalsIgnoreCase("isdir")) {
            assertSession();
            assertArgumentsCount(args, 2);
            System.out.println(session.fileExistsAndIsDirectory(getAbsoluteFile(args[1]), true) + "");
        } else if (args[0].equalsIgnoreCase("length")) {
            assertSession();
            assertArgumentsCount(args, 2);
            System.out.println(session.getFileLength(getAbsoluteFile(args[1]), true) + "");
        } else if (args[0].equalsIgnoreCase("truncate")) {
            assertSession();
            assertArgumentsCount(args, 3);
            session.truncateFile(getAbsoluteFile(args[1]), Integer.valueOf(args[2]));
        } else if (args[0].equalsIgnoreCase("read")) {
            assertSession();
            assertArgumentsCount(args, 4);
            int position = Integer.valueOf(args[2]);
            int count = Integer.valueOf(args[3]);
            byte buffer[] = new byte[count];
            XAFileInputStream xafis = session.createXAFileInputStream(getAbsoluteFile(args[1]), true);
            try {
                xafis.position(position);
                int totalRead = 0;
                while (totalRead < count) {
                    int numRead = xafis.read(buffer);
                    if (numRead != -1) {
                        printFormatted(new String(buffer, 0, numRead, "utf8"));
                    } else {
                        break;
                    }
                    totalRead += numRead;
                }
            } finally {
                xafis.close();
            }
        } else if (args[0].equalsIgnoreCase("append")) {
            assertSession();
            assertArgumentsCount(args, 2);
            XAFileOutputStream xafos = session.createXAFileOutputStream(getAbsoluteFile(args[1]), false);
            try {
                while (true) {
                    String data = br.readLine();
                    if (data == null) {
                        //end of transmission : this does not mean that System.in is closed. It may or may not 
                        //have closed. The inputting of EOT is environment dependent : in windows its CtrlZ,
                        //on unix its CtrlD.
                        break;
                    }
                    data = data + NEW_LINE;
                    xafos.write(data.getBytes());//default encoding.
                }
            } finally {
                xafos.close();
            }
        } else if (args[0].equalsIgnoreCase("begin")) {
            assertArgumentsCount(args, 1);
            if (session != null) {
                printFormatted("There is a current transaction already associated. You can't start a new "
                        + "transaction without committing/rolling back this transaction.");
            } else {
                session = xaFileSystem.createSessionForLocalTransaction();
            }
        } else if (args[0].equalsIgnoreCase("commit")) {
            assertArgumentsCount(args, 1);
            assertSession();
            ((SessionCommonness) session).commit(true);
            session = null;
        } else if (args[0].equalsIgnoreCase("rollback")) {
            assertArgumentsCount(args, 1);
            assertSession();
            session.rollback();
            session = null;
        } else if (args[0].equalsIgnoreCase("shutdown")) {
            assertArgumentsCount(args, 1);
            if (session != null) {
                System.out.println("There is a current transaction already associated. Please commit/rollback that first.");
            } else {
                if (xaFileSystem instanceof RemoteXAFileSystem) {
                    System.out.println("Will not shutdown the remote XADisk instance. Only disconnecting from it..");
                    xaFileSystem.shutdown();
                } else {
                    System.out.println("Shutting down the XADisk instance started during this session..");
                    xaFileSystem.shutdown();
                }
                System.exit(0);
            }
        } else {
            System.out.println("Sorry, didn't recognize the command.");
        }
    }

    private static File getAbsoluteFile(String path) throws IOException {
        File f = new File(path);
        if (f.isAbsolute()) {
            return f.getCanonicalFile();
        } else {
            return new File(currentWorkingDirectory, path).getCanonicalFile();
        }
    }

    private static void displayHelp() {
        System.out.println(NEW_LINE + STARS + STARS + STARS);
        System.out.println("All paths in the commands can be absolute or relative to the current directory.");
        System.out.println("In case an input file/dir path contains spaces, you would need to put "
                + "double-quotes around them.");
        System.out.println("? or help\t\t\tShows this list of commands.");
        System.out.println("cd <dir>\t\t\tChange current directory to <dir>");
        System.out.println("create file <file path>\t\t\tCreates a file.");
        System.out.println("create dir <dir path>\t\t\tCreates a dir.");
        System.out.println("delete <file/dir path>\t\t\tDeletes a file/directory. Directory should be empty.");
        System.out.println("list dir <dir path>\t\t\tLists the contents of the dir.");
        System.out.println("copy <source file path> <target file path> \t\t\tCopies a file.");
        System.out.println("move <source file/dir path> <target file/dir path> \t\t\tMoves a file/dir.");
        System.out.println("exists <file/dir path>\t\t\tTells whether the specified path is a file or a dir.");
        System.out.println("isdir <dir path>\t\t\tTells whether the specified path is a dir.");
        System.out.println("length <file path>\t\t\tReturns the length of the file.");
        System.out.println("truncate <file path> <new-length>\t\t\tTruncates a file to the given length.");
        System.out.println("read <file path> <position> <count>\t\t\tReads and prints specified number of bytes "
                + "from the specified file, starting at the specified position in the file.");
        System.out.println("append <file path>\t\t\tWill read data from this terminal and append that to the "
                + "specified file. End of input is indicated by Ctrl-Z.");
        System.out.println("begin\t\t\tBegins a new transaction.");
        System.out.println("commit\t\t\tCommits the current transaction.");
        System.out.println("rollback\t\t\tRolls back the current transaction.");
        System.out.println("shutdown\t\t\tShuts down the current running XADisk instance. Not valid for remote XADisk instance.");
        System.out.println(STARS + STARS + STARS + NEW_LINE);
    }

    private static boolean readBoolean() throws IOException {
        while (true) {
            String s = normalizeInput(br.readLine());
            if (s.equalsIgnoreCase("n")) {
                return false;
            } else if (s.equalsIgnoreCase("y")) {
                return true;
            } else {
                System.out.println("Invalid Input. Valid inputs are (y) and (n).");
                prompt("Enter Again : ");
            }
        }
    }

    private static String readNonEmptyString() throws Exception {
        while (true) {
            String s = normalizeInput(br.readLine());
            if (s.equals("")) {
                System.out.println("Invalid Input; it was an empty string.");
                prompt("Enter Again : ");
            } else {
                return s;
            }
        }
    }

    private static int readInteger() throws Exception {
        while (true) {
            String s = normalizeInput(br.readLine());
            try {
                return Integer.valueOf(s);
            } catch (NumberFormatException nfe) {
                System.out.println("Invalid Input. Enter a valid number.");
                prompt("Enter Again : ");
            }
        }
    }

    private static String errorToTerminal(Throwable t) {
        StringWriter sw = new StringWriter();
        StringBuffer tab = new StringBuffer();
        while (t != null) {
            sw.append(tab + t.getClass().getName() + " : " + t.getMessage() + NEW_LINE);
            t = t.getCause();
            tab.append("\t\t");
        }
        return sw.toString();
    }

    private static void prompt(String s) {
        System.out.print(s);
        System.out.flush();
    }

    private static void printFormatted(String s) {
        int beginAt = 0;
        int lineLength = 100;
        while (beginAt < s.length()) {
            int lastIndex = Math.min(beginAt + lineLength, s.length()) - 1;
            String part = s.substring(beginAt, lastIndex + 1);
            if (s.length() - 1 != lastIndex) {
                if (s.charAt(lastIndex + 1) != ' ') {
                    lastIndex = part.lastIndexOf(' ');
                    part = part.substring(0, lastIndex + 1);
                }
            }
            System.out.println(part);
            beginAt += lastIndex + 1;
        }
    }

    private static void assertArgumentsCount(String args[], int expectedCount) throws InvalidInputException {
        if (args.length != expectedCount) {
            throw new InvalidInputException("Wrong Number Of Arguments. Expected : " + expectedCount, null);
        }
    }

    private static void assertSession() throws InvalidInputException {
        if (session == null) {
            throw new InvalidInputException("No associated transaction was found. Use \"begin\" to start a transaction.", null);
        }
    }
}
