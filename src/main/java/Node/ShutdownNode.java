package Node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ShutdownNode extends Thread{
    private final NamingNode node;
    private final int currentID;
    private final int nextID;
    private final int previousID;
    private final String previousIP;
    private final String nextIP;
    private final DatagramSocket shutdownSocket;

    public ShutdownNode(NamingNode node) throws SocketException {
        this.node = node;
        String name = node.name;
        this.currentID = node.discoveryNode.getCurrentID();
        this.nextID = node.discoveryNode.getNextID();
        this.previousID = node.discoveryNode.getPreviousID();
        this.nextIP = node.discoveryNode.getNextIP();
        this.previousIP = node.discoveryNode.getPreviousIP();
        this.shutdownSocket = new DatagramSocket(8002);
        this.shutdownSocket.setSoTimeout(1000);
        node.delete(currentID);
    }
    @Override
    public void start(){
        try {
            System.out.println("Shutting down...");
            // Send the nextID to the previousNode and send the previousID to the nextNode using datagrampackets
            String previousResponse;
            String nextResponse;
            previousResponse = "{\"status\":\"Shutdown\"," + "\"sender\":\"nextNode\"," + "\"senderID\":" + currentID + "," +
                "\"nextID\":" + nextID + ","+ "\"nextIP\":" + "\"" + nextIP + "\"" + "}";
            DatagramPacket previousNode = new DatagramPacket(previousResponse.getBytes(), previousResponse.length(), InetAddress.getByName(previousIP), 8001);
            shutdownSocket.send(previousNode);
            nextResponse = "{\"status\":\"Shutdown\"," + "\"sender\":\"previousNode\"," + "\"senderID\":" + currentID + "," + "\"previousID\":" + previousID + "," + "\"previousIP\":" + "\"" + previousIP + "\"" + "}";
            DatagramPacket nextNode = new DatagramPacket(nextResponse.getBytes(), nextResponse.length(), InetAddress.getByName(nextIP), 8001);
            shutdownSocket.send(nextNode);
            this.node.setRunning(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
