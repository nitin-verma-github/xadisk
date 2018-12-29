/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.server.conversation;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import org.xadisk.bridge.proxies.impl.RemoteLock;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.bridge.proxies.impl.RemoteMessageEndpoint;
import org.xadisk.bridge.proxies.impl.RemoteSession;
import org.xadisk.bridge.proxies.impl.RemoteXAFileInputStream;
import org.xadisk.bridge.proxies.impl.RemoteXAFileOutputStream;
import org.xadisk.filesystem.Lock;
import org.xadisk.filesystem.NativeSession;

public class ConversationContext {

    private final SocketChannel conversationChannel;
    private int currentMethodInvocationLength = -1;
    private ByteBuffer currentInvocationLengthBytes = ByteBuffer.allocate(4);
    private ByteBuffer currentMethodInvocationBuffer;
    private byte[] currentMethodInvocation;
    private final HostedContext conversationalHostedContext;
    private final HostedContext globalHostedContext;
    private final ArrayList<NativeSession> allSessionsInsideThisConversation = new ArrayList<NativeSession>();
    private final NativeXAFileSystem xaFileSystem;

    public ConversationContext(SocketChannel conversationChannel, NativeXAFileSystem xaFileSystem) {
        this.xaFileSystem = xaFileSystem;
        this.conversationChannel = conversationChannel;
        this.conversationalHostedContext = new ConversationalHostedContext();
        this.conversationalHostedContext.hostObject(xaFileSystem);//objectId 0 remotely.
        this.conversationalHostedContext.hostObject(xaFileSystem.getConcurrencyControl());//objectId 1 remotely.
        this.globalHostedContext = xaFileSystem.getGlobalCallbackContext();
    }

    public Object getLocalObjectFromProxy(long objectId) {
        if (objectId < 0) {
            return globalHostedContext.getHostedObjectWithId(objectId);
        } else {
            return conversationalHostedContext.getHostedObjectWithId(objectId);
        }
    }

    public SocketChannel getConversationChannel() {
        return conversationChannel;
    }

    public byte[] getCurrentMethodInvocation() {
        return currentMethodInvocation;
    }

    public void updateWithConversation(ByteBuffer buffer) throws WorkException, IOException {
        if (currentMethodInvocationLength == -1) {
            byte[] t = new byte[Math.min(currentInvocationLengthBytes.remaining(), buffer.remaining())];
            buffer.get(t);
            currentInvocationLengthBytes.put(t);
            if (currentInvocationLengthBytes.position() == 4) {
                currentInvocationLengthBytes.flip();
                currentMethodInvocationLength = getIntegerFromDataOutputCompliantBytes(currentInvocationLengthBytes);
            }
        }
        if (currentMethodInvocationLength != -1) {
            if (currentMethodInvocationBuffer == null) {
                currentMethodInvocationBuffer = ByteBuffer.allocate(currentMethodInvocationLength);
            }
            currentMethodInvocationBuffer.put(buffer);

            if (currentMethodInvocationBuffer.position() == currentMethodInvocationLength) {
                currentMethodInvocation = new byte[currentMethodInvocationLength];
                currentMethodInvocationBuffer.flip();
                currentMethodInvocationBuffer.get(currentMethodInvocation);
                clearLastConversation();

                Work handleRemoteMethodInvocation = new RemoteMethodInvocationHandler(this, xaFileSystem);
                xaFileSystem.startWork(handleRemoteMethodInvocation);
            }
        }
    }

    private void clearLastConversation() {
        currentMethodInvocationLength = -1;
        currentInvocationLengthBytes.clear();
        currentMethodInvocationBuffer = null;
    }

    Object convertToProxyResponseIfRequired(Object response) {
        if (xaFileSystem.getHandleGeneralRemoteInvocations()) {
            if (response instanceof Session) {
                allSessionsInsideThisConversation.add((NativeSession) response);
                return new RemoteSession(conversationalHostedContext.hostObject(response), null);
            }
            if (response instanceof XAFileInputStream) {
                return new RemoteXAFileInputStream(conversationalHostedContext.hostObject(response), null);
            }
            if (response instanceof XAFileOutputStream) {
                return new RemoteXAFileOutputStream(conversationalHostedContext.hostObject(response), null);
            }
            if (response instanceof MessageEndpoint) {
                return new RemoteMessageEndpoint(globalHostedContext.hostObject(response), null);
            }
        }
        if (xaFileSystem.getHandleClusterRemoteInvocations()) {
            if (response instanceof Lock) {
                Lock lock = (Lock) response;
                return new RemoteLock(conversationalHostedContext.hostObject(response),
                        lock.getResource(), lock.isExclusive());
            }
        }
        return response;
    }

    private int getIntegerFromDataOutputCompliantBytes(ByteBuffer b) {
        return (((b.get() & 0xff) << 24) | ((b.get() & 0xff) << 16) | ((b.get() & 0xff) << 8) | (b.get() & 0xff));
    }
}
