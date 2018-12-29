/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */

/*
 * Many Thanks to Jasper Siepkes for suggesting the bug fix for
 * https://java.net/jira/browse/XADISK-140
 */
package org.xadisk.bridge.server;

import org.xadisk.bridge.server.conversation.ConversationGateway;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import org.xadisk.filesystem.NativeXAFileSystem;

public class PointOfContact implements Work {

    private final ServerSocket serverSocket;
    private final ServerSocketChannel serverSocketChannel;
    private volatile boolean enabled = true;
    private final ConversationGateway conversationGateway;
    private final NativeXAFileSystem xaFileSystem;

    public PointOfContact(NativeXAFileSystem xaFileSystem, int port) throws IOException, WorkException {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocket = this.serverSocketChannel.socket();
        this.serverSocket.bind(new InetSocketAddress(port));
        this.conversationGateway = new ConversationGateway(xaFileSystem);
        this.xaFileSystem = xaFileSystem;
    }

    public void run() {
        try {
            xaFileSystem.startWork(conversationGateway);
            while (enabled) {
                try {
                    SocketChannel clientConverationChannel = serverSocketChannel.accept();
                    conversationGateway.delegateConversation(clientConverationChannel);
                } catch (AsynchronousCloseException asce) {
                    //the only possibility is release().
                    break;
                }
            }
        } catch (Throwable t) {
            xaFileSystem.notifySystemFailure(t);
        } finally {
            conversationGateway.release();
            closeServerSocket();
        }
    }

    public void release() {
        enabled = false;
        closeServerSocket();
        //we need to close the channel here to come out of accept.
    }

    private void closeServerSocket() {
        try {
            serverSocketChannel.close();
        } catch (Throwable t) {
            //no-op.
        }
    }
}