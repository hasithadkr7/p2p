import model.Rank;

public final class JsonUtils {

    public static final Rank getRank(int rankValue, String username) {
        // if a rank is already there by a node. just replace. it.

        Rank rank = new Rank();
        rank.setNodeId(username);
        rank.setRankValue(rankValue);

        return rank;
    }
}
