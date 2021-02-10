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

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.DriverManager;

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
        try {
            open("chatDatabase.db");
        } catch (SQLException e) {
            System.out.println("Log - SQLexception");
        }
    }

    public void open(String dbName) throws SQLException {
        String path = "jdbc:sqlite:" + dbName;
        database = DriverManager.getConnection(path);
        try {
            initializeDatabase(database, dbName);
            System.out.println("Database connection established!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void initializeDatabase(Connection database, String dbName) throws SQLException {
        PreparedStatement dataTable  = database.prepareStatement("CREATE TABLE IF NOT EXISTS Data (\n"  
		            + " id INTEGER PRIMARY KEY,\n"  
		            + " username TEXT NOT NULL,\n"  
		            + " message TEXT NOT NULL,\n"
		            + " time INTEGER NOT NULL \n"
		            + ");");
        
        PreparedStatement userTable  = database.prepareStatement("CREATE TABLE IF NOT EXISTS Users (\n"  
		            + " id INTEGER PRIMARY KEY,\n"  
		            + " username TEXT NOT NULL UNIQUE,\n"  
		            + " password TEXT NOT NULL,\n"
		            + " email TEXT NOT NULL \n"
		            + ");");                

		dataTable.executeUpdate();
        userTable.executeUpdate();
        dataTable.close();
		userTable.close();
    }

    public void addUser(String user, String password, String email) throws SQLException {
        PreparedStatement statement = database.prepareStatement("INSERT INTO Users (username,password,email) VALUES (?,?,?)");
        statement.setString(1, user);
        statement.setString(2, password);
        statement.setString(3, email);
        try {
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        if (statement != null) {
            statement.close();
        }
    }

    public void addMessage(String user, String message, long time) throws SQLException {
        PreparedStatement statement = database.prepareStatement("INSERT INTO Data (username,message,time) VALUES (?,?,?)");
        statement.setString(1, user);
        statement.setString(2, message);
        statement.setLong(3, time);
        try {
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        if (statement != null) {
            statement.close();
        }
    }

    public boolean checkUser(String username, String password) throws SQLException {
        PreparedStatement statement = database.prepareStatement("SELECT * from Users WHERE username =? LIMIT 1");
		statement.setString(1,username);
        try {
            ResultSet result = statement.executeQuery();
            String dbuserString = result.getString("username");
            String dbPassword = result.getString("password");
            if (username.equals(dbuserString) && password.equals(dbPassword)) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean checkUsername(String username) throws SQLException {
        PreparedStatement statement = database.prepareStatement("SELECT * from Users WHERE username =? LIMIT 1");
		statement.setString(1,username);
        try {
            ResultSet result = statement.executeQuery();
            String dbuserString = result.getString("username");
            if (dbuserString.equals(username)) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            return false;
        }
    }

   public JSONArray getMessages() throws SQLException {
        JSONArray objs = new JSONArray();
        PreparedStatement statement = database.prepareStatement("SELECT * from Data");
		try {
			ResultSet result = statement.executeQuery();
			while (result.next()) {
                String dbUser = result.getString("username");
                String dbMessage = result.getString("message");
                Long dbTime = result.getLong("time");
                JSONObject obj = new JSONObject();
                LocalDateTime sent = LocalDateTime.ofInstant(Instant.ofEpochMilli(dbTime), ZoneOffset.UTC);
                ChatMessage newMessage = new ChatMessage(sent, dbUser, dbMessage);
                ZonedDateTime toSend = ZonedDateTime.of(newMessage.getSent(), ZoneId.of("UTC"));
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                String dateText = toSend.format(formatter);
                obj.put("sent", dateText);
                obj.put("user", newMessage.getNick());
                obj.put("message", newMessage.getMessage());
                objs.put(obj);
            }
        } catch (SQLException e) {
        }
        return objs;
    } 
}