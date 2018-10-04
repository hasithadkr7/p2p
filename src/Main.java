public class Main {
    public static void main(String[] args) {
        System.out.println("Starting P2P network...");
        PeerNode peerNode = new PeerNode("127.0.0.1",56551,"node1");
        PeerNode peerNode1 = new PeerNode("127.0.0.1",32212, "node2");
        PeerNode peerNode2 = new PeerNode("127.0.0.1",8761, "node3");
        PeerNode peerNode3 = new PeerNode("127.0.0.1",4211, "node4");
        PeerNode peerNode4 = new PeerNode("127.0.0.1",8937, "node5");

        peerNode.getFilesList();
        peerNode.getRountingTable();

        peerNode1.getFilesList();
        peerNode1.getRountingTable();

        peerNode2.getFilesList();
        peerNode2.getRountingTable();

        peerNode3.getFilesList();
        peerNode3.getRountingTable();

        peerNode4.getFilesList();
        peerNode4.getRountingTable();
    }
}
