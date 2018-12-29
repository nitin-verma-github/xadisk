/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.facilitators;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import org.xadisk.filesystem.exceptions.ConnectionException;

public class RemoteMethodInvoker implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String serverAddress;
    private final int serverPort;
    private transient SocketChannel channel;
    private transient Socket socket;
    private boolean connected = false;
    private static final String UTF8CharsetName = "UTF-8";

    public RemoteMethodInvoker(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    /*@Override
     * Not overriding clone. The javaDoc says : "By convention, the returned object
     * should be obtained by calling super.clone". As we are not doing that, so we
     * won't "occupy/defame" the clone() method.
     public Object clone() throws CloneNotSupportedException {
     return new RemoteMethodInvoker(serverAddress, serverPort);
     }*/
    public RemoteMethodInvoker makeCopy() {
        return new RemoteMethodInvoker(serverAddress, serverPort);
    }

    public RemoteMethodInvoker ensureConnected() throws IOException {
        if (connected) {
            return this;
        }
        channel = SocketChannel.open(new InetSocketAddress(serverAddress, serverPort));
        channel.configureBlocking(true);
        channel.finishConnect();
        socket = channel.socket();
        connected = true;
        return this;
    }

    public void disconnect() throws IOException {
        if (connected) {
            socket.close();
            connected = false;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public Object invokeRemoteMethod(long targetObjectId, String method, Serializable... args) throws Throwable {
        boolean isError;
        Object returnObject;
        try {
            ensureConnected();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeLong(targetObjectId);
            byte[] methodNameBytes = method.getBytes(UTF8CharsetName);
            oos.writeInt(methodNameBytes.length);
            oos.write(methodNameBytes);
            oos.writeInt(args.length);
            ArrayList<OptimizedRemoteReference> remoteReferences = new ArrayList<OptimizedRemoteReference>();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof OptimizedRemoteReference) {
                    remoteReferences.add((OptimizedRemoteReference) args[i]);
                }
                oos.writeObject(args[i]);
            }
            oos.flush();

            byte[] toSend = baos.toByteArray();
            int lengthOfInvocation = toSend.length;

            OutputStream socketOS = new BufferedOutputStream(
                    socket.getOutputStream(), 1024);
            socketOS.write(getDataOutputCompliantBytesFromInteger(lengthOfInvocation));
            socketOS.write(toSend);
            socketOS.flush();

            BufferedInputStream bis = new BufferedInputStream(socket.getInputStream(), 1024);
            ObjectInputStream ois = new ObjectInputStream(bis);
            isError = ois.readBoolean();
            int numOutputs = ois.readInt();
            returnObject = ois.readObject();
            for (int i = 1; i < numOutputs; i++) {
                OptimizedRemoteReference updatedRef = (OptimizedRemoteReference) ois.readObject();
                if (updatedRef instanceof ByteArrayRemoteReference) {
                    ByteArrayRemoteReference barr = (ByteArrayRemoteReference) remoteReferences.get(i - 1);
                    ByteArrayRemoteReference updatedBarr = (ByteArrayRemoteReference) updatedRef;
                    barr.mergeWithRemoteObject(updatedBarr.getResultObject());
                }
            }
        } catch (IOException ioe) {
            this.disconnect();
            throw new ConnectionException(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new InternalXASystemException(cnfe);
        }
        if (isError) {
            throw (Throwable) returnObject;
        }
        return returnObject;
    }

    private byte[] getDataOutputCompliantBytesFromInteger(int i) {
        byte[] b = new byte[4];
        b[0] = (byte) ((i >> 24) & 0xFF);
        b[1] = (byte) ((i >> 16) & 0xFF);
        b[2] = (byte) ((i >> 8) & 0xFF);
        b[3] = (byte) (i & 0xFF);
        return b;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RemoteMethodInvoker) {
            RemoteMethodInvoker that = (RemoteMethodInvoker) obj;
            return this.serverAddress.equals(that.serverAddress)
                    && this.serverPort == that.serverPort;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.serverPort + this.serverAddress.hashCode();
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getServerPort() {
        return serverPort;
    }
}
