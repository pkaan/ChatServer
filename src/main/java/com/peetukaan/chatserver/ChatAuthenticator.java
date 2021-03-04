package com.peetukaan.chatserver;

public class ChatAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {

    ChatDatabase database = ChatDatabase.getInstance();

    public ChatAuthenticator(String realm) {
        super(realm);
    }

    // Checking if the username & the password match
    @Override
    public boolean checkCredentials(String username, String password) {
        boolean validCredentials;
        validCredentials = database.checkUsernamePassword(username, password);
        return validCredentials;
    }

    // Adding user to the database;
    public void addUser(String username, String password, String email) {
        User newUser = new User(username, password, email);
        database.addUser(newUser.getName(), newUser.getPassword(), newUser.getEmail());
    }

    // checking if the username is already taken
    public boolean checkUser(String username, String password, String email) {
        boolean UsernameTaken = false;
        UsernameTaken = database.checkIfUserExists(username);
        return UsernameTaken;
    }

}