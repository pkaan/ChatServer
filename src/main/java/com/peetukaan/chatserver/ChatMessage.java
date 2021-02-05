package com.peetukaan.chatserver;

import java.time.LocalDateTime;

public class ChatMessage {

    private LocalDateTime sent;
    private String nick;
    private String message;

    public ChatMessage(LocalDateTime sent, String nick, String message) {
        this.sent = sent;
        this.nick = nick;
        this.message = message;
    } 
    
    public LocalDateTime getSent() {
        return this.sent;
    }

    public String getNick() {
        return this.nick;
    }

    public String getMessage() {
        return this.message;
    }
}
