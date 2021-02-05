package com.peetukaan.chatserver;

import java.util.Hashtable;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class ChatAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {

    private Map<String, User> users = null;

    public ChatAuthenticator(String realm) {
        super(realm);
        users = new Hashtable<String,User>();
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        for (Map.Entry<String, User> entry : users.entrySet()) {
            String user = entry.getKey();
            String passwd = entry.getValue().getPassword();
            if (user.equals(username) && passwd.equals(password)) {
                return true;
            } 
        }
        return false;
    }
    
    public boolean addUser(String username, String password, String email) { 
        try {
            if (username.isEmpty() || password.isEmpty()) {
                return false;
            } 
            if (users.containsKey(username)) {
                return false;
            }
            User newUser = new User(username, password, email);
            users.put(username, newUser);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }
}