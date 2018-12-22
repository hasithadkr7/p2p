package com.dc.peer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class StartApplication {
    public static void main(String[] args) {
        // get the boot node to the session
        InitConfig.setBootstrap_ip(args[0]);
        int port = 22222;
        String address = null;
        try {
            address = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        Peer node = new Peer(address, port,"Node :" + address);
        Scanner in = new Scanner(System.in);
        while(true) {
            System.out.println("What you want to do?");
            System.out.println("1. Search for a file");
            System.out.println("2. Print File Names for a node.");
            System.out.println("3. Print Routing table for a node.");
            System.out.println("4. Previous queries.");
            System.out.println("5. Rank a file.");
            System.out.println("6. Get file rank.");
            System.out.println("7. Add forum post.");
            System.out.println("8. Comment on forum post.");
            System.out.println("9. Rank a Forum Post");
            System.out.println("10. View the Forum.");
	    System.out.println("11. Leave the network.");

            int selection = Integer.parseInt(in.nextLine().trim());
            switch (selection){
                case 1:
                    System.out.println("Enter Search Query :");
                    String query = in.nextLine();
                    node.searchFileQuery(query);
                    break;
                case 2:
                    System.out.println("Print File Names for a node.");
                    node.getFilesList();
                    break;
                case 3:
                    System.out.println("Print Routing table for a node.");
                    node.getRountingTable();
                    break;
                case 4:
                    System.out.println("Previous queries.");
                    node.getPreviousQueries();
                    break;
                case 5:
                    System.out.println("Enter file name: ");
                    String fileName = in.nextLine();
                    System.out.println("fileName:"+fileName);
                    System.out.println("Enter rank: ");
                    int rank = Integer.parseInt(in.nextLine().trim());
                    System.out.println("Rank:"+rank);
                    node.rankFile(fileName,rank);
                    break;
                case 6:
                    System.out.println("Enter file name: ");
                    String queryName = in.nextLine();
                    System.out.println("queryName:"+queryName);
                    node.getFileRank(queryName);
                    break;
                case 7:
                    System.out.println("Enter Post description: ");
                    String post = in.nextLine();
                    System.out.println("Forum Post: " + post);
                    // forum post should be added within the node.
                    node.addForumPost(post);
                    break;
                case 8:
                    System.out.println("Comment on Post description: ");
                    int postId = Integer.parseInt(in.nextLine().trim());
                    String comment = in.nextLine();
                    // forum post should be added within the node.
                    node.addForumComment(postId, comment);
                    break;
                case 9:
                    System.out.println("Rank a Post: ");
                    postId = Integer.parseInt(in.nextLine().trim());
                    rank = Integer.parseInt(in.nextLine().trim());
                    // forum post should be added within the node.
                    node.rankForumPost(postId, rank);
                    break;
                case 10:
                    System.out.println("Current Forum :");
                    System.out.println(node.getForum().getPostList().toString());
		case 11:
                    System.out.println("Leave the network.");
//                    node.leaveRequest();
                default:
                    System.out.println("No matching input.");
            }
        }
    }
}

