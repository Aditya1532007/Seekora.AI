package com.seekora.model;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeEntry {
    private long id;
    private String topic;
    private String keywords;
    private String summary;
    private int accuracy;
    private List<Source> sources = new ArrayList<>();

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public int getAccuracy() { return accuracy; }
    public void setAccuracy(int accuracy) { this.accuracy = accuracy; }

    public List<Source> getSources() { return sources; }
    public void setSources(List<Source> sources) { this.sources = sources; }
}
