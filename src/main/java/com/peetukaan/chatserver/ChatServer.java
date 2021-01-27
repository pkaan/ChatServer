package com.peetukaan.chatserver;

import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

public class ChatServer 
{
    public static void main( String[] args ) throws Exception
    {
        HttpServer server = HttpServer.create(new InetSocketAddress(8001), 0);
        server.createContext("/chat", new ChatHandler());
        server.setExecutor(null);
        server.start();

    }
}