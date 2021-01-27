package com.peetukaan.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ChatHandler implements HttpHandler {

    private ArrayList<String> messages = new ArrayList<String>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        
        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {  
            InputStream iStream = exchange.getRequestBody();
            String text = new BufferedReader(new InputStreamReader(iStream,StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
       
            if (text != null) {
                messages.add(text);
            }
            iStream.close();
            exchange.sendResponseHeaders(200, -1);
        }
        else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            String messageBody = "";
            for (String message : messages) {
                messageBody = messageBody + message + "\n";
            }            
            byte [] bytes = messageBody.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream oStream = exchange.getResponseBody();
            oStream.write(messageBody.getBytes());
            oStream.close();
        }
        else {
            String message = "Not supported";
            byte [] bytes = message.getBytes("UTF-8");
            exchange.sendResponseHeaders(404, bytes.length);
            OutputStream oStream = exchange.getResponseBody();
            oStream.write(message.getBytes());
        }        
    }
}
