package com.peetukaan.chatserver;

import java.sql.SQLException;


public class ChatAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {

    ChatDatabase database = ChatDatabase.getInstance();

    public ChatAuthenticator(String realm) {
        super(realm);
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        boolean credentials;
        try {
            credentials = database.checkUser(username, password);
        } catch (SQLException e) {
            return false;
        }
        return credentials;
    }

    public boolean addUser(String username, String password, String email) {
        try {
            User newUser = new User(username, password, email);
            boolean userExists = database.checkUsername(username);
            if (userExists == false) {
                database.addUser(newUser.getName(), newUser.getPassword(), newUser.getEmail());
                return true;
            }
            else {
                return false;
            }
        } catch (SQLException e) {
            return false;
        }
    }
}