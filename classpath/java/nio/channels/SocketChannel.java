/* Copyright (c) 2008-2009, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.nio.channels;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class SocketChannel extends SelectableChannel
  implements ReadableByteChannel, WritableByteChannel
{
  public static final int InvalidSocket = -1;

  int socket = InvalidSocket;
  boolean connected = false;
  boolean blocking = true;

  public static SocketChannel open() {
    return new SocketChannel();
  }

  public SelectableChannel configureBlocking(boolean v) throws IOException {
    blocking = v;
    if (socket != InvalidSocket) {
      configureBlocking(socket, v);
    }
    return this;
  }

  public boolean isBlocking() {
    return blocking;
  }

  public boolean isConnected() {
    return connected;
  }

  public Socket socket() {
    return new Handle();
  }

  public boolean connect(SocketAddress address) throws IOException {
    InetSocketAddress a;
    try {
      a = (InetSocketAddress) address;
    } catch (ClassCastException e) {
      throw new UnsupportedAddressTypeException();
    }
    socket = doConnect(a.getHostName(), a.getPort());
    configureBlocking(blocking);
    return connected;
  }

  public boolean finishConnect() throws IOException {
    if (! connected) {
      connected = natFinishConnect(socket);
    }
    return connected;
  }

  public void close() throws IOException {
    if (isOpen()) {
      super.close();
      closeSocket();
    }
  }

  private int doConnect(String host, int port) throws IOException {
    if (host == null) throw new NullPointerException();

    boolean b[] = new boolean[1];
    int s = natDoConnect(host, port, blocking, b);
    connected = b[0];
    return s;
  }

  public int read(ByteBuffer b) throws IOException {
    if (! isOpen()) return -1;
    if (b.remaining() == 0) return 0;

    byte[] array = b.array();
    if (array == null) throw new NullPointerException();

    int r = natRead(socket, array, b.arrayOffset() + b.position(), b.remaining());
    if (r > 0) {
      b.position(b.position() + r);
    }
    return r;
  }

  public int write(ByteBuffer b) throws IOException {
    if (! connected) {
      natThrowWriteError(socket);
    }
    if (b.remaining() == 0) return 0;

    byte[] array = b.array();
    if (array == null) throw new NullPointerException();

    int w = natWrite(socket, array, b.arrayOffset() + b.position(), b.remaining());
    if (w > 0) {
      b.position(b.position() + w);
    }
    return w;
  }

  private void closeSocket() {
    natCloseSocket(socket);
  }

  int socketFD() {
    return socket;
  }

  public class Handle extends Socket {
    public void setTcpNoDelay(boolean on) throws SocketException {
      natSetTcpNoDelay(socket, on);
    }
  }

  private static native void configureBlocking(int socket, boolean blocking)
    throws IOException;

  private static native void natSetTcpNoDelay(int socket, boolean on)
    throws SocketException;

  private static native int natDoConnect(String host, int port, boolean blocking, boolean[] connected)
    throws IOException;
  private static native boolean natFinishConnect(int socket)
    throws IOException;
  private static native int natRead(int socket, byte[] buffer, int offset, int length)
    throws IOException;
  private static native int natWrite(int socket, byte[] buffer, int offset, int length)
    throws IOException;
  private static native void natThrowWriteError(int socket) throws IOException;
  private static native void natCloseSocket(int socket);
}
