package com.fqt.fqtmod.translation;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.List;

public class SniSSLSocketFactory extends SSLSocketFactory {
    private final SSLSocketFactory delegate;
    private final String sniHostname;

    public SniSSLSocketFactory(String sniHostname) throws GeneralSecurityException {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, null, null);
        this.delegate = ctx.getSocketFactory();
        this.sniHostname = sniHostname;
    }

    private void applySni(Socket socket) {
        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket) socket;
            SSLParameters params = sslSocket.getSSLParameters();
            params.setServerNames(List.of(new SNIHostName(sniHostname)));
            sslSocket.setSSLParameters(params);
        }
    }

    @Override public String[] getDefaultCipherSuites() { return delegate.getDefaultCipherSuites(); }
    @Override public String[] getSupportedCipherSuites() { return delegate.getSupportedCipherSuites(); }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        Socket socket = delegate.createSocket(s, sniHostname, port, autoClose); applySni(socket); return socket;
    }
    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket socket = delegate.createSocket(host, port); applySni(socket); return socket;
    }
    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        Socket socket = delegate.createSocket(host, port, localHost, localPort); applySni(socket); return socket;
    }
    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        Socket socket = delegate.createSocket(host, port); applySni(socket); return socket;
    }
    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        Socket socket = delegate.createSocket(address, port, localAddress, localPort); applySni(socket); return socket;
    }
}
