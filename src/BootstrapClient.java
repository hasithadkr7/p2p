import java.io.IOException;
import java.net.*;

//length REG IP_address port_no username
//0036 REG 129.82.123.45 5001 1234abcd
public class BootstrapClient {
    public static void main(String[] args) {
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket();
            //String sending_msg = "0036 REG 129.82.123.48 5001 1234abcd";
            String sending_msg = "0034 REG 127.0.0.1 5602 localnode6";
            try {
                InetAddress ip = InetAddress.getByName("127.0.0.1");
                DatagramPacket sending_pkt = new DatagramPacket(sending_msg.getBytes(), sending_msg.length(), ip, 55555);
                try {
                    ds.send(sending_pkt);
                    byte[] buffer = new byte[65536];
                    DatagramPacket receiving_pkt = new DatagramPacket(buffer, buffer.length);
                    ds.receive(receiving_pkt);
                    String received_msg = new String(receiving_pkt.getData(), 0, receiving_pkt.getLength());
                    System.out.println(received_msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } finally {
            //Closing the socket
            try {
                ds.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
