package com.peetukaan.chatserver;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import org.apache.commons.codec.digest.Crypt;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.DriverManager;
import java.security.SecureRandom;
import java.sql.Connection;

public class ChatDatabase {

    private static ChatDatabase singleton = null;
    Connection database = null;

    public static synchronized ChatDatabase getInstance() {
        if (null == singleton) {
            singleton = new ChatDatabase();
        }
        return singleton;
    }

    private ChatDatabase() {
    }

    public void open(String dbName) {
        try {
            String path = "jdbc:sqlite:" + dbName;
            database = DriverManager.getConnection(path);
            initializeDatabase(database, dbName);
            System.out.println("Database connection established!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // Initializing the database
    public void initializeDatabase(Connection database, String dbName) {
        try {
            // Preparing a statement to create a a database table Data if not exists
            // Tables: id, username, message, time, weather(optional)
            PreparedStatement statementDataTable = database.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS Data (\n" + " id INTEGER PRIMARY KEY,\n" + " username TEXT NOT NULL,\n"
                            + " message TEXT NOT NULL,\n" + " time INTEGER NOT NULL,\n" + " weather TEXT \n" + ");");
            // Preparing a statement to create a a database table User if not exists
            // Tables: id, username, password, salt, email
            PreparedStatement statementUserTable = database.prepareStatement("CREATE TABLE IF NOT EXISTS Users (\n"
                    + " id INTEGER PRIMARY KEY,\n" + " username TEXT NOT NULL UNIQUE,\n" + " password TEXT NOT NULL,\n"
                    + " salt TEXT NOT NULL,\n" + " email TEXT NOT NULL \n" + ");");
            // Executing statements
            statementDataTable.executeUpdate();
            statementDataTable.close();
            statementUserTable.executeUpdate();
            statementUserTable.close();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Adding user to the database
    public void addUser(String user, String password, String email) {
        try {
            // Creating the salt and hashed password
            SecureRandom secureRandom = new SecureRandom();
            byte bytes[] = new byte[13];
            secureRandom.nextBytes(bytes);
            String saltBytes = new String(Base64.getEncoder().encode(bytes));
            String salt = "$6$" + saltBytes;
            String hashedPassword = Crypt.crypt(password, salt);
            // Creating a row to the Users table with the user information
            PreparedStatement statement = database
                    .prepareStatement("INSERT INTO Users (username,password,salt,email) VALUES (?,?,?,?)");
            statement.setString(1, user);
            statement.setString(2, hashedPassword);
            statement.setString(3, salt);
            statement.setString(4, email);
            statement.executeUpdate();
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Adding message to the database
    public void addMessage(ChatMessage message, long unixTime) {
        // If the message has some weather information attached to it, it will be stored
        // into database as well
        try {
            if (message.getWeatherInfo() != null) {
                PreparedStatement statement = database
                        .prepareStatement("INSERT INTO Data (username,message,time,weather) VALUES (?,?,?,?)");
                statement.setString(1, message.getNick());
                statement.setString(2, message.getMessage());
                statement.setLong(3, unixTime);
                statement.setString(4, message.getWeatherInfo());
                statement.executeUpdate();
                statement.close();
            // No weather information attached
            } else {
                PreparedStatement statement = database
                        .prepareStatement("INSERT INTO Data (username,message,time) VALUES (?,?,?)");
                statement.setString(1, message.getNick());
                statement.setString(2, message.getMessage());
                statement.setLong(3, unixTime);
                statement.executeUpdate();
                statement.close();
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Returns True if username and password match
    // Returns False if username and password do not match
    public boolean checkUsernamePassword(String username, String password) {
        try {
            PreparedStatement statement = database.prepareStatement("SELECT * from Users WHERE username =? LIMIT 1");
            statement.setString(1, username);
            ResultSet result = statement.executeQuery();
            String dbuserString = result.getString("username");
            String dbPassword = result.getString("password");
            result.close();
            statement.close();
            if (username.equals(dbuserString) && (dbPassword.equals(Crypt.crypt(password, dbPassword)))) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    // Returns true if username already exists in the database
    public boolean checkIfUserExists(String username) {
        try {
            PreparedStatement statement = database.prepareStatement("SELECT * from Users WHERE username =? LIMIT 1");
            statement.setString(1, username);
            ResultSet result = statement.executeQuery();
            String dbuserString = result.getString("username");
            result.close();
            statement.close();
            if (dbuserString.equals(username)) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    // Gets all the messages
    public JSONArray getMessages() {
        JSONArray objs = new JSONArray();
        try {
            PreparedStatement statement = database.prepareStatement("SELECT * from Data");
            ResultSet result = statement.executeQuery();
            int messageCount = 0;
            // 100 messages is the limit.
            while (result.next() && messageCount < 100) {
                JSONObject obj = new JSONObject();
                String dbUser = result.getString("username");
                String dbMessage = result.getString("message");
                Long dbTime = result.getLong("time");
                LocalDateTime sent = LocalDateTime.ofInstant(Instant.ofEpochMilli(dbTime), ZoneOffset.UTC);
                ChatMessage newMessage = new ChatMessage(sent, dbUser, dbMessage);
                ZonedDateTime toSend = ZonedDateTime.of(newMessage.getSent(), ZoneId.of("UTC"));
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                String dateText = toSend.format(formatter);
                // Putting data to JSON. No weather data available.
                if (result.getString("weather") == null) {
                    obj.put("sent", dateText);
                    obj.put("user", newMessage.getNick());
                    obj.put("message", newMessage.getMessage());
                    // Putting data to JSON. Weather data available.
                } else {
                    String weatherInfo = result.getString("weather");
                    obj.put("sent", dateText);
                    obj.put("user", newMessage.getNick());
                    obj.put("message", newMessage.getMessage());
                    obj.put("weather", weatherInfo);
                }
                // Putting JSONObject in JSONArray.
                objs.put(obj);
                // Counting the messages (limit 100).
                messageCount++;
            }
            result.close();
            statement.close();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        // Returns JSONArray
        return objs;
    }

    // Get the messages the Client has not yet seen
    public JSONArray getMessages(long since) {
        JSONArray objs = new JSONArray();
        try {
            PreparedStatement statement = database
                    .prepareStatement("SELECT * from Data where time >? order by time asc;");
            statement.setLong(1, since);
            ResultSet result = statement.executeQuery();
            int messageCount = 0;
            // 100 messages is the limit.
            while (result.next() && messageCount < 100) {
                JSONObject obj = new JSONObject();
                String dbUser = result.getString("username");
                String dbMessage = result.getString("message");
                Long dbTime = result.getLong("time");
                LocalDateTime sent = LocalDateTime.ofInstant(Instant.ofEpochMilli(dbTime), ZoneOffset.UTC);
                ChatMessage newMessage = new ChatMessage(sent, dbUser, dbMessage);
                ZonedDateTime toSend = ZonedDateTime.of(newMessage.getSent(), ZoneId.of("UTC"));
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                String dateText = toSend.format(formatter);
                // Putting data to JSON. No weather data available.
                if (result.getString("weather") == null) {
                    obj.put("sent", dateText);
                    obj.put("user", newMessage.getNick());
                    obj.put("message", newMessage.getMessage());
                    // Putting data to JSON. Weather data available.
                } else {
                    String weatherInfo = result.getString("weather");
                    obj.put("sent", dateText);
                    obj.put("user", newMessage.getNick());
                    obj.put("message", newMessage.getMessage());
                    obj.put("weather", weatherInfo);
                }
                // Putting JSONObject in JSONArray.
                objs.put(obj);
                // Counting the messages (limit 100).
                messageCount++;
            }
            result.close();
            statement.close();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return objs;
    }

    // Returns Last-Modified Header information
    public String getLastModified() {
        String lastModified = null;
        try {
            PreparedStatement statement = database.prepareStatement("SELECT * from Data");
            ResultSet result = statement.executeQuery();
            LocalDateTime modified = LocalDateTime.parse("0000-01-01T00:00:00");
            while (result.next()) {
                Long dbTime = result.getLong("time");
                LocalDateTime sent = LocalDateTime.ofInstant(Instant.ofEpochMilli(dbTime), ZoneOffset.UTC);
                if (sent.compareTo(modified) > 0) {
                    modified = sent;
                }
            }
            result.close();
            statement.close();
            ZonedDateTime sendToHeader = ZonedDateTime.of(modified, ZoneId.of("GMT"));
            DateTimeFormatter formatterGMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss.SSS 'GMT'");
            lastModified = sendToHeader.format(formatterGMT);
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return lastModified;
    }

    // Closing the database connection
    public void close() {
        try {
            database.close();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}