import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Receiver extends Thread {
    DatagramSocket listener;
    Communicator communicator;

    public Receiver(int port, Communicator communicator){
        try {
            this.listener = new DatagramSocket(port);
            this.communicator = communicator;
            start();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run(){
        try {
            while (true) {
                System.out.println("Waiting for Incomming..");
                byte[] buffer = new byte[65536];
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                listener.receive(receivePacket);
                String receivedMessage = new String(receivePacket.getData());
                //String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                communicator.onReceive(receivedMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
