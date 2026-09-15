package com.seekora.model;

import java.time.LocalDateTime;

public class ChatMessage {
    private long id;
    private long chatSessionId;
    private String sender;
    private String message;
    private LocalDateTime createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getChatSessionId() { return chatSessionId; }
    public void setChatSessionId(long chatSessionId) { this.chatSessionId = chatSessionId; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
