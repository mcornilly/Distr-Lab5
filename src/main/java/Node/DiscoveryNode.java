package Node;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiscoveryNode extends Thread {
    private DatagramSocket discoverySocket; //socket used for discovery
    private DatagramSocket answerSocket; //socket used to answer discoveries / shutdowns
    private DatagramSocket failureSocket;
    private final InetAddress broadcastAddress; //address used to broadcast
    private int amount; //amount of nodes in the system
    private String currentIP; //IP of the node
    private String previousIP; //IP of the previous node
    private String nextIP; //IP of the next node
    private String serverIP; //IP of the server
    private int currentID; //ID of the node
    private int previousID; //ID of the previous node
    private int nextID; //ID of the next node
    private String name; //name of the current node
    private final NamingNode node; //NamingNode
    private volatile boolean discoveryPhase;
    private int previousAnswer;
    private int nextAnswer;


    /*GETTERS AND SETTERS*/

    public DatagramSocket getFailureSocket() {
        return failureSocket;
    }
    public void setFailureSocket(DatagramSocket failureSocket) {
        this.failureSocket = failureSocket;
    }
    public int getPreviousAnswer() {
        return previousAnswer;
    }
    public void setPreviousAnswer(int previousAnswer) {
        this.previousAnswer = previousAnswer;
    }
    public int getNextAnswer() {
        return nextAnswer;
    }
    public void setNextAnswer(int nextAnswer) {
        this.nextAnswer = nextAnswer;
    }
    public boolean isDiscoveryPhase() {
        return discoveryPhase;
    }
    public void setDiscoveryPhase(boolean discoveryPhase) {
        this.discoveryPhase = discoveryPhase;
    }
    public DatagramSocket getDiscoverySocket() {
        return discoverySocket;
    }
    public void setDiscoverySocket(DatagramSocket discoverySocket) {
        this.discoverySocket = discoverySocket;
    }
    public DatagramSocket getAnswerSocket() {
        return answerSocket;
    }
    public void setAnswerSocket(DatagramSocket answerSocket) {
        this.answerSocket = answerSocket;
    }
    public InetAddress getBroadcastAddress() {
        return broadcastAddress;
    }
    public int getAmount() {
        return amount;
    }
    public void setAmount(int amount) {
        this.amount = amount;
    }
    public String getCurrentIP() {
        return currentIP;
    }
    public void setCurrentIP(String currentIP) {
        this.currentIP = currentIP;
    }
    public String getServerIP() {
        return serverIP;
    }
    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }
    public int getCurrentID() {
        return currentID;
    }
    public void setCurrentID(int currentID) {
        this.currentID = currentID;
    }
    public int getPreviousID() {
        return previousID;
    }
    public void setPreviousID(int previousID) {
        this.previousID = previousID;
    }
    public int getNextID() {
        return nextID;
    }
    public void setNextID(int nextID) {
        this.nextID = nextID;
    }
    public String getPreviousIP() {
        return previousIP;
    }
    public void setPreviousIP(String previousIP) {
        this.previousIP = previousIP;
    }
    public String getNextIP() {
        return nextIP;
    }
    public void setNextIP(String nextIP) {
        this.nextIP = nextIP;
    }
    public String getNodeName() {
        return name;
    }
    public void setNodeName(String name) {
        this.name = name;
    }
    public NamingNode getNode() {
        return node;
    }

    public DiscoveryNode(String name, NamingNode node) throws IOException {
        this.node = node;
        this.broadcastAddress = InetAddress.getByName("255.255.255.255"); //Broadcast
        try{
            this.nextAnswer = 0;
            this.previousAnswer = 0;
            this.discoveryPhase = true;
            this.name = name;
            this.discoverySocket = new DatagramSocket(8000, InetAddress.getLocalHost());
            this.answerSocket = new DatagramSocket(8001); //socket for answering the broadcast
            this.failureSocket = new DatagramSocket(8003);
            this.failureSocket.setSoTimeout(1000);
            this.discoverySocket.setBroadcast(true);
            this.discoverySocket.setSoTimeout(2000); //broadcast after sending
            this.answerSocket.setBroadcast(true);
            this.answerSocket.setSoTimeout(1000);
            this.currentIP = InetAddress.getLocalHost().getHostAddress(); //current IP of the node
            this.amount = 100; //start amount --> 100 so we don't leave discoveryPhase whilst the server hasnt answer yetµ



        } catch (SocketException e) {
            this.discoverySocket = null;
            this.answerSocket = null;
            System.out.println("Something went wrong during construction of DiscoveryNode");
            e.printStackTrace();
        }
    }
    @Override
    public void run(){
        List<String> nodesList = new ArrayList<>(); //list that keeps track of which nodes answered already
        List<String> nodesList2 = new ArrayList<>(); //list that keeps track of which nodes we already received discovery
        byte[] receive = new byte[512];
        //Discovery broadcast
        String send = "{\"status\":\"Discovery\"," + "\"name\":" +"\"" + getNodeName() + "\"" + "}";
        DatagramPacket sendPacket = new DatagramPacket(send.getBytes(StandardCharsets.UTF_8), send.length(), getBroadcastAddress(), 8001); //broadcast on port 8001
        DatagramPacket receivePacket = new DatagramPacket(receive, receive.length);  // receivePacket
        while (isDiscoveryPhase() && getNode().getRunning()) { // send a datagram packet until everyone answers
            try {
                Thread.sleep(1000);

                getDiscoverySocket().send(sendPacket);
                System.out.println("sent packet to: " + sendPacket.getSocketAddress());
                getDiscoverySocket().receive(receivePacket); // receive a packet on this socket
                String receivedData = new String(receivePacket.getData(),0,receivePacket.getLength()).trim();
                System.out.println("Packet received from: " + receivePacket.getSocketAddress());
                System.out.println("received data: " + receivedData);
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(receivedData);
                String status = ((JSONObject) obj).get("status").toString();
                String sender = ((JSONObject) obj).get("sender").toString();
                switch (sender) {
                    case "NamingServer":
                        setServerIP(String.valueOf(receivePacket.getAddress().getHostAddress()));
                        setCurrentID((int) (long) ((JSONObject) obj).get("node ID"));
                        setAmount((int) (long) ((JSONObject) obj).get("node amount"));
                        if (status.equals("OK")) {
                            setPreviousID((int) (long) ((JSONObject) obj).get("previousID"));
                            setNextID((int) (long) ((JSONObject) obj).get("nextID"));
                            setPreviousIP((String) ((JSONObject) obj).get("previousIP"));
                            setNextIP((String) ((JSONObject) obj).get("nextIP"));
                         }
                        break;
                    case "Node":
                        break;
                }
                //make sure we get answer from ALL nodes so use diff IPS
                if(!nodesList.contains(receivePacket.getAddress().getHostAddress())) {
                    nodesList.add(receivePacket.getAddress().getHostAddress());
                }
                if(nodesList.size() >= getAmount()){ // If we received from all Nodes and the server --> discoveryPhase false
                    setDiscoveryPhase(false);
                }
            }
            catch (IOException | ParseException | InterruptedException e) {
                // e.printStackTrace();
            }
        }
        while(getNode().getRunning()) {
            try {
                getAnswerSocket().receive(receivePacket);
                String s1 = receivePacket.getAddress().toString(); //IP of the sending node
                String s2 = "/" + InetAddress.getLocalHost().getHostAddress(); //IP of the current node
                String IP = receivePacket.getAddress().getHostAddress(); //IP of the sending node
                String receivedData = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(receivedData);
                String status = ((JSONObject) obj).get("status").toString();
                if(status.equals("Discovery")) {
                    if ((!s1.equals(s2)) && (!nodesList2.contains(IP))) { // We only listen to other IP than our own and only IPs we havent listened to.
                        System.out.println("Package received from: " + receivePacket.getAddress() + ":" + receivePacket.getPort());
                        nodesList2.add(IP); //add to our list of received IPs
                        String response;
                        String name = ((JSONObject) obj).get("name").toString();
                        int hash = ToHash.hash(name);
                        if (getCurrentID() < hash && (hash < getNextID() || getNextID() == getCurrentID())) {
                            setNextID(hash);
                            setNextIP(IP);
                            response = "{\"status\":\"nextID changed\"," + "\"sender\":\"Node\"," + "\"senderID\":" + getCurrentID() + "," +
                                    "\"nextID\":" + getNextID() + "," + "\"previousID\":" + getPreviousID() + "}";
                        } else if (hash < getCurrentID() && (getPreviousID() < hash || getPreviousID() == getCurrentID())) {
                            setPreviousID(hash);
                            setPreviousIP(IP);
                            response = "{\"status\":\"previousID changed\"," + "\"sender\":\"Node\"," + "\"senderID\":" + getCurrentID() + "," +
                                    "\"nextID\":" + getNextID() + "," + "\"previousID\":" + getPreviousID() + "}";
                        } else {
                            response = "{\"status\":\"Nothing changed\"," + "\"sender\":\"Node\"," + "\"senderID\":" + getCurrentID() + "," +
                                    "\"nextID\":" + getNextID() + "," + "\"previousID\":" + getPreviousID() + "}";
                        }
                        DatagramPacket responsePacket = new DatagramPacket(response.getBytes(StandardCharsets.UTF_8), response.length(), receivePacket.getAddress(), receivePacket.getPort());
                        getAnswerSocket().send(responsePacket);
                    }
                }
                //If a neighbour node shuts down, handle this packet and update our neighbours
                if(status.equals("Shutdown")){
                    if(!s1.equals(s2)) {
                        System.out.println("Package received from:  " + receivePacket.getAddress() + ":" + receivePacket.getPort());
                        System.out.println("received data: " + receivedData);
                        String sender = ((JSONObject) obj).get("sender").toString(); //get the sender, either nextNode or PreviousNode
                        int senderID = (int) (long) ((JSONObject) obj).get("senderID"); //get senderID
                        if (sender.equals("nextNode")) { // If the sender is the next neighbour
                            setNextID((int) (long) ((JSONObject) obj).get("nextID")); //update neighbour
                            setNextIP((String) ((JSONObject) obj).get("nextIP"));
                            if (senderID == getNextID()) { // If the sender is the last node
                                setNextID(getCurrentID()); //make current node the last node
                                setNextIP(getCurrentIP());
                            }
                        } else if (sender.equals("previousNode")) {
                            setPreviousID((int) (long) ((JSONObject) obj).get("previousID"));
                            setPreviousIP((String) ((JSONObject) obj).get("previousIP"));
                            if (senderID == getPreviousID()) { // If the sender is the last node
                                setPreviousID(getCurrentID()); //make current node the last node
                                setPreviousIP(getCurrentIP());
                            }
                        }
                        setAmount(getAmount() - 1); // Lower amount by 1 because there is one less node
                    }
                }
                if(status.equals("FailureOK")){
                    if(!s1.equals(s2)) {
                        System.out.println("Package received from:  " + receivePacket.getAddress() + ":" + receivePacket.getPort());
                        System.out.println("received data: " + receivedData);
                        int removedID = (int) (long) ((JSONObject) obj).get("removedID"); //get removedID
                        setNextID((int) (long) ((JSONObject) obj).get("nextID")); //update neighbour
                        setNextIP((String) ((JSONObject) obj).get("nextIP"));
                        setPreviousID((int) (long) ((JSONObject) obj).get("previousID"));
                        setPreviousIP((String) ((JSONObject) obj).get("previousIP"));
                        setAmount((int) (long) ((JSONObject) obj).get("node amount"));
                    }
                }if(status.equals("Ping")){
                    if(!s1.equals(s2)) {
                        System.out.println("Package received from:  " + receivePacket.getAddress() + ":" + receivePacket.getPort());
                        System.out.println("received data: " + receivedData);
                        int senderID = (int) (long) ((JSONObject) obj).get("senderID"); //get senderID
                        if (senderID == getPreviousID()){
                            setPreviousAnswer(0);
                        }
                        if(senderID == getNextID()){
                            setNextAnswer(0);
                        }
                    }               }
            } catch (IOException | ParseException e) {
                //e.printStackTrace();
            }

        }

    }


}