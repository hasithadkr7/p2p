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
//            byte[] buffer = new byte[65536];
//            DatagramPacket receiving_pkt = new DatagramPacket(buffer, buffer.length);
//            clientSocket.receive(receiving_pkt);
//            String received_msg = new String(receiving_pkt.getData(), 0, receiving_pkt.getLength());
//            System.out.println("received_msg: "+received_msg);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


//    public String sendMessage(String ipAddress,int port, String message) {
//        try {
//            DatagramSocket clientSocket = new DatagramSocket();
//            InetAddress ip = InetAddress.getByName(ipAddress);
//            DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.length(), ip, port);
//            clientSocket.send(sendPacket);
//            byte[] buffer = new byte[65536];
//            DatagramPacket receiving_pkt = new DatagramPacket(buffer, buffer.length);
//            clientSocket.receive(receiving_pkt);
//            String received_msg = new String(receiving_pkt.getData(), 0, receiving_pkt.getLength());
//            System.out.println("received_msg: "+received_msg);
//            return received_msg;
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        } catch (SocketException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }



}
