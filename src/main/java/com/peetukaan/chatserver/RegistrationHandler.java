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
    public void handle(HttpExchange exchange) {
        // Initializing the response, code and the content(response bytes) to be sent.
        // Default values
        String response = "Bad Request";
        Integer code = 400;
        register: try {
            // Request must be only POST
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                Headers headers = exchange.getRequestHeaders();
                int contentLength = 0;
                String contentType = null;
                // Headers must contain a Content-Length
                if (headers.containsKey("Content-Length")) {
                    contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
                    // Limit of the post is 280 characters
                    if (contentLength > 280) {
                        response = "Post limit is 280 characters";
                        code = 406;
                        break register;
                    }
                } else {
                    response = "No content length";
                    code = 411;
                    break register;
                }
                // Headers must contain a Content-Type
                if (headers.containsKey("Content-Type")) {
                    contentType = headers.get("Content-Type").get(0);
                } else {
                    response = "Content type missing";
                    code = 406;
                    break register;
                }
                // Content-Type must be application/json
                if (contentType.equalsIgnoreCase("application/json")) {
                    try {
                        InputStream iStream = exchange.getRequestBody();
                        String text = new BufferedReader(new InputStreamReader(iStream, StandardCharsets.UTF_8)).lines()
                                .collect(Collectors.joining("\n"));
                        // No empty text allowed
                        if (text.trim().length() > 0) {
                            // Username & password has not yet been checked -> False
                            boolean userNameTaken = false;
                            // Parsing a new JSONObject
                            JSONObject registrationMsg = new JSONObject(text);
                            String username = registrationMsg.getString("username");
                            String password = registrationMsg.getString("password");
                            String email = registrationMsg.getString("email");
                            // Registering the user to the database if the content is not empty
                            // (// && email.contains("@") will fail the chat-client tests)
                            if (username.isBlank() == false && password.isBlank() == false && email.isBlank() == false
                                    && username.length() > 1 && password.length() > 3
                                    && email.length() > 5) {
                                userNameTaken = authenticator.checkUser(username, password, email);
                                if (userNameTaken == true) {
                                    iStream.close();
                                    response = "Invalid credentials or username already taken";
                                    code = 405;
                                    break register;
                                    // validUser (True)
                                } else if (userNameTaken == false) {
                                    authenticator.addUser(username, password, email);
                                    iStream.close();
                                    response = "Registration success!";
                                    code = 200;
                                    break register;
                                }
                            } else {
                                iStream.close();
                                response = "Username, password or email not provided correctly. Check ReadMe.";
                                code = 403;
                                break register;
                            }
                        } else {
                            iStream.close();
                            response = "JSON empty";
                            code = 406;
                            break register;
                        }
                    } catch (JSONException e) {
                        response = "Cannot parse JSON";
                        code = 400;
                        break register;
                    }
                } else {
                    response = "Not supported Content-Type";
                    code = 415;
                    break register;
                }
            } else {
                response = "Not supported";
                code = 404;
                break register;
            }
        } catch (IOException e) {
            break register;
        }
        // Sending the response to the client
        // Code and response will vary
        try {
            byte[] bytes = response.getBytes("UTF-8");
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream oStream = exchange.getResponseBody();
            oStream.write(response.getBytes());
            oStream.close();
        } catch (IOException e) {
            System.out.print(e.getMessage());
        }
    }
}