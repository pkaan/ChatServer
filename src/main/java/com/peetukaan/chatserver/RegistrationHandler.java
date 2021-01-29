package com.peetukaan.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RegistrationHandler implements HttpHandler {
    ChatAuthenticator authenticator;

    public RegistrationHandler(ChatAuthenticator chatAuthenticator) {
        authenticator = chatAuthenticator;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        
        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {  
            InputStream iStream = exchange.getRequestBody();
            String text = new BufferedReader(new InputStreamReader(iStream,StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining());

            String[] pieces = text.split(":");
            boolean validUser = authenticator.addUser(pieces[0], pieces[1]);
            if (validUser == false) {
                iStream.close();
                String message = "Invalid username or username already taken";
                byte [] bytes = message.getBytes("UTF-8");
                exchange.sendResponseHeaders(405, bytes.length);
                OutputStream oStream = exchange.getResponseBody();
                oStream.write(message.getBytes());
            } else {
                iStream.close();
                exchange.sendResponseHeaders(200, -1);
            }
        } else {
            String message = "Not supported";
            byte [] bytes = message.getBytes("UTF-8");
            exchange.sendResponseHeaders(404, bytes.length);
            OutputStream oStream = exchange.getResponseBody();
            oStream.write(message.getBytes());
        }   
    }
}