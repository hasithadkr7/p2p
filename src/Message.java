public class Message {
    private Enum  MessageType;

    public Enum getMessageType() {
        return MessageType;
    }

    public void setMessageType(Enum messageType) {
        MessageType = messageType;
    }
}

class RegisterResponseMessage extends Message{
    private String peer1Ip = "", peer2Ip = "";
    private int peer1Port, peer2Port;

    public RegisterResponseMessage(String payLoad){
        this.processPayLoad(payLoad);
        this.setMessageType(InitConfig.MessageType.MSG_REGOK);
    }

    public String getPeer1Ip() {
        return peer1Ip;
    }

    public void setPeer1Ip(String peer1Ip) {
        this.peer1Ip = peer1Ip;
    }

    public String getPeer2Ip() {
        return peer2Ip;
    }

    public void setPeer2Ip(String peer2Ip) {
        this.peer2Ip = peer2Ip;
    }

    public int getPeer1Port() {
        return peer1Port;
    }

    public void setPeer1Port(int peer1Port) {
        this.peer1Port = peer1Port;
    }

    public int getPeer2Port() {
        return peer2Port;
    }

    public void setPeer2Port(int peer2Port) {
        this.peer2Port = peer2Port;
    }

    private void processPayLoad(String payLoad){
        String[] payLoadParts = payLoad.split(" ");
        int memberCount = Integer.parseInt(payLoadParts[0]);
        switch(memberCount){
            case 1:
                this.setPeer1Ip(payLoadParts[1]);
                this.setPeer1Port(Integer.parseInt(payLoadParts[2]));
                break;
            case 2:
                this.setPeer1Ip(payLoadParts[1]);
                this.setPeer1Port(Integer.parseInt(payLoadParts[2]));
                this.setPeer2Ip(payLoadParts[3]);
                this.setPeer2Port(Integer.parseInt(payLoadParts[4]));
                break;
            default:
                System.out.println("No members found.");
        }
    }
}

class UnRegisterResponseMessage extends Message{
    private int status;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public UnRegisterResponseMessage(String payLoad){
        this.processPayLoad(payLoad);
        this.setMessageType(InitConfig.MessageType.MSG_UNREG);
    }

    private void processPayLoad(String payLoad){
        String[] payLoadParts = payLoad.split(" ");
        this.setStatus(Integer.parseInt(payLoadParts[0]));
    }
}