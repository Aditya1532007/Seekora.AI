package com.seekora.model;

public class Testimonial {
    private int id;
    private String authorName;
    private String authorTitle;
    private String initials;
    private int rating;
    private String content;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorTitle() { return authorTitle; }
    public void setAuthorTitle(String authorTitle) { this.authorTitle = authorTitle; }

    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = initials; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
