package com.dc.peer;

import com.dc.peer.model.Comment;
import com.dc.peer.model.Forum;
import com.dc.peer.model.Post;
import com.dc.peer.model.Rank;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.io.IOException;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Peer {
    ArrayList<Node> routingTable;
    String[] filesList;
    Node node;
    DatagramSocket listenerSocket = null;
    HashMap<Integer, String> previousQueries = new HashMap();
    HashMap<Integer, String> previousRankings = new HashMap();
    HashMap<Integer, String> previousPosts = new HashMap();
    HashMap<Integer, String> previousComments = new HashMap();
    HashMap<Integer, String> previousPostRankings = new HashMap();
    HashMap<String, HashMap<Node, Integer>> fileRanks = new HashMap();
    private int leaveRequestCount = 0;
    private static DecimalFormat df2 = new DecimalFormat(".##");
    private Forum forum = new Forum(); // is a JSON Array
    private int timestamp = 0;
    private DatagramSocket sendSocket;


    public Peer(String my_ip, int my_port, String my_username) {
        node = new Node(my_ip, my_port);
        node.setUserName(my_username);
        createSendSocket();
        createReceiveSocket(my_ip, my_port);
        filesList = InitConfig.getRandomFiles();
        routingTable = new ArrayList();
        getFilesList();
        sendRegisterRequest();
        listen();
    }


    public void listen() {
        (new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        System.out.println("Waiting for Incoming...");
                        byte[] buffer = new byte[65536];
                        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                        listenerSocket.receive(receivePacket);
                        byte[] data = receivePacket.getData();
                        String receivedMessage = new String(data, 0, receivePacket.getLength());
                        System.out.println("listen|port: "+ node.getPort()+"|receivedMessage : "+receivedMessage);

                        StringTokenizer st = new StringTokenizer(receivedMessage, " ");
                        String length = st.nextToken();
                        String command = st.nextToken().trim();
                        System.out.println("command : "+ command);
                        if (command.equals("REGOK")) {
                            //0051 REGOK 2 129.82.123.45 5001 64.12.123.190 34001
                            int peerCount = Integer.parseInt(st.nextToken());
                            System.out.println("peerCount : "+peerCount);
                            for (int i = 0; i < peerCount; i++) {
                                String ip = st.nextToken();
                                int port = Integer.parseInt(st.nextToken().trim());
                                Node node = new Node(ip, port);
                                routingTable.add(node);
                                sendJoinRequest(node);

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
                            //<<length>>|FILE_RANK|<<file_id>>|<<rank>>|<<timestamp>>|<<creator node>>|<<sender node>>
                            StringTokenizer tokens = new StringTokenizer(receivedMessage, "|");
                            tokens.nextToken();
                            String fileName = tokens.nextToken().trim();
                            int rank = Integer.parseInt(tokens.nextToken().trim());
                            String ip = tokens.nextToken().trim();
                            int port = Integer.parseInt(tokens.nextToken().trim());
                            Node creator = new Node(ip,port);
                            String ip1 = tokens.nextToken().trim();
                            int port1 = Integer.parseInt(tokens.nextToken().trim());
                            Node sender = new Node(ip1,port1);
                            int rankKey = getRankHashKey(creator,fileName,rank);
                            if (!previousRankings.containsKey(rankKey)){
                                updateRanks(fileName,rank, creator, sender);
                                previousRankings.put(rankKey,fileName);
                            }else {
                                System.out.println("Ignoring|Duplicate ranking.");
                            }
                        }
                        else if(command.contains("FORUM_POST")){
                            //0132 FORUM_POST |1|{"post_id":0,"timestamp":1,"node_id":"node35685","content":"hello world","ranks":[],"comments":[],"avg_rank":0.0}|creator|sender
                            //now need to check for the timestamp in the
                            StringTokenizer tokens = new StringTokenizer(receivedMessage, "|");
                            tokens.nextToken();
                            timestamp = Integer.max(Integer.parseInt(tokens.nextToken().trim()), timestamp);
                            String postMsg = tokens.nextToken();
                            String ip = tokens.nextToken().trim();
                            int port = Integer.parseInt(tokens.nextToken().trim());
                            Node creator = new Node(ip,port);
                            System.out.println(postMsg);
                            System.out.println(receivedMessage);
                            ObjectMapper mapper = new ObjectMapper();
                            Post post = mapper.readValue(postMsg, Post.class);
                            //post+creator
                            int hashKey = getPostHashKey(post,creator);
                            if (!previousPosts.containsKey(hashKey)){
                                updateForumPost(post,creator);
                            }else {
                                System.out.println("Ignoring|Duplicate forum post.");
                            }
                            System.out.println(forum);
                        }
                        else if(command.equals("FORUM_COMMENT") && st.hasMoreTokens()){
                            //<<length>> FORUM_COMMENT|<<post_id>>|<<comment_message>>|<<timestamp>>|<<node_id>>
                            //0038 FORUM_COMMENT |0|nice|2|node35685|sender node
                            //0053 FORUM_COMMENT |0|like|3|node49061|127.0.0.1|6889
                            StringTokenizer commentMsg = new StringTokenizer(receivedMessage, "|");
                            commentMsg.nextToken();
                            int postId = Integer.parseInt(commentMsg.nextToken());
                            String comment = commentMsg.nextToken();
                            timestamp = Integer.max(timestamp, Integer.parseInt(commentMsg.nextToken().trim()));
                            String nodeId = commentMsg.nextToken().trim();
                            int hashKey = getCommentHashKey(postId,comment,nodeId);
                            if (!previousComments.containsKey(hashKey)){
                                Comment commentObj = new Comment();
                                commentObj.setContent(comment);
                                commentObj.setTimestamp(timestamp);
                                commentObj.setNodeId(nodeId);
                                commentObj.setCommentId(forum.getPostBytId(postId).getComments().size());
                                forum.getPostBytId(postId).getComments().add(commentObj);
                                StringJoiner joiner = new StringJoiner("|");
                                String messageType = "FORUM_COMMENT ";
                                joiner.add(messageType);
                                joiner.add(String.valueOf(postId));
                                joiner.add(comment);
                                joiner.add(String.valueOf(timestamp));
                                joiner.add(commentObj.getNodeId());
                                joiner.add(node.getIp());
                                joiner.add(String.valueOf(node.getPort()));
                                length = String.format("%04d ", joiner.toString().length() + 5);
                                String message = length + joiner.toString();
                                broadcastMessage(message);
                                previousComments.put(hashKey,comment);
                            }else {
                                System.out.println("Ignoring|Duplicate forum post comment.");
                            }
                            //post id+Comment+creator id
                        }
                        else if(command.equals("POST_RANK") && st.hasMoreTokens()){
                            //<<length>> POST_RANK|<<post_id>>|<<rank>>|<<timestamp>>|<<node_id>>
                            //0031 POST_RANK |0|4|2|node35685|sender node
                            StringTokenizer tokenizer = new StringTokenizer(receivedMessage, "|");
                            tokenizer.nextToken();
                            int postId = Integer.parseInt(tokenizer.nextToken());
                            int rankValue = Integer.parseInt(tokenizer.nextToken());
                            timestamp = Integer.max(timestamp, Integer.parseInt(tokenizer.nextToken().trim()));
                            String nodeId = tokenizer.nextToken().trim();

                            int hashKey = getPostRankHashKey(postId,rankValue,nodeId);
                            if (!previousPostRankings.containsKey(hashKey)){
                                Rank rank = new Rank();
                                rank.setNodeId(nodeId);
                                rank.setRankValue(rankValue);

                                forum.getPostBytId(postId).addRank(rank);

                                StringJoiner joiner = new StringJoiner("|");
                                String messageType = "POST_RANK ";
                                joiner.add(messageType);
                                joiner.add(String.valueOf(postId));
                                joiner.add(String.valueOf(rankValue));
                                joiner.add(String.valueOf(timestamp));
                                joiner.add(nodeId);
                                joiner.add(node.getIp());
                                joiner.add(String.valueOf(node.getPort()));
                                length = String.format("%04d ", joiner.toString().length() + 5);
                                String message = length + joiner.toString();
                                previousPostRankings.put(hashKey,rank.toString());
                                broadcastMessage(message);
                            }else {
                                System.out.println("Ignoring|Duplicate forum post comment.");
                            }
                            //post id+rank+node id
                        }
                        else System.out.println("Invalid message format|receivedMessage: " + receivedMessage);
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

    private void broadcastMessage(String message){
        for (int i = 0; i < this.routingTable.size(); i++) {
            Node neighbour = this.routingTable.get(i);
            try {
                InetAddress ip = InetAddress.getByName(neighbour.getIp());
                DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.length(),ip,neighbour.getPort());
                listenerSocket.send(sendPacket);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void addForumPost(String post){
        Post postObj = new Post();
        postObj.setPostId(forum.getPostList().size());
        postObj.setNodeId(this.node.getUserName());
        postObj.setTimestamp(timestamp);
        postObj.setContent(post);
        postObj.setRanks(new ArrayList<>());
        postObj.setComments(new ArrayList<>());
        updateForumPost(postObj, this.node);
    }

    public void updateForumPost(Post post,Node creator){
        //<<length>> FORUM_POST|<<post_id>>|<<post_message>>|<<timestamp>>|<<node_id>>
        // post content should be validated.
        timestamp++;
        // an empty Json is created. Now let's add this to the forum
        //forum.addPost(postObj); // order has to be preserved.
        if (forum.postExist(post)) {
            forum.updatePost(post);
        } else {
            forum.addPost(post);
        }
        //forumMessage has to be sent with the header which has the timestamp.
        StringJoiner joiner = new StringJoiner("|");
        String messageType = "FORUM_POST "; //keep space after message type(command) otherwise tokenizing won't work as expected.
        joiner.add(messageType);
        joiner.add(String.valueOf(timestamp));
        joiner.add(post.toString());
        joiner.add(creator.getIp());
        joiner.add(String.valueOf(creator.getPort()));
        joiner.add(this.node.getIp());
        joiner.add(String.valueOf(this.node.getPort()));
        String length = String.format("%04d ", joiner.toString().length() + 5);
        String message = length + joiner.toString();
        broadcastMessage(message);
        int hashKey = getPostHashKey(post,creator);
        previousPosts.put(hashKey,post.toString());
        // we should update the rankings and comments accordingly.
    }

    public void rankForumPost(int postId, int rank){
        //<<length>> POST_RANK|<<post_id>>|<<rank>>|<<timestamp>>|<<node_id>>
        Post post = forum.getPostBytId(postId);
        post.getRanks().add(JsonUtils.getRank(rank, this.node.getUserName()));

        forum.updatePost(post);
        StringJoiner joiner = new StringJoiner("|");
        String messageType = "POST_RANK ";
        joiner.add(messageType);
        joiner.add(String.valueOf(postId));
        joiner.add(String.valueOf(rank));
        joiner.add(String.valueOf(timestamp));
        joiner.add(this.node.getUserName());
        joiner.add(this.node.getIp());
        joiner.add(String.valueOf(this.node.getPort()));
        String length = String.format("%04d ", joiner.toString().length() + 5);
        String message = length + joiner.toString();
        int hashKey = getPostRankHashKey(postId,rank,this.node.getUserName());
        Rank rankObj = new Rank();
        rankObj.setNodeId(this.node.getUserName());
        rankObj.setRankValue(rank);
        previousPostRankings.put(hashKey,rankObj.toString());
        System.out.println("rankForumPost|message: "+message);
        broadcastMessage(message);
    }

    public void addForumComment(int postId, String comment){
        //<<length>> FORUM_COMMENT|<<post_id>>|<<comment_message>>|<<timestamp>>|<<node_id>>
        //0038 FORUM_COMMENT |0|nice|2|node35685
        Post post = forum.getPostBytId(postId);

        Comment commentJson = new Comment();
        timestamp ++;
        commentJson.setNodeId(this.node.getUserName());
        commentJson.setTimestamp(timestamp);
        commentJson.setCommentId(post.getComments().size());
        commentJson.setContent(comment);
        post.getComments().add(commentJson);

        forum.updatePost(post); // added the newer post with updated comment. Same for ranking.

        // broadcast the post to
        StringJoiner joiner = new StringJoiner("|");
        String messageType = "FORUM_COMMENT ";
        joiner.add(messageType);
        joiner.add(String.valueOf(postId));
        joiner.add(comment);
        joiner.add(String.valueOf(timestamp));
        joiner.add(this.node.getUserName());
        joiner.add(this.node.getIp());
        joiner.add(String.valueOf(this.node.getPort()));
        String length = String.format("%04d ", joiner.toString().length() + 5);
        String message = length + joiner.toString();
        int hashKey = getCommentHashKey(postId,comment,this.node.getUserName());
        previousComments.put(hashKey,comment);
        broadcastMessage(message);
    }

    public void searchFileQuery(String searchQuery){
        ArrayList<String> findings = findFileInList(searchQuery,this.filesList);
        if (findings.isEmpty()){
            //Forward search query.
            forwardSearchQuery(this.node,searchQuery,0);
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
        updateRanks(fileName,rank, this.node, this.node);
    }

    private void updateRanks(String fileName, int rank, Node creator, Node sender){
        //<<length>>|FILE_RANK|<<file_id>>|<<rank>>|<<timestamp>>|<<node_id>>
        //HashMap<String,HashMap<String, Integer>>
        System.out.println("updateRanks: "+fileName+" rank:"+rank+" node:"+creator.toString());
        HashMap<Node, Integer> rankMap;
        this.getFileRanks();
        if (fileRanks.containsKey(fileName)){
            System.out.println("Existing.");
            rankMap = fileRanks.get(fileName);
            if (rankMap.containsKey(creator)){
                System.out.println("----Existing.");
                rankMap.replace(creator,rank);
            }else {
                System.out.println("----New file.");
                rankMap.put(creator,rank);
            }
            fileRanks.replace(fileName,rankMap);
        }else {
            System.out.println("New file.");
            rankMap = new HashMap<Node, Integer>();
            rankMap.put(creator,rank);
            fileRanks.put(fileName,rankMap);
        }
        String rankFileMessageTmp = " FILE_RANK |"+fileName+"|"+rank+"|"+creator.getIp()+"|"+creator.getPort()+"|"+ this.node.getIp()+"|"+ this.node.getPort();
        //0034 FILE_RANK |hello world.mp4|2|132.43.12.43|45231
        String rankFileMessage = String.format("%04d", rankFileMessageTmp.length() + 4)+rankFileMessageTmp;
        System.out.println("rankFileMessage: "+rankFileMessage);
        int rankKey = getRankHashKey(creator,fileName,rank);
        previousRankings.put(rankKey,fileName);
        broadcastMessage(rankFileMessage);
    }

    private int getHashKey(Node node, String searchQuery){
        String fullStr = node.toString()+"|"+searchQuery;
        return fullStr.hashCode();
    }

    private int getRankHashKey(Node node, String fileName,int rank){
        String fullStr = node.toString()+"|"+fileName+"|"+rank;
        return fullStr.hashCode();
    }

    ////post+creator
    private int getPostHashKey(Post post, Node creator){
        String fullStr = post.toString()+"|"+creator.toString();
        return fullStr.hashCode();
    }

    //post id+Comment+creator id
    private int getCommentHashKey(int postId, String comment,String nodeId){
        String fullStr = postId+"|"+comment+"|"+nodeId;
        return fullStr.hashCode();
    }

    //post id+rank+node id
    private int getPostRankHashKey(int postId, int rank,String nodeId){
        String fullStr = postId+"|"+rank+"|"+nodeId;
        return fullStr.hashCode();
    }

    private void sendJoinRequest(Node node){
        String joinRequestMessageTmp = " JOIN " + this.node.getIp() + " " + this.node.getPort();
        String joinRequestMessage = String.format("%04d", joinRequestMessageTmp.length() + 4)+joinRequestMessageTmp;
        System.out.println("joinRequestMessage: "+joinRequestMessage);
        try {
            InetAddress ip = InetAddress.getByName(node.getIp());
            DatagramPacket sendPacket = new DatagramPacket(joinRequestMessage.getBytes(), joinRequestMessage.length(),ip,node.getPort());
            listenerSocket.send(sendPacket);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void forwardSearchQuery(Node node,String searchQuery,int hopCount){
        //0047 SER 129.82.62.142 5070 "Lord of the rings" 2
        String newQueryMessageTmp = " SER "+node.getIp()+" "+node.getPort()+" \""+searchQuery+"\" "+String.format("%02d", hopCount+1);
        String newQueryMessage = String.format("%04d", newQueryMessageTmp.length() + 4)+ newQueryMessageTmp;
        broadcastMessage(newQueryMessage);
    }

    private void leaveRequest(){
        //0028 LEAVE 64.12.123.190 432
        String leaveRequestMessageTmp = " LEAVE " + this.node.getIp() + " " + this.node.getPort();
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
        String searchOkMessageTmp = " SEROK "+findings.size()+" "+this.node.getIp()+" "+this.node.getPort()
                +" "+hopCount+" "+findingsStr;
        String searchOkMessage = String.format("%04d", searchOkMessageTmp.length() + 4)+ searchOkMessageTmp;
        System.out.println("searchOkMessage: "+searchOkMessage);
        try {
            InetAddress ip = InetAddress.getByName(node.getIp());
            DatagramPacket sendPacket = new DatagramPacket(searchOkMessage.getBytes(), searchOkMessage.length(),ip,node.getPort());
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
            InetAddress ip = InetAddress.getByName(node.getIp());
            DatagramPacket sendPacket = new DatagramPacket(joinOkMessage.getBytes(), joinOkMessage.length(),ip,node.getPort());
            listenerSocket.send(sendPacket);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendRegisterRequest(){
        String register_message_tmp = " REG " + this.node.getIp() + " " + this.node.getPort() + " " + this.node.getUserName();
        String register_message = String.format("%04d", register_message_tmp.length() + 4)+ register_message_tmp;
        System.out.println("register_message: "+register_message);
        try {
            InetAddress bootIp = InetAddress.getByName(InitConfig.bootstrap_ip);
            DatagramPacket sendPacket = new DatagramPacket(register_message.getBytes(), register_message.length(),bootIp,InitConfig.bootstrap_port);
            System.out.println("Bootstrap IP " + bootIp);
            listenerSocket.send(sendPacket);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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

    private DatagramSocket createSendSocket() {
        try {
            sendSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return sendSocket;
    }

    private DatagramSocket createReceiveSocket(String ip, int port) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            listenerSocket = new DatagramSocket(port, inetAddress);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return sendSocket;
    }
}

