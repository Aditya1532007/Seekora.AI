package com.seekora.model;

public class PlatformStat {
    private int id;
    private String label;
    private int value;
    private String suffix;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }

    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }
}
