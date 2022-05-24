package Node;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class NamingNode {
    private static int nextID;
    private final String node_IP;
    public String name;
    private int hash;
    private static int previousID;
    private static volatile boolean running;
    private int amount;
    private String nodes;
    DiscoveryNode discoveryNode;

    public NamingNode(String name) throws IOException { //constructor
        running = true;
        this.node_IP = InetAddress.getLocalHost().getHostAddress();
        this.name = name;
        //start discovery
        this.discoveryNode = new DiscoveryNode(name, this);
        this.discoveryNode.start();


    }
    public String getFile(String filename) {
        try {
            //String URL = "http://localhost:8080/NamingServer/getFile/" + filename; //REST command
            //System.out.println(discoveryNode.getServerIP());
            //String URL = "http://" + discoveryNode.getServerIP() + ":8080/NamingServer/Files/" + filename;
            String URL = "http://" + "192.168.80.3" + ":8080/NamingServer/Files/" + filename;
            System.out.println(Unirest.get(URL).asString().getBody());
            return Unirest.get(URL).asString().getBody();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Error";
    }
    public void getNode(String user) throws UnirestException, ParseException {
        //String URL = "http://localhost:8080/NamingServer/Nodes/" + user;
        String URL = "http://" + discoveryNode.getServerIP() + ":8080/NamingServer/Nodes/" + user;
        String data = Unirest.get(URL).asString().getBody();
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(data);
        String status = ((JSONObject)obj).get("node status").toString();
        if(status.equals("Node exists")) {
            this.hash = (int) (long) ((JSONObject) obj).get("node hash");
            this.amount = (int) (long) ((JSONObject) obj).get("node amount");
            this.nodes = ((JSONObject) obj).get("nodes").toString();
        }
        else{
            System.out.println("Error, node does not exist");
        }
    }
    public void newNode(String user, String IP) throws UnirestException {
        //String URL = "http://localhost:8080/NamingServer/Nodes/" + user;
        String URL = "http://" + discoveryNode.getServerIP() + ":8080/NamingServer/Nodes/" + user;
        System.out.println(Unirest.post(URL).header("Content-Type", "application/json").body(IP).asString().getBody());

    }
    public void delete(int hash){
        try {
            String url = "http://" + discoveryNode.getServerIP() + ":8080/NamingServer/Nodes/" + hash;
            System.out.println(Unirest.delete(url).asString().getBody());
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }
    public void printOut() throws UnknownHostException {
        System.out.println("Node IP:\t\t" + this.node_IP);
        System.out.println("NamingServer IP:\t" + discoveryNode.getServerIP());
        System.out.println("Node hash:\t\t" + this.hash);
        System.out.println("Node amount:\t\t" + this.amount);
        System.out.println("Nodes:\t\t\t" + this.nodes);
        System.out.println("node hostname + IP : " + InetAddress.getLocalHost());
    }
    public static boolean getRunning(){
        return running;
    }
    public static void setRunning(boolean running2){
        running = running2;
    }
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Starting Node...");
        //turn off most of the logging
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.http");
        root.setLevel(ch.qos.logback.classic.Level.OFF);
        String name;
        if (args.length > 0) {
            name = args[0];
        } else {
            System.out.println("Please give a name to your node!");
            return;
        }
        NamingNode node = new NamingNode(name); //start new node --> also starts discovery in Thread
        new PingNode(node).start();
        //FileManager fileManager = new FileManager(node);
        //fileManager.start();
        Thread.sleep(6000);
        new ShutdownNode(node).start(); // start shutdown in different Thread

        //node.newNode(name, IP);
        //node.getNode(name);
        //node.printOut();

        //test some files
        //node.getFile("testFile.txt");
        //node.getFile("testFile2.pdf");
        //node.getFile("testFile3.jpg");
        //node.delete();
    }
}