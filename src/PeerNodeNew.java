import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PeerNodeNew{
    ArrayList<Node> routingTable;
    String[] filesList;
    Node nodeSelf;
    DatagramSocket listenerSocket = null;
    HashMap<Integer, String> previousQueries = new HashMap<Integer, String>();
    private int leaveRequestCount = 0;


    public PeerNodeNew(String my_ip, int my_port, String my_username) {
        nodeSelf = new Node(my_ip, my_port);
        nodeSelf.setUserName(my_username);
        try {
            listenerSocket = new DatagramSocket(my_port);
            routingTable = new ArrayList<Node>();
            filesList = InitConfig.getRandomFiles();
            getFilesList();
            sendRegisterRequest();
            listen();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }


    public void listen() {
        (new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        System.out.println("Waiting for Incomming...");
                        byte[] buffer = new byte[65536];
                        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                        listenerSocket.receive(receivePacket);
                        byte[] data = receivePacket.getData();
                        String receivedMessage = new String(data, 0, receivePacket.getLength());
                        System.out.println("listen|port: "+nodeSelf.port+"|receivedMessage : "+receivedMessage);

                        StringTokenizer st = new StringTokenizer(receivedMessage, " ");
                        String length = st.nextToken();
                        String command = st.nextToken().trim();
                        System.out.println("command : "+command);
                        if (command.equals("REGOK")) {
                            //0051 REGOK 2 129.82.123.45 5001 64.12.123.190 34001
                            int peerCount = Integer.parseInt(st.nextToken());
                            System.out.println("peerCount : "+peerCount);
                            if (peerCount==1){
                                String ip = st.nextToken();
                                int port = Integer.parseInt(st.nextToken().trim());
                                Node node = new Node(ip,port);
                                routingTable.add(node);
                                sendJoinRequest(node);
                            }
                            else if (peerCount==2){
                                String ip1 = st.nextToken();
                                int port1 = Integer.parseInt(st.nextToken().trim());
                                Node node1 = new Node(ip1,port1);
                                routingTable.add(node1);
                                sendJoinRequest(node1);
                                String ip2 = st.nextToken();
                                int port2 = Integer.parseInt(st.nextToken().trim());
                                Node node2 = new Node(ip2,port2);
                                routingTable.add(node2);
                                sendJoinRequest(node2);
                            }
                        }
                        else if(command.equals("JOIN") && st.hasMoreTokens()){
                            //0027 JOIN 64.12.123.190 432
                            String ip = st.nextToken();
                            int port = Integer.parseInt(st.nextToken().trim());
                            Node node = new Node(ip,port);
                            boolean success = false;
                            if (!routingTable.contains(node)){
                                try {
                                    routingTable.add(node);
                                    success = true;
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                            sendJoinOk(node, success);
                        }
                        else if(command.equals("JOINOK") && st.hasMoreTokens()){
                            //0014 JOINOK 0
                            int result = Integer.parseInt(st.nextToken().trim());
                            if (result==0){
                                System.out.println("0 – successful");
                            }else if (result==9999){
                                System.out.println("9999 – error while adding new node to routing table");
                            }
                        }
                        else if(command.equals("LEAVEOK") && st.hasMoreTokens()){
                            //0014 LEAVEOK 0
                            int result = Integer.parseInt(st.nextToken().trim());
                            if (result==0){
                                System.out.println("Node successfully leaves the distributed system.");
                            }else if (leaveRequestCount<2){
                                TimeUnit.SECONDS.sleep(1);
                                leaveRequest();
                            }
                        }
                        else if(command.equals("SER") && st.hasMoreTokens()){
                            //0047 SER 129.82.62.142 5070 "Lord of the rings" 2
                            String ip = st.nextToken();
                            int port = Integer.parseInt(st.nextToken().trim());
                            Node node = new Node(ip,port);
                            StringTokenizer st1 = new StringTokenizer(receivedMessage, "\"");
                            st1.nextToken().trim();
                            String searchQuery = st1.nextToken().trim();
                            int hopCount = Integer.parseInt(st1.nextToken().trim());
                            int searchKey = getHashKey(node,searchQuery);
                            if (!previousQueries.containsKey(searchKey)){
                                ArrayList<String> findings = findFileInList(searchQuery,filesList);
                                if (findings.isEmpty()){
                                    //Forward search query.
                                    forwardSearchQuery(node,searchQuery,hopCount);
                                }else {
                                    //send search ok
                                    sendSearchOk(findings,hopCount,node);
                                }
                                previousQueries.put(searchKey,searchQuery);
                            }else {
                                System.out.println("Previously searched query.");
                            }
                        }
                        else if(command.equals("SEROK") && st.hasMoreTokens()){
                            //0114 SEROK 3 129.82.128.1 2301 baby_go_home.mp3 baby_come_back.mp3 baby.mpeg
                            int findingCount = Integer.parseInt(st.nextToken().trim());
                            String ip = st.nextToken().trim();
                            int port = Integer.parseInt(st.nextToken().trim());
                            if (findingCount>0){
                                System.out.println("Successfull|Result: "+receivedMessage);
                            }
                            else if(findingCount==0){
                                System.out.println("Unsuccessfull|Result: "+receivedMessage);
                            }else {
                                System.out.println("Error|Result: "+receivedMessage);
                            }
                        }
                        getRountingTable();
                        getFilesList();
                        getPreviousQueries();
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

/*
    public void listen(){
        try {
            while (true) {
                System.out.println("Waiting for Incomming...");
                byte[] buffer = new byte[65536];
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                listenerSocket.receive(receivePacket);
                byte[] data = receivePacket.getData();
                String receivedMessage = new String(data, 0, receivePacket.getLength());
                System.out.println("listen|port: "+nodeSelf.port+"|receivedMessage : "+receivedMessage);

                StringTokenizer st = new StringTokenizer(receivedMessage, " ");
                String length = st.nextToken();
                String command = st.nextToken().trim();
                System.out.println("command : "+command);
                if (command.equals("REGOK")) {
                    //0051 REGOK 2 129.82.123.45 5001 64.12.123.190 34001
                    int peerCount = Integer.parseInt(st.nextToken());
                    System.out.println("peerCount : "+peerCount);
                    if (peerCount==1){
                        String ip = st.nextToken();
                        int port = Integer.parseInt(st.nextToken().trim());
                        Node node = new Node(ip,port);
                        this.routingTable.add(node);
                        sendJoinRequest(node);
                    }
                    else if (peerCount==2){
                        String ip1 = st.nextToken();
                        int port1 = Integer.parseInt(st.nextToken().trim());
                        Node node1 = new Node(ip1,port1);
                        this.routingTable.add(node1);
                        sendJoinRequest(node1);
                        String ip2 = st.nextToken();
                        int port2 = Integer.parseInt(st.nextToken().trim());
                        Node node2 = new Node(ip2,port2);
                        this.routingTable.add(node2);
                        sendJoinRequest(node2);
                    }
                }
                else if(command.equals("JOIN") && st.hasMoreTokens()){
                    //0027 JOIN 64.12.123.190 432
                    String ip = st.nextToken();
                    int port = Integer.parseInt(st.nextToken().trim());
                    Node node = new Node(ip,port);
                    boolean success = false;
                    if (!this.routingTable.contains(node)){
                        try {
                            this.routingTable.add(node);
                            success = true;
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    sendJoinOk(node, success);
                }
                else if(command.equals("JOINOK") && st.hasMoreTokens()){
                    //0014 JOINOK 0
                    int result = Integer.parseInt(st.nextToken().trim());
                    if (result==0){
                        System.out.println("0 – successful");
                    }else if (result==9999){
                        System.out.println("9999 – error while adding new node to routing table");
                    }
                }
                else if(command.equals("LEAVEOK") && st.hasMoreTokens()){
                    //0014 LEAVEOK 0
                    int result = Integer.parseInt(st.nextToken().trim());
                    if (result==0){
                        System.out.println("Node successfully leaves the distributed system.");
                    }else if (leaveRequestCount<2){
                        TimeUnit.SECONDS.sleep(1);
                        leaveRequest();
                    }
                }
                else if(command.equals("SER") && st.hasMoreTokens()){
                    //0047 SER 129.82.62.142 5070 "Lord of the rings" 2
                    String ip = st.nextToken();
                    int port = Integer.parseInt(st.nextToken().trim());
                    Node node = new Node(ip,port);
                    StringTokenizer st1 = new StringTokenizer(receivedMessage, "\"");
                    st1.nextToken().trim();
                    String searchQuery = st1.nextToken().trim();
                    int hopCount = Integer.parseInt(st1.nextToken().trim());
                    int searchKey = getHashKey(node,searchQuery);
                    if (!previousQueries.containsKey(searchKey)){
                        ArrayList<String> findings = findFileInList(searchQuery,this.filesList);
                        if (findings.isEmpty()){
                            //Forward search query.
                            forwardSearchQuery(node,searchQuery,hopCount);
                        }else {
                            //send search ok
                            sendSearchOk(findings,hopCount,node);
                        }
                        previousQueries.put(searchKey,searchQuery);
                    }else {
                        System.out.println("Previously searched query.");
                    }
                }
                else if(command.equals("SEROK") && st.hasMoreTokens()){
                    //0114 SEROK 3 129.82.128.1 2301 baby_go_home.mp3 baby_come_back.mp3 baby.mpeg
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
*/

    public void searchFileQuery(String searchQuery){
        ArrayList<String> findings = findFileInList(searchQuery,this.filesList);
        if (findings.isEmpty()){
            //Forward search query.
            forwardSearchQuery(this.nodeSelf,searchQuery,0);
        }else {
            System.out.println("Files : "+findings.toString());
        }
    }

    private int getHashKey(Node node, String searchQuery){
        String fullStr = node.toString()+"|"+searchQuery;
        return fullStr.hashCode();
    }

    private void sendJoinRequest(Node node){
        String joinRequestMessageTmp = " JOIN " + nodeSelf.ip + " " + nodeSelf.port;
        String joinRequestMessage = String.format("%04d", joinRequestMessageTmp.length() + 4)+joinRequestMessageTmp;
        System.out.println("joinRequestMessage: "+joinRequestMessage);
        try {
            InetAddress ip = InetAddress.getByName(node.ip);
            DatagramPacket sendPacket = new DatagramPacket(joinRequestMessage.getBytes(), joinRequestMessage.length(),ip,node.port);
            listenerSocket.send(sendPacket);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void forwardSearchQuery(Node node,String searchQuery,int hopCount){
        //0047 SER 129.82.62.142 5070 "Lord of the rings" 2
        String newQueryMessageTmp = " SER "+node.ip+" "+node.port+" \""+searchQuery+"\" "+String.format("%02d", hopCount+1);
        String newQueryMessage = String.format("%04d", newQueryMessageTmp.length() + 4)+ newQueryMessageTmp;
        for (int i = 0; i < this.routingTable.size(); i++) {
            Node neighbour = this.routingTable.get(i);
            try {
                InetAddress ip = InetAddress.getByName(neighbour.ip);
                DatagramPacket sendPacket = new DatagramPacket(newQueryMessage.getBytes(), newQueryMessage.length(),ip,neighbour.port);
                listenerSocket.send(sendPacket);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void leaveRequest(){
        //0028 LEAVE 64.12.123.190 432
        String leaveRequestMessageTmp = " LEAVE " + nodeSelf.ip + " " + nodeSelf.port;
        String leaveRequestMessage = String.format("%04d", leaveRequestMessageTmp.length() + 4)+ leaveRequestMessageTmp;
        System.out.println("leaveRequestMessage: "+leaveRequestMessage);
        try {
            InetAddress bootIp = InetAddress.getByName(InitConfig.bootstrap_ip);
            DatagramPacket sendPacket = new DatagramPacket(leaveRequestMessage.getBytes(), leaveRequestMessage.length(),bootIp,InitConfig.bootstrap_port);
            listenerSocket.send(sendPacket);
            leaveRequestCount++;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendSearchOk(ArrayList<String> findings, int hopCount, Node node){
        //0114 SEROK 3 129.82.128.1 2301 baby_go_home.mp3 baby_come_back.mp3 baby.mpeg
        String findingsStr = String.join(" ", findings);
        //length SEROK no_files IP port hops filename1 filename2
        String searchOkMessageTmp = " SEROK "+findings.size()+" "+this.nodeSelf.ip+" "+this.nodeSelf.port
                +" "+hopCount+" "+findingsStr;
        String searchOkMessage = String.format("%04d", searchOkMessageTmp.length() + 4)+ searchOkMessageTmp;
        System.out.println("searchOkMessage: "+searchOkMessage);
        try {
            InetAddress ip = InetAddress.getByName(node.ip);
            DatagramPacket sendPacket = new DatagramPacket(searchOkMessage.getBytes(), searchOkMessage.length(),ip,node.port);
            listenerSocket.send(sendPacket);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendJoinOk(Node node, boolean success){
        //0014 JOINOK 0
        String joinOkMessageTmp = "";
        if (success==true){
            joinOkMessageTmp = " JOINOK 0";
        }else {
            joinOkMessageTmp = " JOINOK 9999";
        }
        String joinOkMessage = String.format("%04d", joinOkMessageTmp.length() + 4)+ joinOkMessageTmp;
        System.out.println("joinOkMessageTmp: "+joinOkMessageTmp);
        try {
            InetAddress ip = InetAddress.getByName(node.ip);
            DatagramPacket sendPacket = new DatagramPacket(joinOkMessageTmp.getBytes(), joinOkMessageTmp.length(),ip,node.port);
            listenerSocket.send(sendPacket);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendRegisterRequest(){
        String register_message_tmp = " REG " + nodeSelf.ip + " " + nodeSelf.port + " " + nodeSelf.getUserName();
        String register_message = String.format("%04d", register_message_tmp.length() + 4)+ register_message_tmp;
        System.out.println("register_message: "+register_message);
        try {
            InetAddress bootIp = InetAddress.getByName(InitConfig.bootstrap_ip);
            DatagramPacket sendPacket = new DatagramPacket(register_message.getBytes(), register_message.length(),bootIp,InitConfig.bootstrap_port);
            listenerSocket.send(sendPacket);
        } catch (UnknownHostException e) {
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

    public void getFilesList() {
        System.out.println("----------------------------------------------------------------------------");
        System.out.println(String.join(",", filesList));
        System.out.println("----------------------------------------------------------------------------");
        System.out.println("");
    }

    public void getRountingTable() {
        System.out.println("----------------------------------------------------------------------------");
        System.out.println(routingTable.toString());
        System.out.println("----------------------------------------------------------------------------");
        System.out.println("");
    }

    public void getPreviousQueries() {
        System.out.println("----------------------------------------------------------------------------");
        System.out.println(previousQueries.toString());
        previousQueries.values().stream().map(Object::toString).collect(Collectors.joining(","));
        System.out.println("----------------------------------------------------------------------------");
        System.out.println("");
    }

}
