package Server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class Discovery extends Thread {
    boolean running = true;
    DatagramSocket socket;
    NamingServer ns;
    public Discovery(NamingServer nameserver){
        this.ns = nameserver;
        try{
            this.socket = new DatagramSocket(8001); // socket where we will receive discovery on
            this.socket.setBroadcast(true);
            this.socket.setSoTimeout(1000);
        } catch (SocketException e) {
            this.socket = null;
            System.out.println("Something went wrong");
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        if (this.socket == null) return;
        byte[] receiveBuffer = new byte[512];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        while (this.running) {
            try {
                this.socket.receive(receivePacket);
                System.out.println("Package received from: " + receivePacket.getAddress() + ":" + receivePacket.getPort());
                String receivedData = new String(receivePacket.getData(),0,receivePacket.getLength()).trim();
                System.out.println("received data: " + receivedData);
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(receivedData);
                String status = ((JSONObject) obj).get("status").toString();
                if(status.equals("Discovery")) {
                    String name = ((JSONObject) obj).get("name").toString();
                    int hash = ns.hash(name);
                    String IP = receivePacket.getAddress().getHostAddress(); //IP of the Node
                    String response;
                    if (ns.addNode(name, IP).equals("Added Node " + name + " with hash: " + hash + "\n")) {
                        //if adding is successful
                        NamingServer.ipMapLock.readLock().lock();
                        ns.logger.info(NamingServer.getIpMapping().toString());
                        Integer previousID = NamingServer.getIpMapping().lowerKey(hash);
                        if (previousID == null) previousID = hash;
                        ns.logger.info(previousID.toString());
                        Integer nextID = NamingServer.getIpMapping().higherKey(hash);
                        if (nextID == null) nextID = hash;
                        ns.logger.info(nextID.toString());
                        String previousIP = NamingServer.getIpMapping().get(previousID);
                        String nextIP = NamingServer.getIpMapping().get(nextID);
                        response = "{\"status\":\"OK\"," + "\"sender\":\"NamingServer\"," + "\"node ID\":" + hash + "," +
                                "\"node amount\":" + NamingServer.getIpMapping().size() + ","
                                + "\"previousID\":" + previousID + "," + "\"nextID\":" + nextID + "," + "\"previousIP\":" + "\"" +
                                previousIP + "\"" +  "," + "\"nextIP\":" + "\"" + nextIP + "\"" + "}";
                        NamingServer.ipMapLock.readLock().unlock();
                    } else {
                        //adding unsuccessful
                        ns.logger.info("Node already exists");
                        response = "{\"status\":\"Node already exists\"," + "\"sender\":\"NamingServer\"," + "\"node ID\":" + hash + "," +
                                "\"node amount\":" + NamingServer.getIpMapping().size() + "}";
                    }
                    DatagramPacket responsePacket = new DatagramPacket(response.getBytes(StandardCharsets.UTF_8), response.length(), receivePacket.getAddress(), receivePacket.getPort());
                    this.socket.send(responsePacket);
                }else if(status.equals("Failure")) {
                    int senderID = (int) (long) ((JSONObject) obj).get("senderID");
                    int failedID = (int) (long) ((JSONObject) obj).get("failedID");
                    String response =" ";
                    if (senderID != failedID) {
                            ns.removeNode(failedID);
                            NamingServer.ipMapLock.readLock().lock();
                            ns.logger.info(NamingServer.getIpMapping().toString());
                            Integer previousID = NamingServer.getIpMapping().lowerKey(senderID);
                            if (previousID == null) previousID = senderID;
                            ns.logger.info(previousID.toString());
                            Integer nextID = NamingServer.getIpMapping().higherKey(senderID);
                            if (nextID == null) nextID = senderID;
                            ns.logger.info(nextID.toString());
                            String previousIP = NamingServer.getIpMapping().get(previousID);
                            String nextIP = NamingServer.getIpMapping().get(nextID);
                            response = "{\"status\":\"FailureOK\"," + "\"sender\":\"NamingServer\"," + "\"removedID\":" + failedID + "," +
                                    "\"node amount\":" + NamingServer.getIpMapping().size() + ","
                                    + "\"previousID\":" + previousID + "," + "\"nextID\":" + nextID + "," + "\"previousIP\":" + "\"" +
                                    previousIP + "\"" + "," + "\"nextIP\":" + "\"" + nextIP + "\"" + "}";
                            NamingServer.ipMapLock.readLock().unlock();
                        }
                        DatagramPacket responsePacket = new DatagramPacket(response.getBytes(StandardCharsets.UTF_8), response.length(), receivePacket.getAddress(), 8001);
                        this.socket.send(responsePacket);
                    }
                } catch (IOException | ParseException e) {
                //e.printStackTrace();
            }
        }
    }
    public void terminate(){
        this.running = false;
    }
}




