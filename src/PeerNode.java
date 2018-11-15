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
    HashMap<Integer,String> previousQueries = new HashMap<Integer, String>();


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
            getFilesList();
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
                        String join_request_message_tmp = " JOIN " + nodeSelf.ip + " " + nodeSelf.port;
                        String join_request_message = String.format("%04d", join_request_message_tmp.length() + 4)+join_request_message_tmp;
                        System.out.println("join_request_message: "+join_request_message);
                        if (payLoadParts[0]=="1" || payLoadParts[0].equals("1")){
                            Node node = new Node(payLoadParts[1],Integer.parseInt(payLoadParts[2]));
                            InetAddress ip = InetAddress.getByName(node.ip);
                            DatagramPacket sendPacket = new DatagramPacket(join_request_message.getBytes(), join_request_message.length(),ip,node.port);
                            listenerSocket.send(sendPacket);
                            routingTable.add(node);
                        }else if (payLoadParts[0]=="2" || payLoadParts[0].equals("2")){
                            Node node1 = new Node(payLoadParts[1],Integer.parseInt(payLoadParts[2]));
                            InetAddress ip = InetAddress.getByName(node1.ip);
                            DatagramPacket sendPacket = new DatagramPacket(join_request_message.getBytes(), join_request_message.length(),ip,node1.port);
                            listenerSocket.send(sendPacket);
                            Node node2 = new Node(payLoadParts[3],Integer.parseInt(payLoadParts[4]));
                            InetAddress ip1 = InetAddress.getByName(node2.ip);
                            DatagramPacket sendPacket1 = new DatagramPacket(join_request_message.getBytes(), join_request_message.length(),ip1,node2.port);
                            listenerSocket.send(sendPacket1);
                            routingTable.add(node1);
                            routingTable.add(node2);
                        }
                    } else if (receivedMessage.substring(5, 10).equals("UNROK")){
                        System.out.println("UNROK : "+receivedMessage.substring(11, len));
                        String messagePayload = receivedMessage.substring(11, len);
                        String[] payLoadParts = messagePayload.split(" ");
                        if (payLoadParts[0]=="0" || payLoadParts[0].equals("0")){
                            String leaveRequestMessageTmp = " LEAVE " + nodeSelf.ip + " " + nodeSelf.port;
                            String leaveRequestMessage = String.format("%04d", leaveRequestMessageTmp.length() + 4)+leaveRequestMessageTmp;
                            System.out.println("leaveRequestMessage: "+leaveRequestMessage);
                            for (int i = 0; i < routingTable.size(); i++) {
                                Node neighbour = routingTable.get(i);
                                InetAddress ip = InetAddress.getByName(neighbour.ip);
                                DatagramPacket sendPacket = new DatagramPacket(leaveRequestMessage.getBytes(), leaveRequestMessage.length(),ip,neighbour.port);
                                listenerSocket.send(sendPacket);
                            }
                        }
                    }else if(receivedMessage.substring(5,11).equals("JOINOK")){
                        System.out.println("JOINOK : "+receivedMessage.substring(12,len));
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
                    }else if(receivedMessage.substring(5,10).equals("LEAVE")) {
                        System.out.println("LEAVE : "+receivedMessage.substring(11, len));
                        String messagePayload = receivedMessage.substring(11, len);
                        String[] payLoadParts = messagePayload.split(" ");
                        String leaveOkMessageTmp = "";
                        Node node = null;
                        if (payLoadParts.length==2){
                            node = new Node(payLoadParts[0],formatNumber(payLoadParts[1]));
                            if (!routingTable.contains(node)){
                                routingTable.remove(node);
                            }
                            leaveOkMessageTmp = " LEAVEOK 0";
                        }else {
                            leaveOkMessageTmp = " LEAVEOK 9999";
                        }
                        String leaveOkMessage = String.format("%04d", leaveOkMessageTmp.length() + 4)+ leaveOkMessageTmp;
                        System.out.println("parseReceiveMessage|node.toString(): "+node.toString());
                        DatagramPacket sendPacket = new DatagramPacket(leaveOkMessage.getBytes(), leaveOkMessage.length(),receivePacket.getAddress(),receivePacket.getPort());
                        listenerSocket.send(sendPacket);
                    }else if(receivedMessage.substring(5,10).equals("SEROK")){
                        System.out.println("SEROK : "+receivedMessage.substring(11,len));
                    }else if(receivedMessage.substring(5,8).equals("SER")) {
                        System.out.println("SER : "+receivedMessage.substring(9, len));
                        String messagePayload = receivedMessage.substring(9, len);
                        System.out.println("messagePayload : "+messagePayload);
                        String searchQuery = messagePayload.substring(0,messagePayload.length()-3);
                        System.out.println("searchQuery : "+searchQuery);
                        String[] searchParts = searchQuery.split("\"");
                        String searchName = searchParts[1];
                        System.out.println("searchName : "+searchName);

                        String hopsCountStr = messagePayload.substring(messagePayload.length()-3,
                                messagePayload.length()).trim().replaceAll("\\r|\\n", "");
                        System.out.println("hopsCountStr : |"+hopsCountStr+"|");

                        int hopCount = Integer.parseInt(hopsCountStr);

                        ArrayList<String> findings = findFileInList(searchName,this.myFiles);
                        System.out.println("findings : "+findings.toString());
                        if (findings.isEmpty()){
                            System.out.println("previousQueries : "+this.previousQueries.toString());
                            System.out.println("searchQuery.hashCode : "+searchQuery.hashCode());
                            System.out.println("this.previousQueries.containsKey(searchQuery.hashCode()) : "
                                    +this.previousQueries.containsKey(searchQuery.hashCode()));
                            if (!this.previousQueries.containsKey(searchQuery.hashCode())){
                                System.out.println("First time broadcasted query : "+searchQuery);
                                this.previousQueries.put(searchQuery.hashCode(),searchQuery);
                                int nweHopCount = hopCount + 1;
                                String newQueryMessageTmp = " SER "+searchQuery+" "+String.format("%02d", nweHopCount);
                                String newQueryMessage = String.format("%04d", newQueryMessageTmp.length() + 4)+ newQueryMessageTmp;
                                broadCast(listenerSocket,newQueryMessage,this.routingTable);
                            }else {
                                System.out.println("Previously broadcasted query : "+searchQuery);
                            }
                        }else {
                            if (!this.previousQueries.containsKey(searchQuery.hashCode())){
                                String findingsStr = String.join(" ", findings);
                                //length SEROK no_files IP port hops filename1 filename2
                                String searchOkMessageTmp = " SEROK "+findings.size()+" "+this.nodeSelf.ip+" "+this.nodeSelf.port
                                        +" "+hopsCountStr+" "+findingsStr;
                                String searchOkMessage = String.format("%04d", searchOkMessageTmp.length() + 4)+ searchOkMessageTmp;
                                //0047 SER 129.82.62.142 5070 "Lord of the rings"
                                String[] senderInfo = searchParts[0].split(" ");
                                InetAddress ip = InetAddress.getByName(senderInfo[0]);
                                DatagramPacket sendPacket = new DatagramPacket(searchOkMessage.getBytes(),
                                        searchOkMessage.length(),ip,Integer.parseInt(senderInfo[1]));
                                listenerSocket.send(sendPacket);
                            }else {
                                System.out.println("Search result for query : "+searchQuery);
                            }
                        }
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

    private void broadCast(DatagramSocket listenerSocket, String message, ArrayList<Node> routingTable){
        for (int i = 0; i < routingTable.size(); i++) {
            Node neighbour = routingTable.get(i);
            InetAddress ip = null;
            try {
                ip = InetAddress.getByName(neighbour.ip);
                DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.length(),ip,neighbour.port);
                listenerSocket.send(sendPacket);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ArrayList<String> findFileInList(String queryName,String[] fileList){
        ArrayList<String> findings = new ArrayList<String>();
        for(String fileName: fileList){
            if (fileName.contains(queryName)){
                int similarityCount = 0;
                String[] queryWords = queryName.split(" ");
                for(String queryWord: queryWords){
                    for(String fileWord:fileName.split(" ")){
                        if (queryWord.equals(fileWord)){
                            similarityCount++;
                        }
                    }
                }
                if (similarityCount==queryWords.length){
                    findings.add(fileName);
                }
            }
        }
        return findings;
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

    public void searchQuery(String query){
        ArrayList<String> findings = findFileInList(query, this.myFiles);
        if (!findings.isEmpty()){
            for (String fileName:findings){
                System.out.println("File name: "+fileName);
            }
        }else {
            //0047 SER 129.82.62.142 5070 "Lord of the rings" 00
            String searchReqeustTmp = " SER "+this.nodeSelf.ip+" "+this.nodeSelf.port+" \""+query+"\" 00";
            String searchReqeust = String.format("%04d", searchReqeustTmp.length() + 4)+ searchReqeustTmp;
            broadCast(this.listenerSocket, searchReqeust, this.routingTable);
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