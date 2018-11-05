import java.io.IOException;


public class Communicator {
    Sender sender;
    //Receiver receiver;
    PeerNode peer;

    public Communicator(PeerNode peer){
        sender=new Sender();
        this.peer=peer;
        //receiver=new Receiver(peer.nodeSelf.port, this);
    }

//    public String sendMessage(String ipAddress, int port, String message,Enum MessageType){
//        return sender.sendMessage(ipAddress,port,message);
//    }

    public void sendMessage(String ipAddress, int port, String message,Enum MessageType){
        sender.sendMessage(ipAddress,port,message);
    }

    public void onReceive(String msg){
        String temp=cleanReceived(msg.toCharArray());
        System.out.println("MSG RECEIVED: "+ temp);
        parseReceiveMessage(temp);
    }

    private String cleanReceived(char[] buf){
        String temp=new String(buf);
        String lenStr=temp.substring(0, 4);
        int len=Integer.parseInt(lenStr);
        if(len>0) return temp.substring(5,len);
        return null;
    }

    public void parseReceiveMessage(String message) {
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
            System.out.println("Unmactched Message : "+message);
        }
    }
}
