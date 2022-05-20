package Node;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ShutdownNode extends Thread{
    private final NamingNode node;
    private final int currentID;
    private final int nextID;
    private final int previousID;
    private final String previousIP;
    private final String nextIP;
    private final DatagramSocket shutdownSocket;
    //private final FileManager fileManager;
    private final HashMap<String, String> sharedfiles;

    public ShutdownNode(NamingNode node) throws SocketException {
        this.node = node;
        //this.fileManager = fileManager;
        String name = node.name;
        this.currentID = node.discoveryNode.getCurrentID();
        this.nextID = node.discoveryNode.getNextID();
        this.previousID = node.discoveryNode.getPreviousID();
        this.nextIP = node.discoveryNode.getNextIP();
        this.previousIP = node.discoveryNode.getPreviousIP();
        this.shutdownSocket = new DatagramSocket(8002);
        this.shutdownSocket.setSoTimeout(1000);
        this.sharedfiles = FileManager.getSentFiles();
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
            //sharedFilesShutdown();
            this.node.delete(currentID);
            this.node.setRunning(false);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sharedFilesShutdown() throws IOException {
        // tell owners of the file that we are shutting down
        Set<Map.Entry<String,String>> entries = this.sharedfiles.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String response;
            response = "{\"status\":\"ShutdownFile\","  + "\"senderID\":" + currentID + "," +
                     "\"filename\":" + "\"" + entry.getKey() + "\"" + "}";
            DatagramPacket file = new DatagramPacket(response.getBytes(), response.length(), InetAddress.getByName(entry.getValue()), 8001); // In Discovery node nog antwoord krijgen
            shutdownSocket.send(file);
        }
    }

    public void sendReplicatedFile() {

    }


}


