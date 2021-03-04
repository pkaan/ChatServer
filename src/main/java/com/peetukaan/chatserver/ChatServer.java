package com.peetukaan.chatserver;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.HttpsServer;

import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsConfigurator;

public class ChatServer {
    public static void main(String[] args) {
        // Creating instance of the database the data will be stored
        ChatDatabase database = ChatDatabase.getInstance();
        Scanner input = new Scanner(System.in);
        // To start the server the user has to give 3 arguments.
        // 1. Database file name and path
        // 2. Certificate file name and path
        // 3. Password of the certificate
        try {
            if (args.length != 3) {
                System.out.println("Give the following arguments: " + "1. database file name and path "
                        + "2. certificate file name path" + "3. password of the certificate.");
            }
            // Connecting to the database (1. argument)
            database.open(args[0]);
            boolean running = true;
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);
            // Creating secure server (2. and 3. argument)
            SSLContext sslContext = chatServerSSLContext(args[1], args[2]);
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    InetSocketAddress remote = params.getClientAddress();
                    SSLContext c = getSSLContext();
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                }
            });
            // Creating paths /chat & /registration. Setting up the authenticator.
            ChatAuthenticator authenticator = new ChatAuthenticator("/chat");
            HttpContext context = server.createContext("/chat", new ChatHandler());
            HttpContext reg = server.createContext("/registration", new RegistrationHandler(authenticator));
            context.setAuthenticator(authenticator);
            Executor executor = Executors.newCachedThreadPool();
            server.setExecutor(executor);
            server.start();
            // Informing that server is running succesfully
            // Server can be shut down by typing /quit.
            System.out.println("Server running!");
            System.out.println("To shut down the server enter '/quit'");
            while (running == true) {
                String quit = input.nextLine();
                //Typing /quit will stop the server and close the database connection
                if (quit.equals("/quit")) {
                    input.close();
                    running = false;
                    // Closing database connection
                    database.close();
                    // Server stops in 3 seconds
                    server.stop(3);
                }
            }
            System.out.println("Server shut down.");
        } catch (Exception e) {
            System.out.println("Error:" + e.getMessage());
        }
    }

    private static SSLContext chatServerSSLContext(String keystore, String certificatePassword) {
        SSLContext ssl = null;
        char[] passphrase = certificatePassword.toCharArray();
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(keystore), passphrase);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, passphrase);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);
            ssl = SSLContext.getInstance("TLS");
            ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return ssl;
        } catch (Exception e) {
            System.out.println("Error:" + e.getMessage());
        }
        return ssl;
    }
}