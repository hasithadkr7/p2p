import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

public class Sender {
    public char[] sendMessageTcp(String ipAddress,int port, String message) {
        Socket echoSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try {
            echoSocket = new Socket(ipAddress, port);
            out = new PrintWriter(echoSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
            out.println(message);
            char[] buf=new char[100];
            in.read(buf);
            out.close();
            in.close();
            echoSocket.close();
            return buf;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendMessage(String ipAddress,int port, String message) {
        try {
            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress ip = InetAddress.getByName(ipAddress);
            DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.length(), ip, port);
            clientSocket.send(sendPacket);
            byte[] buffer = new byte[65536];
            DatagramPacket receiving_pkt = new DatagramPacket(buffer, buffer.length);
            clientSocket.receive(receiving_pkt);
            String received_msg = new String(receiving_pkt.getData(), 0, receiving_pkt.getLength());
            parseReceiveMessage(received_msg);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void parseReceiveMessage(String message) {
        System.out.println("Received Message : "+message);
        int len = message.length();
        if (message.substring(5, 10).equals("REGOK")) {
            System.out.println("REGOK : "+message.substring(11, len));
            String messagePayload = message.substring(11, len);
            String[] payLoadParts = messagePayload.split(" ");
        } else if (message.substring(5, 10).equals("UNROK")){
            System.out.println("UNROK : "+message.substring(11, len));
            String messagePayload = message.substring(11, len);
            String[] payLoadParts = messagePayload.split(" ");
        }else if(message.substring(5,11).equals("JOINOK")){
            System.out.println("JOINOK : "+message.substring(12,len));
            String messagePayload = message.substring(12, len);
            String[] payLoadParts = messagePayload.split(" ");
        }else if(message.substring(5,9).equals("JOIN")) {
            System.out.println("JOIN : "+message.substring(10, len));
            String messagePayload = message.substring(10, len);
            String[] payLoadParts = messagePayload.split(" ");
        }else if(message.substring(5,12).equals("LEAVEOK")) {
            System.out.println("LEAVEOK : "+message.substring(13, len));
            String messagePayload = message.substring(13, len);
            String[] payLoadParts = messagePayload.split(" ");
        }else if(message.substring(5,10).equals("LEAVE")) {
            System.out.println("LEAVE : "+message.substring(11, len));
            String messagePayload = message.substring(11, len);
            String[] payLoadParts = messagePayload.split(" ");
        }else if(message.substring(5,10).equals("SEROK")){
            System.out.println("SEROK : "+message.substring(11,len));
            String messagePayload = message.substring(11, len);
            String[] payLoadParts = messagePayload.split(" ");
        }else if(message.substring(5,8).equals("SER")) {
            System.out.println("SER : "+message.substring(9, len));
            String messagePayload = message.substring(9, len);
            String[] payLoadParts = messagePayload.split(" ");
        }else {
            System.out.println();
        }
    }
}
