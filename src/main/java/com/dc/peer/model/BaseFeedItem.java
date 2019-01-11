package com.dc.peer.model;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseFeedItem {

    protected long timestamp;
    protected String nodeId;
    protected int itemId;
    protected List<Rank> ranks;
    protected List<Comment> comments;
    protected double avgRank;

    protected BaseFeedItem() {
        this.ranks = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.avgRank = 0.0;
    }
    // can add a rank.
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
}
