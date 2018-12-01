import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class StartNode {
    public static void main(String[] args) {
        int port = ThreadLocalRandom.current().nextInt(1000, 50000);
        PeerNodeNew node = new PeerNodeNew("127.0.0.1",port,"node" + port);

        Scanner in = new Scanner(System.in);
        while(true) {
            System.out.println("What you want to do?");
            System.out.println("1. Search for a file");
            System.out.println("2. Print File Names for a node.");
            System.out.println("3. Print Routing table for a node.");
            System.out.println("4. Previous queries.");
            System.out.println("5. Comment on a File.");
            System.out.println("6. Rank a file.");
            System.out.println("7. Summary of files with ranks.");

            if (in.nextLine().equals("1")) {
                System.out.println("Enter Search Query :");
                String query = in.nextLine();
                node.searchFileQuery(query);
            }
            if (in.nextLine().equals("2")) {
                System.out.println("Print File Names for a node.");
                node.getFilesList();
            }
            if (in.nextLine().equals("3")) {
                System.out.println("Print Routing table for a node.");
                node.getRountingTable();
            }
            if (in.nextLine().equals("4")) {
                System.out.println("Previous queries.");
                node.getPreviousQueries();
            }
        }
    }
}
