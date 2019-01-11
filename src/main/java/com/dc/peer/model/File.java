package com.dc.peer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class File extends BaseFeedItem {

    // has a

    @JsonProperty("file_name")
    private String fileName;
    @JsonProperty("url")
    private String url;
//    @JsonProperty("ranks")
//    private List<Rank> ranks;
//    @JsonProperty("comments")
//    private List<Comment> comments;
//    @JsonProperty("avg_rank")
//    private double avgRank;

    public File() {
        this.ranks = new ArrayList<>();
        this.comments = new ArrayList<>();
    }

    @JsonProperty("file_id")
    public int getFileId() {
        return itemId;
    }

    public void setFileId(int fileId) {
        this.itemId = fileId;
    }

    @JsonProperty("timestamp")
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @JsonProperty("node_id")
    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @JsonProperty("ranks")
    public List<Rank> getRanks() {
        return ranks;
    }

    public void setRanks(List<Rank> ranks) {
        this.ranks = ranks;
    }

    @JsonProperty("comments")
    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    @JsonProperty("avg_rank")
    public double getAvgRank() {
        return avgRank;
    }

    public void setAvgRank(double avgRank) {
        this.avgRank = avgRank;
    }


    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}

