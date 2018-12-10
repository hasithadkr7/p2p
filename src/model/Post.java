package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Post {

    @JsonProperty("post_id")
    private int postId;
    @JsonProperty("timestamp")
    private long timestamp;
    @JsonProperty("node_id")
    private String nodeId;
    @JsonProperty("content")
    private String content;
    @JsonProperty("ranks")
    private List<Rank> ranks;
    @JsonProperty("comments")
    private List<Comment> comments;
    @JsonProperty("avg_rank")
    private double avgRank;

    public Post() {
        this.ranks = new ArrayList<>();
        this.comments = new ArrayList<>();
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<Rank> getRanks() {
        return ranks;
    }

    public void setRanks(List<Rank> ranks) {
        this.ranks = ranks;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public double getAvgRank() {
        return avgRank;
    }

    public void setAvgRank(double avgRank) {
        this.avgRank = avgRank;
    }

    public void addRank(Rank rank) {
        if (this.ranks.stream().anyMatch(rank1 -> rank.getNodeId().equals(rank1.getNodeId()))) {
            this.ranks.forEach(rank1 -> {
                if (rank.getNodeId().equals(rank1.getNodeId())) {
                    rank1.setRankValue(rank.getRankValue());
                }
            });
        } else {
            this.ranks.add(rank);
        }
        this.avgRank = ((this.avgRank * (this.ranks.size() - 1)) + rank.getRankValue())/this.ranks.size();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Post)) return false;
        Post post = (Post) o;
        return postId == post.postId &&
                timestamp == post.timestamp &&
                Objects.equals(nodeId, post.nodeId) &&
                Objects.equals(content, post.content) &&
                Objects.equals(ranks, post.ranks) &&
                Objects.equals(comments, post.comments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, timestamp, nodeId, content, ranks, comments);
    }
}
