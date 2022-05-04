package Node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;


public class FailureNode extends Thread {
    private final int failedNode;
    private final int currentID;
    private final int nextID;
    private final int previousID;
    private final String serverIP;
    private final String previousIP;
    private final String nextIP;
    private final DatagramSocket failureSocket;

    /* what happens when a node loses connection, it does not have a chance to tell the others, so we need a failure algorithm
    to detect this change in the network and to update the other nodes.
    If a node loses connection, it neighbours notice because they ping each other constantly, then these neighbours ask
    the nameserver for its new neighbours. Lastly, we remove this missing node from the naming server
     */
    public FailureNode(NamingNode node, int failedNode) throws SocketException {
        this.failedNode = failedNode;
        String name = node.name;
        this.currentID = node.discoveryNode.getCurrentID();
        this.nextID = node.discoveryNode.getNextID();
        this.previousID = node.discoveryNode.getPreviousID();
        this.serverIP = node.discoveryNode.getServerIP();
        this.nextIP = node.discoveryNode.getNextIP();
        this.previousIP = node.discoveryNode.getPreviousIP();
        this.failureSocket = node.discoveryNode.getFailureSocket();
        this.failureSocket.setSoTimeout(1000);
    }
    @Override
    public void run() {
        try {
            System.out.println("Failure detected at node with hash: " + failedNode);
            String serverResponse;
            serverResponse = "{\"status\":\"Failure\"," + "\"sender\":\"node\"," + "\"senderID\":" + currentID + "," +
                    "\"failedID\":" + failedNode + "}";
            // Send the nextID to the previousNode and send the previousID to the nextNode using datagrampackets
            DatagramPacket server = new DatagramPacket(serverResponse.getBytes(), serverResponse.length(), InetAddress.getByName(serverIP), 8001);
            this.failureSocket.send(server);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
