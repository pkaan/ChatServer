package com.peetukaan.chatserver;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class ChatMessage {

    private LocalDateTime sent;
    private String nickname;
    private String message;
    private String weatherInfo;

    // Constructor for the message. WeatherInfo is optional.
    // Weather info will be added if the user has given a correct location (a city),
    // and a correct time (No older than 168.000000 hours (from now)).
    public ChatMessage(LocalDateTime sent, String nick, String message) {
        this.sent = sent;
        this.nickname = nick;
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
        return this.nickname;
    }

    public String getMessage() {
        return this.message;
    }

    public void setWeatherInfo(String info) {
        this.weatherInfo = info;
    }

    public String getWeatherInfo() {
        return this.weatherInfo;
    }
}
