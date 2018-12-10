package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Rank {

    @JsonProperty("node_id")
    private String nodeId;
    @JsonProperty("rank")
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
        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.writeValueAsString(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
//        return "Rank{" +
//                "nodeId='" + nodeId + '\'' +
//                ", rankValue=" + rankValue +
//                '}';
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
