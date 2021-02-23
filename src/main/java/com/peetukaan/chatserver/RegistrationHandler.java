package com.peetukaan.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONException;
import org.json.JSONObject;

public class RegistrationHandler implements HttpHandler {
    ChatAuthenticator authenticator;

    public RegistrationHandler(ChatAuthenticator chatAuthenticator) {
        authenticator = chatAuthenticator;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("Request handled in thread " + Thread.currentThread().getId());
        try {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                Headers headers = exchange.getRequestHeaders();
                int contentLength = 0;
                String contentType = "";

                if (headers.containsKey("Content-Length")) {
                    contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
                } else {
                    String message = "No content length";
                    byte[] bytes = message.getBytes("UTF-8");
                    exchange.sendResponseHeaders(411, bytes.length);
                    OutputStream oStream = exchange.getResponseBody();
                    oStream.write(message.getBytes());
                }
                if (headers.containsKey("Content-Type")) {
                    contentType = headers.get("Content-Type").get(0);
                } else {
                    String message = "Content type missing";
                    byte[] bytes = message.getBytes("UTF-8");
                    exchange.sendResponseHeaders(406, bytes.length);
                    OutputStream oStream = exchange.getResponseBody();
                    oStream.write(message.getBytes());
                }
                if (contentType.equalsIgnoreCase("application/json")) {
                    try {
                        InputStream iStream = exchange.getRequestBody();
                        String text = new BufferedReader(new InputStreamReader(iStream, StandardCharsets.UTF_8)).lines()
                                .collect(Collectors.joining("\n"));

                        if (text.trim().length() > 0) {
                            JSONObject registrationMsg = new JSONObject(text);
                            String username = registrationMsg.getString("username");
                            String password = registrationMsg.getString("password");
                            String email = registrationMsg.getString("email");
                            boolean validUser = authenticator.addUser(username, password, email);
                            if (validUser == false) {
                                iStream.close();
                                String message = "Invalid username or username already taken";
                                byte[] bytes = message.getBytes("UTF-8");
                                exchange.sendResponseHeaders(405, bytes.length);
                                OutputStream oStream = exchange.getResponseBody();
                                oStream.write(message.getBytes());
                            } else {
                                iStream.close();
                                String message = "Registration success!";
                                byte[] bytes = message.getBytes("UTF-8");
                                exchange.sendResponseHeaders(200, bytes.length);
                                OutputStream oStream = exchange.getResponseBody();
                                oStream.write(message.getBytes());
                            }
                        } else {
                            iStream.close();
                            String message = "Content missing";
                            byte[] bytes = message.getBytes("UTF-8");
                            exchange.sendResponseHeaders(405, bytes.length);
                            OutputStream oStream = exchange.getResponseBody();
                            oStream.write(message.getBytes());
                        }
                    } catch (JSONException e) {
                        String message = "Invalid json-file";
                        byte[] bytes = message.getBytes("UTF-8");
                        exchange.sendResponseHeaders(406, bytes.length);
                        OutputStream oStream = exchange.getResponseBody();
                        oStream.write(message.getBytes());
                    }
                } else {
                    String message = "Not supported format";
                    byte[] bytes = message.getBytes("UTF-8");
                    exchange.sendResponseHeaders(406, bytes.length);
                    OutputStream oStream = exchange.getResponseBody();
                    oStream.write(message.getBytes());
                }
            } else {
                String message = "Not supported";
                byte[] bytes = message.getBytes("UTF-8");
                exchange.sendResponseHeaders(400, bytes.length);
                OutputStream oStream = exchange.getResponseBody();
                oStream.write(message.getBytes());
            }
        } catch (IOException e) {
            String message = e.getMessage();
            byte[] bytes = message.getBytes("UTF-8");
            exchange.sendResponseHeaders(400, bytes.length);
            OutputStream oStream = exchange.getResponseBody();
            oStream.write(message.getBytes());
        }
    }
}