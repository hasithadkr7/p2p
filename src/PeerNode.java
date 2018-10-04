import java.util.ArrayList;
import java.util.HashMap;

public class PeerNode {
    ArrayList<Node> routingTable;
    HashMap<String, Node> fileListMap;
    String[] myFiles;
    Node nodeSelf;
    Communicator communicator;


    public PeerNode(String my_ip, int my_port, String my_username) {
        //nodeSelf = new Node(InitConfig.my_ip, InitConfig.my_port);
        nodeSelf = new Node(my_ip, my_port);
        nodeSelf.setUserName(my_username);
        //nodeSelf.setUserName(InitConfig.my_username);
        routingTable = new ArrayList<Node>();
        fileListMap = new HashMap<String, Node>();
        communicator = new Communicator(this);

        myFiles = InitConfig.getRandomFiles();
        initFileMap();

        registerRequest();
        joinRequest();
    }

    private void initFileMap() {
        for (String f : myFiles) {
            fileListMap.put(f, nodeSelf);
        }
    }

    private void registerRequest() {
        String register_message_tmp = " REG " + nodeSelf.ip + " " + nodeSelf.port + " " + nodeSelf.getUserName();
        String register_message = String.format("%04d", register_message_tmp.length() + 4)+ register_message_tmp;
        System.out.println("register_message: "+register_message);
        communicator.sendMessage(InitConfig.bootstrap_ip, InitConfig.bootstrap_port, register_message, InitConfig.MessageType.MSG_REG);
    }

    private void unregisterRequest() {
        String unregister_message_tmp = " UNREG " + nodeSelf.ip + " " + nodeSelf.port + " " + nodeSelf.getUserName();
        String unregister_message = String.format("%04d", unregister_message_tmp.length() + 4)+unregister_message_tmp;
        communicator.sendMessage(InitConfig.bootstrap_ip, InitConfig.bootstrap_port, unregister_message,InitConfig.MessageType.MSG_UNREG);
    }

    private void joinRequest() {
        String join_request_message_tmp = " JOIN " + nodeSelf.ip + " " + nodeSelf.port;
        String join_request_message = String.format("%04d", join_request_message_tmp.length() + 4)+join_request_message_tmp;
        System.out.println("join_request_message: "+join_request_message);
        for (int i = 0; i < routingTable.size(); i++) {
            Node neighbour = routingTable.get(i);
            communicator.sendMessage(neighbour.ip, neighbour.port, join_request_message, InitConfig.MessageType.MSG_JOIN);
        }
    }

    private void leaveRequest() {
        String leave_request_message_tmp = " LEAVE " + nodeSelf.ip + " " + nodeSelf.port;
        String leave_request_message = String.format("%04d", leave_request_message_tmp.length() + 4)+leave_request_message_tmp;
        for (int i = 0; i < routingTable.size(); i++) {
            Node neighbour = routingTable.get(i);
            communicator.sendMessage(neighbour.ip, neighbour.port, leave_request_message, InitConfig.MessageType.MSG_LEAVE);
        }
    }

    public void getFilesList() {
        System.out.println("----------------------------------------------------------------------------");
        System.out.println(fileListMap.toString());
        System.out.println("----------------------------------------------------------------------------");
        System.out.println("");
    }
    public void getRountingTable() {
        System.out.println("----------------------------------------------------------------------------");
        System.out.println(routingTable.toString());
        System.out.println("----------------------------------------------------------------------------");
        System.out.println("");
    }
}

class Node {
    public String ip;
    public int port;
    public String userName;

    public Node(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void setUserName(String userName){
        this.userName = userName;
    }

    public String getUserName(){
        return this.userName;
    }

    @Override
    public String toString() {
        return "Node{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }
}