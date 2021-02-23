package com.peetukaan.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatHandler implements HttpHandler {

    ChatDatabase database = ChatDatabase.getInstance();

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
                    InputStream iStream = exchange.getRequestBody();
                    String text = new BufferedReader(new InputStreamReader(iStream, StandardCharsets.UTF_8)).lines()
                            .collect(Collectors.joining("\n"));

                    if (text.trim().length() > 0) {
                        try {
                            iStream.close();
                            JSONObject msg = new JSONObject(text);
                            String message = msg.getString("message");
                            String user = msg.getString("user");
                            String sent = msg.getString("sent");
                            OffsetDateTime odt = OffsetDateTime.parse(sent);
                            LocalDateTime now = odt.toLocalDateTime();
                            ChatMessage newMessage = new ChatMessage(now, user, message);
                            long unix = newMessage.dateAsInt();
                            try {
                                database.addMessage(newMessage.getNick(), newMessage.getMessage(), unix);
                            } catch (Exception e) {
                                String error = "Could not store message to the database!";
                                byte[] bytes = error.getBytes("UTF-8");
                                exchange.sendResponseHeaders(401, bytes.length);
                                OutputStream oStream = exchange.getResponseBody();
                                oStream.write(error.getBytes());
                            }
                            String response = "Message sent!";
                            byte[] bytes = response.getBytes("UTF-8");
                            exchange.sendResponseHeaders(200, bytes.length);
                            OutputStream oStream = exchange.getResponseBody();
                            oStream.write(response.getBytes());
                        } catch (JSONException e) {
                            String message = "Error";
                            byte[] bytes = message.getBytes("UTF-8");
                            exchange.sendResponseHeaders(406, bytes.length);
                            OutputStream oStream = exchange.getResponseBody();
                            oStream.write(message.getBytes());
                        }
                    } else {
                        iStream.close();
                        String message = "Empty message";
                        byte[] bytes = message.getBytes("UTF-8");
                        exchange.sendResponseHeaders(406, bytes.length);
                        OutputStream oStream = exchange.getResponseBody();
                        oStream.write(message.getBytes());
                    }
                }
            } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                Headers headers = exchange.getRequestHeaders();
                try {
                    if (headers.containsKey("If-Modified-Since")) {
                        String time = headers.get("If-Modified-Since").get(0);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss.SSS zzz");
                        ZonedDateTime modified = ZonedDateTime.parse(time, formatter);
                        LocalDateTime fromWhichModified = modified.toLocalDateTime();
                        long messagesSince = -1;
                        messagesSince = fromWhichModified.toInstant(ZoneOffset.UTC).toEpochMilli();
                        JSONArray objs = database.getMessages(messagesSince);

                        if (objs.isEmpty()) {
                            String error = "No new messages!";
                            byte[] bytes = error.getBytes("UTF-8");
                            exchange.sendResponseHeaders(204, bytes.length);
                            OutputStream oStream = exchange.getResponseBody();
                            oStream.write(error.getBytes());
                        }
                        String messageBody3 = objs.toString();
                        byte[] bytes = messageBody3.getBytes("UTF-8");
                        String headerDateText = database.getHeaderDate();
                        exchange.getResponseHeaders().add("Last-Modified", headerDateText);
                        exchange.sendResponseHeaders(200, bytes.length);
                        OutputStream oStream = exchange.getResponseBody();
                        oStream.write(messageBody3.getBytes());
                        oStream.close();
                    } else {
                        JSONArray objs = database.getMessages();
                        String messageBody3 = objs.toString();
                        byte[] bytes = messageBody3.getBytes("UTF-8");
                        String headerDateText = database.getHeaderDate();
                        exchange.getResponseHeaders().add("Last-Modified", headerDateText);
                        exchange.sendResponseHeaders(200, bytes.length);
                        OutputStream oStream = exchange.getResponseBody();
                        oStream.write(messageBody3.getBytes());
                        oStream.close();
                    }
                } catch (JSONException e) {
                    String message = e.getMessage();
                    byte[] bytes = message.getBytes("UTF-8");
                    exchange.sendResponseHeaders(400, bytes.length);
                    OutputStream oStream = exchange.getResponseBody();
                    oStream.write(message.getBytes());
                    oStream.close();
                } catch (SQLException e) {
                    String message = e.getMessage();
                    byte[] bytes = message.getBytes("UTF-8");
                    exchange.sendResponseHeaders(400, bytes.length);
                    OutputStream oStream = exchange.getResponseBody();
                    oStream.write(message.getBytes());
                    oStream.close();
                }
            } else {
                String message = "Not supported";
                byte[] bytes = message.getBytes("UTF-8");
                exchange.sendResponseHeaders(404, bytes.length);
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
