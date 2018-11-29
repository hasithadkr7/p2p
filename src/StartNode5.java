public class StartNode5 {
    public static void main(String[] args) {
        //PeerNode peerNode = new PeerNode("127.0.0.1",56551,"node1");
        PeerNode peerNode = new PeerNode("127.0.0.1",56555, "node5");
        //peerNode.getRountingTable();
//        peerNode1.start();
        peerNode.getRountingTable();
        peerNode.getFilesList();
    }
}
