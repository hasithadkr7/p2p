import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

public class PeerNode{
    ArrayList<Node> routingTable;
    HashMap<String, Node> fileListMap;
    String[] myFiles;
    Node nodeSelf;
    Communicator communicator;
    DatagramSocket listenerSocket = null;


    public PeerNode(String my_ip, int my_port, String my_username) {
        //nodeSelf = new Node(InitConfig.my_ip, InitConfig.my_port);
        nodeSelf = new Node(my_ip, my_port);
        nodeSelf.setUserName(my_username);
        try {
            listenerSocket = new DatagramSocket(my_port);
            routingTable = new ArrayList<Node>();
            fileListMap = new HashMap<String, Node>();
            communicator = new Communicator(this);
            myFiles = InitConfig.getRandomFiles();
            initFileMap();
            listen();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }


    public void listen(){
        boolean init=true;
        try {
            while (true) {
                if (init==true){
                    String register_message_tmp = " REG " + nodeSelf.ip + " " + nodeSelf.port + " " + nodeSelf.getUserName();
                    String register_message = String.format("%04d", register_message_tmp.length() + 4)+ register_message_tmp;
                    System.out.println("register_message: "+register_message);
                    InetAddress bootIp = InetAddress.getByName(InitConfig.bootstrap_ip);
                    DatagramPacket sendPacket = new DatagramPacket(register_message.getBytes(), register_message.length(),bootIp,InitConfig.bootstrap_port);
                    listenerSocket.send(sendPacket);
                    init = false;
                }
                System.out.println("Waiting for Incomming..");
                byte[] buffer = new byte[65536];
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                listenerSocket.receive(receivePacket);
                byte[] data = receivePacket.getData();
                String receivedMessage = new String(data, 0, receivePacket.getLength());
                System.out.println("listen|port: "+nodeSelf.port+"|receivedMessage : "+receivedMessage);
                if (receivedMessage !=null){
                    int len = receivedMessage.length();
                    if (receivedMessage.substring(5, 10).equals("REGOK")) {
                        System.out.println("REGOK : "+receivedMessage.substring(11, len));
                        String messagePayload = receivedMessage.substring(11, len);
                        String[] payLoadParts = messagePayload.split(" ");
                        if (payLoadParts[0]=="1" || payLoadParts[0].equals("1")){
                            Node node = new Node(payLoadParts[1],Integer.parseInt(payLoadParts[2]));
                            routingTable.add(node);
                            System.out.println("registerRequest|routingTable.size(): "+routingTable.size());
                        }else if (payLoadParts[0]=="2" || payLoadParts[0].equals("2")){
                            Node node1 = new Node(payLoadParts[1],Integer.parseInt(payLoadParts[2]));
                            Node node2 = new Node(payLoadParts[3],Integer.parseInt(payLoadParts[4]));
                            routingTable.add(node1);
                            routingTable.add(node2);
                            System.out.println("registerRequest|routingTable.size(): "+routingTable.size());
                        }
                        String join_request_message_tmp = " JOIN " + nodeSelf.ip + " " + nodeSelf.port;
                        String join_request_message = String.format("%04d", join_request_message_tmp.length() + 4)+join_request_message_tmp;
                        System.out.println("join_request_message: "+join_request_message);
                        for (int i = 0; i < routingTable.size(); i++) {
                            Node neighbour = routingTable.get(i);
                            InetAddress ip = InetAddress.getByName(neighbour.ip);
                            DatagramPacket sendPacket = new DatagramPacket(join_request_message.getBytes(), join_request_message.length(),ip,neighbour.port);
                            listenerSocket.send(sendPacket);
                        }
                    } else if (receivedMessage.substring(5, 10).equals("UNROK")){
                        System.out.println("UNROK : "+receivedMessage.substring(11, len));
                        String messagePayload = receivedMessage.substring(11, len);
                        String[] payLoadParts = messagePayload.split(" ");
                    }else if(receivedMessage.substring(5,11).equals("JOINOK")){
                        System.out.println("JOINOK : "+receivedMessage.substring(12,len));
                        String messagePayload = receivedMessage.substring(12, len);
                        String[] payLoadParts = messagePayload.split(" ");
                    }else if(receivedMessage.substring(5,9).equals("JOIN")) {
                        System.out.println("JOIN : "+receivedMessage.substring(10, len));
                        String messagePayload = receivedMessage.substring(10, len);
                        String[] payLoadParts = messagePayload.split(" ");
                        String joinOkMessageTmp = "";
                        Node node = null;
                        if (payLoadParts.length==2){
                            node = new Node(payLoadParts[0],formatNumber(payLoadParts[1]));
                            if (!routingTable.contains(node)){
                                routingTable.add(node);
                            }
                            joinOkMessageTmp = " JOINOK 0";
                        }else {
                            joinOkMessageTmp = " JOINOK 9999";
                        }
                        String joinOkMessage = String.format("%04d", joinOkMessageTmp.length() + 4)+ joinOkMessageTmp;
                        System.out.println("parseReceiveMessage|node.toString(): "+node.toString());
                        DatagramPacket sendPacket = new DatagramPacket(joinOkMessage.getBytes(), joinOkMessage.length(),receivePacket.getAddress(),receivePacket.getPort());
                        listenerSocket.send(sendPacket);
                    }else if(receivedMessage.substring(5,12).equals("LEAVEOK")) {
                        System.out.println("LEAVEOK : "+receivedMessage.substring(13, len));
                        String messagePayload = receivedMessage.substring(13, len);
                        String[] payLoadParts = messagePayload.split(" ");
                    }else if(receivedMessage.substring(5,10).equals("LEAVE")) {
                        System.out.println("LEAVE : "+receivedMessage.substring(11, len));
                        String messagePayload = receivedMessage.substring(11, len);
                        String[] payLoadParts = messagePayload.split(" ");
                    }else if(receivedMessage.substring(5,10).equals("SEROK")){
                        System.out.println("SEROK : "+receivedMessage.substring(11,len));
                        String messagePayload = receivedMessage.substring(11, len);
                        String[] payLoadParts = messagePayload.split(" ");
                    }else if(receivedMessage.substring(5,8).equals("SER")) {
                        System.out.println("SER : "+receivedMessage.substring(9, len));
                        String messagePayload = receivedMessage.substring(9, len);
                        String[] payLoadParts = messagePayload.split(" ");
                    }else {
                        System.out.println("Unmactched Message : "+receivedMessage);
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void initFileMap() {
        for (String f : myFiles) {
            fileListMap.put(f, nodeSelf);
        }
    }

    private int formatNumber(String numberString){
        String whatever = numberString.trim();
        byte[] bytes = whatever.getBytes();
        String str = new String(bytes, Charset.forName("UTF-8"));
        return Integer.parseInt(str);
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