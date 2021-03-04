package com.peetukaan.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class ChatHandler implements HttpHandler {
    ChatDatabase database = ChatDatabase.getInstance();

    public ChatHandler() {
    }

    @Override
    public void handle(HttpExchange exchange) {
        // Initializing the response, code and the content(response bytes) to be sent.
        // Default reponse Bad Request
        String response = "Bad Request";
        Integer code = 400;
        chat: try {
            // Request must be POST or GET
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
                        break chat;
                    }
                } else {
                    response = "No content length";
                    code = 411;
                    break chat;
                }
                // Headers must contain a Content-Type
                if (headers.containsKey("Content-Type")) {
                    contentType = headers.get("Content-Type").get(0);
                } else {
                    response = "Content type missing";
                    code = 406;
                    break chat;
                }
                // Content-Type must be application/json
                if (contentType.equalsIgnoreCase("application/json")) {
                    InputStream iStream = exchange.getRequestBody();
                    String text = new BufferedReader(new InputStreamReader(iStream, StandardCharsets.UTF_8)).lines()
                            .collect(Collectors.joining("\n"));
                    // No empty text allowed
                    if (text.trim().length() > 0) {
                        try {
                            iStream.close();
                            // Parsing a new JSONObject
                            JSONObject msg = new JSONObject(text);
                            String message = msg.getString("message");
                            String user = msg.getString("user");
                            if (database.checkIfUserExists(user) == false) {
                                code = 403;
                                response = "Username does not exist in the  database";
                                break chat;
                            }
                            String sent = msg.getString("sent");
                            // No empty content allowed
                            if (message.isBlank() == true || user.isBlank() == true || sent.isBlank() == true) {
                                code = 406;
                                response = "No content(message/user/sent)";
                                break chat;
                            }
                            /*
                             * JSON content has the message sent time as ZonedDateTime Message sent time
                             * (LocalDateTime) will be stored in the database as UNIX time ZonedDateTime ->
                             * LocalDateTime -> UNIX
                             */
                            OffsetDateTime odt = OffsetDateTime.parse(sent);
                            LocalDateTime now = odt.toLocalDateTime();
                            // A new message
                            ChatMessage newMessage = new ChatMessage(now, user, message);
                            long unix = newMessage.dateAsInt();
                            /*
                             * <OPTIONAL> If user has given a location (a proper city found in fmi.fi) in
                             * the JSON, the weather information will be attached to the message.
                             */
                            try {
                                String location = msg.getString("location").toLowerCase();
                                // Building the string. Adding location.
                                String fullweatherInfo = location;
                                /*
                                 * Parameters StartTime and EndTime will tell the weather service to get the
                                 * most recent weather information available. For example: If the message has
                                 * been sent 16:35, the data could be from 16:20. StartTime is the beginning of
                                 * the day (00:00:00). EndTime is the time the message has been sent. The data
                                 * can't be from the future. Weather information older than 168.000000 hours
                                 * (from now) cannot be obtained. If the information is found it will be
                                 * attached to the message. If the information is not found the message will be
                                 * saved without the weather information. Information String will consist:
                                 * city(data measure time):temperatureC/pressurehPa. Pressure might not be
                                 * available.
                                 */
                                LocalDateTime fmiResetHours = LocalDateTime.of(now.getYear(), now.getMonth(),
                                        now.getDayOfMonth(), 00, 00, 00);
                                String weatherStartTime = fmiResetHours.toString();
                                String weatherEndTime = now.toString();
                                // XML file builder
                                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                                DocumentBuilder builder = factory.newDocumentBuilder();
                                // Parsing the XML file (Temperature)
                                try {
                                    URL url = new URL(
                                            "http://opendata.fmi.fi/wfs?service=WFS&version=2.0.0&request=getFeature"
                                                    + "&storedquery_id=fmi::observations::weather::timevaluepair&parameters=t2m&starttime="
                                                    + weatherStartTime + "&endtime=" + weatherEndTime + "&place="
                                                    + location);
                                    URLConnection conn = url.openConnection();
                                    Document xmlDoc = builder.parse(conn.getInputStream());
                                    NodeList elements = xmlDoc.getElementsByTagName("wml2:value");
                                    int recent = elements.getLength() - 1;
                                    // Getting the temperature information
                                    Integer temperature = (int) Math.round(Double.valueOf(
                                            xmlDoc.getElementsByTagName("wml2:value").item(recent).getTextContent()));
                                    // Getting the most recent measure time of the data
                                    String measureTime = xmlDoc.getElementsByTagName("wml2:time").item(recent)
                                            .getTextContent();
                                    // Building the string. Adding measure time and temperature.
                                    fullweatherInfo = fullweatherInfo + "(" + measureTime + "):" + temperature + "C";
                                    // Attaching information to the message
                                    newMessage.setWeatherInfo(fullweatherInfo);
                                } catch (IOException e) {
                                    response = "Failed to retrieve data from fmi.fi";
                                    code = 400;
                                }
                                /*
                                 * Parsing the XML file (Pressure). IF POSSIBLE. For example Tampere does not
                                 * have the pressure information. -> Tampere's Weather information will only
                                 * have the temperature data
                                 */
                                try {
                                    URL url2 = new URL(
                                            "http://opendata.fmi.fi/wfs?service=WFS&version=2.0.0&request=getFeature"
                                                    + "&storedquery_id=fmi::observations::weather::timevaluepair&parameters=p_sea&starttime="
                                                    + weatherStartTime + "&endtime=" + weatherEndTime + "&place="
                                                    + location);
                                    URLConnection conn2 = url2.openConnection();
                                    Document xmlDoc2 = builder.parse(conn2.getInputStream());
                                    NodeList elements2 = xmlDoc2.getElementsByTagName("wml2:value");
                                    int recent2 = elements2.getLength() - 1;
                                    // Getting the pressure information
                                    Integer pressure = (int) Math.round(Double.valueOf(
                                            xmlDoc2.getElementsByTagName("wml2:value").item(recent2).getTextContent()));
                                    // Building the string. Adding pressure.
                                    fullweatherInfo = fullweatherInfo + "/" + pressure + "hPa";
                                    // Attaching information to the message
                                    newMessage.setWeatherInfo(fullweatherInfo);
                                } catch (IOException e) {
                                    response = "Failed to retrieve data from fmi.fi";
                                    code = 400;
                                }
                            } catch (Exception e) {
                                response = "Error in parsing XML file";
                                code = 500;
                            }
                            try {
                                // Inserting new message to the database
                                database.addMessage(newMessage, unix);
                                /*
                                 * Informing client: The message has been sent with the weather information OR
                                 * the message has been sent without the weather information OR the message
                                 * cannot be sent
                                 */
                                if (newMessage.getWeatherInfo() != null) {
                                    response = "Message sent with the weather information!";
                                    code = 200;
                                    break chat;
                                } else {
                                    response = "Message sent!";
                                    code = 200;
                                    break chat;
                                }
                            } catch (Exception e) {
                                response = "Database error!";
                                code = 500;
                                break chat;
                            }
                        } catch (JSONException e) {
                            response = "JSON parse error";
                            code = 400;
                            break chat;
                        }
                    } else {
                        iStream.close();
                        response = "No content";
                        code = 406;
                        break chat;
                    }
                } else {
                    response = "Not supported Content-Type";
                    code = 415;
                    break chat;
                }
                // Request must be POST or GET
            } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                Headers headers = exchange.getRequestHeaders();
                try {
                    // If the headers contain If-Modified-Since -header,
                    // The client will only get the new messages he has not seen
                    if (headers.containsKey("If-Modified-Since")) {
                        String time = headers.get("If-Modified-Since").get(0);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss.SSS zzz");
                        ZonedDateTime modified = ZonedDateTime.parse(time, formatter);
                        LocalDateTime fromWhichModified = modified.toLocalDateTime();
                        long messagesSince = -1;
                        messagesSince = fromWhichModified.toInstant(ZoneOffset.UTC).toEpochMilli();
                        JSONArray objs = database.getMessages(messagesSince);
                        // No new messages in the system (that client has not seen)
                        if (objs.isEmpty()) {
                            code = 204;
                            break chat;
                        }
                        // Adding the time to the header when the client has checked the messages
                        response = objs.toString();
                        String LastModified = database.getLastModified();
                        exchange.getResponseHeaders().add("Last-Modified", LastModified);
                        code = 200;
                        break chat;
                        // If there is not If-Modified-Since header the client will get all the messages
                        // Last-Modified header will be added
                    } else {
                        JSONArray objs = database.getMessages();
                        if (objs.isEmpty()) {
                            code = 204;
                            break chat;
                        }
                        response = objs.toString();
                        String LastModified = database.getLastModified();
                        exchange.getResponseHeaders().add("Last-Modified", LastModified);
                        code = 200;
                        break chat;
                    }
                } catch (JSONException e) {
                    response = "Header error";
                    code = 400;
                    break chat;
                }
            } else {
                response = "Not supported";
                code = 404;
                break chat;
            }
        } catch (IOException e) {
            response = "Bad Request";
            code = 400;
            break chat;
        }
        // Sending the response to the client
        // Code and response will vary
        try {
            if (code == 204) {
                exchange.sendResponseHeaders(code, -1);
            } else {
                byte[] bytes = response.getBytes("UTF-8");
                exchange.sendResponseHeaders(code, bytes.length);
                OutputStream oStream = exchange.getResponseBody();
                oStream.write(response.getBytes());
                oStream.close();
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
