package Node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class PingNode extends Thread{
    private NamingNode namingNode;
    public PingNode(NamingNode node){
        this.namingNode = node;
    }
    public void run(){
        while(namingNode.getRunning()){
            try {
                Thread.sleep(5000); //sleep for 5 seconds
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
            if(!namingNode.discoveryNode.isDiscoveryPhase()){ //If we are out of the discovery phase
                InetAddress previousIP;
                InetAddress nextIP;
                try {
                    previousIP = InetAddress.getByName(this.namingNode.discoveryNode.getPreviousIP());
                    nextIP = InetAddress.getByName(this.namingNode.discoveryNode.getNextIP());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    continue;
                }
                String ping = "{\"status\":\"Ping\"," + "\"senderID\":" + this.namingNode.discoveryNode.getCurrentID() + "}";
                DatagramPacket previousPing = new DatagramPacket(ping.getBytes(StandardCharsets.UTF_8), ping.length(), previousIP, 8001);
                DatagramPacket nextPing = new DatagramPacket(ping.getBytes(StandardCharsets.UTF_8), ping.length(), nextIP, 8001);
                //System.out.println("PREVIOUSID = " + this.namingNode.discoveryNode.getPreviousID());
                //System.out.println("NEXTID = " + this.namingNode.discoveryNode.getNextID());
                //System.out.println("CURRENTID = " + this.namingNode.discoveryNode.getCurrentID());
                try {
                    if(this.namingNode.discoveryNode.getCurrentID() != this.namingNode.discoveryNode.getPreviousID()) {
                        this.namingNode.discoveryNode.getAnswerSocket().send(previousPing);
                        this.namingNode.discoveryNode.setPreviousAnswer(this.namingNode.discoveryNode.getPreviousAnswer()+1);

                    }
                    if (this.namingNode.discoveryNode.getCurrentID() != this.namingNode.discoveryNode.getNextID()) {
                        this.namingNode.discoveryNode.getAnswerSocket().send(nextPing);
                        this.namingNode.discoveryNode.setNextAnswer(this.namingNode.discoveryNode.getNextAnswer()+1);
                    }
                } catch (IOException e) {
                    //e.printStackTrace();
                }
                if(this.namingNode.discoveryNode.getPreviousAnswer() > 3){ //If we ping 3 times without receiving an answer then failure
                    try {
                        new FailureNode(this.namingNode,this.namingNode.discoveryNode.getPreviousID()).start(); //assume failure of that node
                        this.namingNode.discoveryNode.setPreviousAnswer(0);
                    } catch (SocketException e) {
                        //e.printStackTrace();
                    }
                }
                if(this.namingNode.discoveryNode.getNextAnswer() > 3){
                    try {
                        new FailureNode(this.namingNode,this.namingNode.discoveryNode.getNextID()).start();
                        this.namingNode.discoveryNode.setNextAnswer(0);
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }
}
