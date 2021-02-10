package com.peetukaan.chatserver;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.HttpsServer;

import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsConfigurator;

public class ChatServer 
{
    public static void main( String[] args ) throws Exception {
        try {
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);
            SSLContext sslContext = chatServerSSLContext();
            server.setHttpsConfigurator (new HttpsConfigurator(sslContext) {
                public void configure (HttpsParameters params) {
                InetSocketAddress remote = params.getClientAddress();
                SSLContext c = getSSLContext();
                SSLParameters sslparams = c.getDefaultSSLParameters();
                params.setSSLParameters(sslparams);
                }
            });
            ChatAuthenticator authenticator = new ChatAuthenticator("/chat");
            HttpContext context = server.createContext("/chat", new ChatHandler());
            HttpContext reg = server.createContext("/registration", new RegistrationHandler(authenticator));

            context.setAuthenticator(authenticator);
            server.setExecutor(null);
            server.start();
            System.out.println("Server running!");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static SSLContext chatServerSSLContext() throws Exception {
        char[] passphrase = "c1wsFeoBtGNffM0BPWOZ".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream("keystore.jks"), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ssl;

    }
}