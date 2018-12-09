package model;

import java.util.Objects;

public class Rank {

    private String nodeId;
    private int rankValue;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public int getRankValue() {
        return rankValue;
    }

    public void setRankValue(int value) {
        if ((value < 1) || (value > 5))
            throw new IllegalArgumentException("value is out of range for Rank");
        this.rankValue = value;
    }

    @Override
    public String toString() {
        return "Rank{" +
                "nodeId='" + nodeId + '\'' +
                ", rankValue=" + rankValue +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rank)) return false;
        Rank rank = (Rank) o;
        return rankValue == rank.rankValue &&
                Objects.equals(nodeId, rank.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, rankValue);
    }
}
