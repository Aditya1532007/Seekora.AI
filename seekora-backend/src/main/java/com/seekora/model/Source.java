package com.seekora.model;

public class Source {
    private long id;
    private String title;
    private String url;
    private String sourceType;

    public Source() {
    }

    public Source(long id, String title, String url, String sourceType) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.sourceType = sourceType;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
}
