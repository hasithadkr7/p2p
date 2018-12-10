import model.Post;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class PeerNodeNewTest {

    @Test
    public void testAddForumPost() {

        String postContent = "This is a Post. ";
        PeerNodeNew nodeNew = new PeerNodeNew("127.0.0.1", 12345, "testNode");
        nodeNew.addForumPost(postContent);
    }
}