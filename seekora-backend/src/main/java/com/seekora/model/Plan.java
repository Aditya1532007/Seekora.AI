package com.seekora.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Plan {
    private int id;
    private String code;
    private String name;
    private BigDecimal priceMonthly;
    private BigDecimal priceYearly;
    private int dailySearchLimit;
    private boolean featured;
    private int sortOrder;
    private List<String> features = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPriceMonthly() { return priceMonthly; }
    public void setPriceMonthly(BigDecimal priceMonthly) { this.priceMonthly = priceMonthly; }

    public BigDecimal getPriceYearly() { return priceYearly; }
    public void setPriceYearly(BigDecimal priceYearly) { this.priceYearly = priceYearly; }

    public int getDailySearchLimit() { return dailySearchLimit; }
    public void setDailySearchLimit(int dailySearchLimit) { this.dailySearchLimit = dailySearchLimit; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }
}
