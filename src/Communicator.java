import java.io.IOException;


public class Communicator {
    Sender sender;
    Receiver receiver;
    PeerNode peer;

    public Communicator(PeerNode peer){
        sender=new Sender();
        this.peer=peer;
        receiver=new Receiver(peer.nodeSelf.port, this);
    }

    public void sendMessage(String ipAddress, int port, String message,Enum MessageType){
        sender.sendMessage(ipAddress,port,message);
    }

}
