public class Main {
    public static void main(String[] args) {
        System.out.println("Starting P2P network...");
        PeerNode peerNode = new PeerNode("127.0.0.1",56551,"node1");
        PeerNode peerNode1 = new PeerNode("127.0.0.1",56552, "node2");
        PeerNode peerNode2 = new PeerNode("127.0.0.1",56553, "node3");
        PeerNode peerNode3 = new PeerNode("127.0.0.1",56554, "node4");
        PeerNode peerNode4 = new PeerNode("127.0.0.1",56555, "node5");
//        peerNode.start();
//        peerNode1.start();
//        peerNode2.start();
    }
}
