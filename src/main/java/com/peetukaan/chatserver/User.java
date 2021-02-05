package com.peetukaan.chatserver;

public class User {

    private String username;
    private String password;
    private String email;

    public User (String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getName() {
        return this.password;
    }

    public String getPassword() {
        return this.password;
    }
}
