package com.peetukaan.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatHandler implements HttpHandler {

    private ArrayList<ChatMessage> messages = new ArrayList<ChatMessage>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        try {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {  
                Headers headers = exchange.getRequestHeaders();
                int contentLength = 0;
                String contentType = "";
                if (headers.containsKey("Content-Length")) {
                    contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
                } else {
                    String message = "No content length";
                    byte [] bytes = message.getBytes("UTF-8");
                    exchange.sendResponseHeaders(411, bytes.length);
                    OutputStream oStream = exchange.getResponseBody();
                    oStream.write(message.getBytes());
                }
                if (headers.containsKey("Content-Type")) {
                    contentType = headers.get("Content-Type").get(0);
                } else {
                    String message = "Content type missing";
                    byte [] bytes = message.getBytes("UTF-8");
                    exchange.sendResponseHeaders(406, bytes.length);
                    OutputStream oStream = exchange.getResponseBody();
                    oStream.write(message.getBytes());
                }
                if (contentType.equalsIgnoreCase("application/json")) {
                    InputStream iStream = exchange.getRequestBody();
                    String text = new BufferedReader(new InputStreamReader(iStream,StandardCharsets.UTF_8))
                    .lines()
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
                            try {
                                messages.add(new ChatMessage(now, user, message));
                            } catch (Exception e) {
                                String error = "User authentication missing!";
                                byte [] bytes = error.getBytes("UTF-8");
                                exchange.sendResponseHeaders(401, bytes.length);
                                OutputStream oStream = exchange.getResponseBody();
                                oStream.write(error.getBytes());
                            }
                            Collections.sort(messages, new Comparator<ChatMessage>() {
                                @Override
                                public int compare(ChatMessage lhs, ChatMessage rhs) {
                                return rhs.getSent().compareTo(lhs.getSent());}
                                });                           
                            String response = "Message sent!";
                            byte [] bytes = response.getBytes("UTF-8");
                            exchange.sendResponseHeaders(200, bytes.length);
                            OutputStream oStream = exchange.getResponseBody();
                            oStream.write(response.getBytes());
                        } catch (JSONException e) {
                            String message = "Error";
                            byte [] bytes = message.getBytes("UTF-8");
                            exchange.sendResponseHeaders(406, bytes.length);
                            OutputStream oStream = exchange.getResponseBody();
                            oStream.write(message.getBytes());
                        }
                    } else {
                        iStream.close();
                        String message = "Empty message";
                        byte [] bytes = message.getBytes("UTF-8");
                        exchange.sendResponseHeaders(406, bytes.length);
                        OutputStream oStream = exchange.getResponseBody();
                        oStream.write(message.getBytes());
                    }
                }
            } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                try {
                    JSONArray objs = new JSONArray();
                    for (ChatMessage message : messages) {
                        JSONObject obj = new JSONObject();
                        ZonedDateTime toSend = ZonedDateTime.of(message.getSent(), ZoneId.of("UTC"));
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                        String dateText = toSend.format(formatter);
                        obj.put("sent", dateText);
                        obj.put("user", message.getNick());
                        obj.put("message", message.getMessage());
                        objs.put(obj);
                    }            
                    String messageBody = "";
                    for (int i = 0; i < objs.length(); i++) {
                        String sent = objs.getJSONObject(i).getString("sent");
                        String nick = objs.getJSONObject(i).getString("user");
                        String message = objs.getJSONObject(i).getString("message");
                        messageBody = messageBody + "(" + sent + ") <" + nick + "> " + message + "\n";
                    }            
                    byte [] bytes = messageBody.getBytes("UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    OutputStream oStream = exchange.getResponseBody();
                    oStream.write(messageBody.getBytes());
                    oStream.close();
                } catch (JSONException e) {
                    String message = e.getMessage();
                    byte [] bytes = message.getBytes("UTF-8");
                    exchange.sendResponseHeaders(400, bytes.length);
                    OutputStream oStream = exchange.getResponseBody();
                    oStream.write(message.getBytes());
                    oStream.close();
                }
            }
            else {
                String message = "Not supported";
                byte [] bytes = message.getBytes("UTF-8");
                exchange.sendResponseHeaders(404, bytes.length);
                OutputStream oStream = exchange.getResponseBody();
                oStream.write(message.getBytes());
            }
        } catch (IOException e) {
            String message = e.getMessage();
            byte [] bytes = message.getBytes("UTF-8");
            exchange.sendResponseHeaders(400, bytes.length);
            OutputStream oStream = exchange.getResponseBody();
            oStream.write(message.getBytes());
        }      
    }
}
