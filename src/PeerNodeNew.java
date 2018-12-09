import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.text.DecimalFormat;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.geometry.Pos;
import model.Comment;
import model.Forum;
import model.Post;
import model.Rank;

public class PeerNodeNew{
    ArrayList<Node> routingTable;
    String[] filesList;
    Node nodeSelf;
    DatagramSocket listenerSocket = null;
    HashMap<Integer, String> previousQueries = new HashMap<Integer, String>();
    HashMap<Integer, String> previousRankings = new HashMap<Integer, String>();
    //HashMap<String, ArrayList<HashMap<Node, Integer>>> fileRanks = new HashMap<String, ArrayList<HashMap<Node, Integer>>>();
    HashMap<String, HashMap<Node, Integer>> fileRanks = new HashMap<String, HashMap<Node, Integer>>();
    private int leaveRequestCount = 0;
    private static DecimalFormat df2 = new DecimalFormat(".##");
    private Forum forum = new Forum(); // is a JSON Array
    private int timestamp = 0;


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
                        System.out.println("command : "+ command);
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
                        else if(command.equals("FILE_RANK") && st.hasMoreTokens()){
                            StringTokenizer tokens = new StringTokenizer(receivedMessage, "|");
                            tokens.nextToken();
                            String fileName = tokens.nextToken().trim();
                            int rank = Integer.parseInt(tokens.nextToken().trim());
                            String ip = tokens.nextToken().trim();
                            int port = Integer.parseInt(tokens.nextToken().trim());
                            int rankKey = getRankHashKey(new Node(ip,port),fileName,rank);
                            if (!previousRankings.containsKey(rankKey)){
                                updateRanks(fileName,rank, new Node(ip,port));
                                previousRankings.put(rankKey,fileName);
                            }else {
                                System.out.println("Ignoring|Duplicate ranking.");
                            }
                        }
                        else if(command.contains("FORUM_POST")){
                            //<<length>> FORUM_POST <<timestamp>> <<post_message>>
                            //now need to check for the timestamp in the
                            StringTokenizer tokens = new StringTokenizer(receivedMessage, "|");
                            tokens.nextToken();
                            timestamp = Integer.max(Integer.parseInt(tokens.nextToken().trim()), timestamp);
                            String postMsg = tokens.nextToken();
                            System.out.println(postMsg);
                            System.out.println(receivedMessage);
                            ObjectMapper mapper = new ObjectMapper();
                            Post post = mapper.readValue(postMsg, Post.class);
                            // now add this post to the forum.
                            post.setTimestamp(timestamp);
                            if (forum.postExist(post)) {
                                forum.updatePost(post);
                            } else {
                                forum.addPost(post);
                            }
                            System.out.println(forum);


                        }
                        else if(command.equals("FORUM_COMMENT") && st.hasMoreTokens()){
                            //<<length>> FORUM_COMMENT|<<post_id>>|<<comment_message>>|<<timestamp>>|<<node_id>>
                            StringTokenizer commentMsg = new StringTokenizer(receivedMessage, "|");
                            commentMsg.nextToken();
                            int postId = Integer.parseInt(commentMsg.nextToken());
                            String comment = commentMsg.nextToken();
                            timestamp = Integer.max(timestamp, Integer.parseInt(commentMsg.nextToken().trim()));
                            String nodeId = commentMsg.nextToken().trim();
                            Comment commentObj = new Comment();
                            commentObj.setContent(comment);
                            commentObj.setTimestamp(timestamp);
                            commentObj.setNodeId(nodeId);
                            commentObj.setCommentId(forum.getPostBytId(postId).getComments().size());
                            forum.getPostBytId(postId).getComments().add(commentObj);
                            //find the the post.
//                            forum.

                        }
                        else if(command.equals("POST_RANK") && st.hasMoreTokens()){
                            //<<length>> POST_RANK|<<post_id>>|<<rank>>|<<timestamp>>|<<node_id>>
                            StringTokenizer tokenizer = new StringTokenizer(receivedMessage, "|");
                            tokenizer.nextToken();
                            int postId = Integer.parseInt(tokenizer.nextToken());
                            int rankValue = Integer.parseInt(tokenizer.nextToken());
                            timestamp = Integer.max(timestamp, Integer.parseInt(tokenizer.nextToken().trim()));
                            String nodeId = tokenizer.nextToken().trim();

                            Rank rank = new Rank();
                            rank.setNodeId(nodeId);
                            rank.setRankValue(rankValue);

                            forum.getPostBytId(postId).addRank(rank);
                        }
                        else {
                            System.out.println("jdhasjkfahgjkah afkjaskjklj adjaskljafklj afklvas");
                        }
                        getRountingTable();
                        getFilesList();
                        getPreviousQueries();
                        getFileRanks();
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

    public void addForumPost(String post){
        //<<length>> FORUM_POST|<<post_id>>|<<post_message>>|<<timestamp>>|<<node_id>>
        // post content should be validated.
        timestamp++;
        Post postObj = new Post();

        postObj.setPostId(forum.getPostList().size());
        postObj.setNodeId(nodeSelf.getUserName());
        postObj.setTimestamp(timestamp);
        postObj.setContent(post);
        postObj.setRanks(new ArrayList<>());
        postObj.setComments(new ArrayList<>());

        // an empty Json is created. Now let's add this to the forum
        forum.addPost(postObj); // order has to be preserved.
        DatagramSocket sendSocket = null;
        try {
            sendSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
//        forumMessage has to be sent with the header which has the timestamp.
        StringJoiner joiner = new StringJoiner("|");
        String messageType = "FORUM_POST";
        joiner.add(messageType);
        joiner.add(String.valueOf(timestamp));
        joiner.add(postObj.toString());
        String length = String.format("%04d ", joiner.toString().length() + 5);
        String message = length + joiner.toString();
        broadCast(sendSocket, message, this.routingTable);
        // we should update the rankings and comments accordingly.


    }

    public void rankForumPost(int postId, int rank){
        //<<length>> POST_RANK|<<post_id>>|<<rank>>|<<timestamp>>|<<node_id>>
        Post post = forum.getPostBytId(postId);
        post.getRanks().add(JsonUtils.getRank(rank, nodeSelf.userName));

        forum.updatePost(post);
    }

    public synchronized void addForumComment(int postId, String comment){
        //<<length>> FORUM_COMMENT|<<post_id>>|<<comment_message>>|<<timestamp>>|<<node_id>>

        Post post = forum.getPostBytId(postId);

        Comment commentJson = new Comment();
        timestamp ++;
        commentJson.setNodeId(nodeSelf.getUserName());
        commentJson.setTimestamp(timestamp);
        commentJson.setCommentId(post.getComments().size());
        commentJson.setContent(comment);
        post.getComments().add(commentJson);

        forum.updatePost(post); // added the newer post with updated comment. Same for ranking.

        // broadcast the post to

    }

    public void searchFileQuery(String searchQuery){
        ArrayList<String> findings = findFileInList(searchQuery,this.filesList);
        if (findings.isEmpty()){
            //Forward search query.
            forwardSearchQuery(this.nodeSelf,searchQuery,0);
        }else {
            System.out.println("Files : "+findings.toString());
        }
    }

    public void getFileRank(String fileName){
        if (fileRanks.containsKey(fileName)) {
            int rankTotal = 0;
            HashMap<Node, Integer> rankMap = fileRanks.get(fileName);
            int count = rankMap.size();
            for (Map.Entry<Node, Integer> entry : rankMap.entrySet()) {
                System.out.println("Item : " + entry.getKey() + " Count : " + entry.getValue());
                rankTotal = rankTotal+entry.getValue();
            }
            double averageRank = (double)rankTotal/count;
            System.out.println(fileName+" averrage rank = "+df2.format(averageRank));
        }
        else{
            System.out.println("No rank info.");
        }
    }

    public void rankFile(String fileName, int rank){
        updateRanks(fileName,rank, nodeSelf);
    }

    private void updateRanks(String fileName, int rank, Node node){
        //<<length>>|FILE_RANK|<<file_id>>|<<rank>>|<<timestamp>>|<<node_id>>
        //HashMap<String,HashMap<String, Integer>>
        System.out.println("updateRanks: "+fileName+" rank:"+rank+" node:"+node.toString());
        HashMap<Node, Integer> rankMap;
        this.getFileRanks();
        if (fileRanks.containsKey(fileName)){
            System.out.println("Existing.");
            rankMap = fileRanks.get(fileName);
            if (rankMap.containsKey(node)){
                System.out.println("----Existing.");
                rankMap.replace(node,rank);
            }else {
                System.out.println("----New file.");
                rankMap.put(node,rank);
            }
            fileRanks.replace(fileName,rankMap);
        }else {
            System.out.println("New file.");
            rankMap = new HashMap<Node, Integer>();
            rankMap.put(node,rank);
            fileRanks.put(fileName,rankMap);
        }
        String rankFileMessageTmp = " FILE_RANK |"+fileName+"|"+rank+"|"+node.ip+"|"+node.port;
        //0034 FILE_RANK |hello world.mp4|2|132.43.12.43|45231
        String rankFileMessage = String.format("%04d", rankFileMessageTmp.length() + 4)+rankFileMessageTmp;
        System.out.println("rankFileMessage: "+rankFileMessage);
        int rankKey = getRankHashKey(node,fileName,rank);
        previousRankings.put(rankKey,fileName);
        forwardRankMessage(rankFileMessage);
    }

    private void forwardRankMessage(String rankFileMessage){
        for (int i = 0; i < this.routingTable.size(); i++) {
            Node neighbour = this.routingTable.get(i);
            try {
                InetAddress ip = InetAddress.getByName(neighbour.ip);
                DatagramPacket sendPacket = new DatagramPacket(rankFileMessage.getBytes(), rankFileMessage.length(),ip,neighbour.port);
                listenerSocket.send(sendPacket);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int getNodeHashKey(Node node){
        return node.toString().hashCode();
    }

    private int getHashKey(Node node, String searchQuery){
        String fullStr = node.toString()+"|"+searchQuery;
        return fullStr.hashCode();
    }

    private int getRankHashKey(Node node, String fileName,int rank){
        String fullStr = node.toString()+"|"+fileName+"|"+rank;
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
    }

    public void getFileRanks() {
        System.out.println("----------------------------------------------------------------------------");
        System.out.println(fileRanks.toString());
        fileRanks.values().stream().map(Object::toString).collect(Collectors.joining(","));
        System.out.println("----------------------------------------------------------------------------");
    }

    public void printRankMap(HashMap<Node, Integer> rankMap) {
        System.out.println("----------------------------------------------------------------------------");
        System.out.println(rankMap.toString());
        rankMap.values().stream().map(Object::toString).collect(Collectors.joining(","));
        System.out.println("----------------------------------------------------------------------------");
    }

    // getters and setters.

    public Forum getForum() {
        return forum;
    }

    public void setForum(Forum forum) {
        this.forum = forum;
    }
}

