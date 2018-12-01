class Node {
    public String ip;
    public int port;
    public String userName;

    public Node(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void setUserName(String userName){
        this.userName = userName;
    }

    public String getUserName(){
        return this.userName;
    }

    @Override
    public String toString() {
        return "Node{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }
}