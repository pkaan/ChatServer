package com.peetukaan.chatserver;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class ChatMessage {

    private LocalDateTime sent;
    private String nick;
    private String message;

    public ChatMessage(LocalDateTime sent, String nick, String message) {
        this.sent = sent;
        this.nick = nick;
        this.message = message;
    }

    public long dateAsInt() {
        return sent.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    public void setSent(long epoch) {
        sent = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
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
