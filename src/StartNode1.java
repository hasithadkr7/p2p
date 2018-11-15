public class StartNode1 {
    public static void main(String[] args) {
        PeerNode peerNode = new PeerNode("127.0.0.1",56551,"node1");
        //PeerNode peerNode1 = new PeerNode("127.0.0.1",32212, "node2");
//        peerNode.start();
        peerNode.getRountingTable();
        peerNode.getFilesList();
    }
}
