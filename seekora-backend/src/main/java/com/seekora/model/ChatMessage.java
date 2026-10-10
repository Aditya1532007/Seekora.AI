package com.seekora.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatMessage {
    private long id;
    private long chatSessionId;
    private String sender;
    private String message;
    private LocalDateTime createdAt;
    private List<Source> sources = new ArrayList<>();

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
    public List<Source> getSources() { return sources; }
    public void setSources(List<Source> sources) { this.sources = sources; }
}
