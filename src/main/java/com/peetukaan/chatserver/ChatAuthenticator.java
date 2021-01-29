package com.peetukaan.chatserver;

import java.util.Hashtable;
import java.util.Map;

public class ChatAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {

    private Map<String,String> users = null;

    public ChatAuthenticator(String realm) {
        super(realm);
        users = new Hashtable<String,String>();
        users.put("dummy", "passwd");

    }

    @Override
    public boolean checkCredentials(String username, String password) {
        for (Map.Entry<String, String> entry : users.entrySet()) {
            String user = entry.getKey();
            String passwd = entry.getValue();
            if (user.equals(username) && passwd.equals(password)) {
                return true;
            } 
        }
        return false;
    }
    
    public boolean addUser(String userName, String password) { 
     if (userName.isEmpty() || password.isEmpty()) {
         return false;
     } 
     if (users.containsKey(userName)) {
        return false;
     }
    users.put(userName, password);
    return true;
    }
}
