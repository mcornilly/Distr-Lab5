package Node;

import Node.NamingNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.persistence.criteria.CriteriaBuilder;
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.NotDirectoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class FileManager extends Thread {
    private final NamingNode node;
    private DiscoveryNode discoveryNode;
    private ArrayList<String> fileList = new ArrayList<>();

    public static HashMap<String, String> getSentFiles() {
        return sentFiles;
    }

    public static void setSentFiles(HashMap<String, String> sentFiles) {
        FileManager.sentFiles = sentFiles;
    }

    public boolean isSendFiles() {
        return sendFiles;
    }

    public void setSendFiles(boolean sendFiles) {
        this.sendFiles = sendFiles;
    }

    public boolean isStartup() {
        return startup;
    }

    public void setStartup(boolean startup) {
        this.startup = startup;
    }

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    private static HashMap<String, String> sentFiles = new HashMap<>(); //files we have shared
    private static HashMap<String, String> receivedFiles = new HashMap<>();

    private volatile boolean sendFiles;
    private boolean startup;
    private volatile boolean update;
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private File[] localFiles;
    private File localFolder;
    private File replicatedFolder;
    private File[] replicatedFiles;
    FileChecker fileChecker;
    // Determines when to send or receive a file and where to send it to,
    //If we start a node, we want to send all our files to the respective
    public FileManager(NamingNode node, DiscoveryNode discoveryNode) throws IOException {
        this.node = node;
        this.discoveryNode = discoveryNode;
        this.startup = true;
        this.sendFiles = true;
        this.update = false;
        String launchDirectory = System.getProperty("user.dir");
        //System.out.println(launchDirectory);
        this.localFolder = new File(launchDirectory + "/src/main/resources/LocalFiles"); //All localfiles
        this.replicatedFolder = new File( launchDirectory + "/src/main/resources/ReplicatedFiles");
        this.localFiles = this.localFolder.listFiles();
        System.out.println("All LocalFiles at startup: " + Arrays.toString(this.localFiles));
        this.fileChecker = new FileChecker(node, launchDirectory + "/src/main/resources/LocalFiles"); //check local directory for changes
        this.fileChecker.start();

    }
    @Override
    public void run(){
        //Starting the FileManager
        //what if a node is added? maybe here in filemanager or filechecker another function
        while(this.discoveryNode.getNode().getRunning()) {  //while the node is running, issues with volatile
            while(this.sendFiles) {
                // System.out.println(this.node.discoveryNode.isDiscoveryPhase());
                if (this.startup && !this.discoveryNode.isDiscoveryPhase()) { //if the node is out of the discovery phase
                    //System.out.println(Arrays.toString(this.localFiles));
                    System.out.println("Distribute all our LocalFiles at startup");
                    for (File f : this.localFiles) { // for every local File
                        try {
                            System.out.println("Filename:" + f.getName()); //print out the name
                            String fileLocation = this.node.getFile(f.getName()); //get the location where the file should be
                            sendFile(f, fileLocation);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    this.startup = false;
                    this.sendFiles = false;
                }
                else if(sendFiles && update) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    this.localFiles = this.localFolder.listFiles();
                    for (File f : this.localFiles) { // for every local File
                        try {
                            if (!sentFiles.containsKey(f.getName())) //If the file is not in the shared lists so we still have it ourselves, check if we need to send it
                            {
                                System.out.println("Filename:" + f.getName()); //print out the name
                                String fileLocation = this.node.getFile(f.getName()); //get the location where the file should be
                                sendFile(f, fileLocation);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    this.replicatedFiles = this.replicatedFolder.listFiles(); //update to most recent
                    for (File f : this.replicatedFiles){ //check every replicatedFile if we need to move is, and delete it ourselves and tell the local owner
                        try {
                            System.out.println("Filename:" + f.getName()); //print out the name
                            String fileLocation = this.node.getFile(f.getName()); //get the location where the file should be
                            boolean transfer = sendFile(f, fileLocation); //transfer = true if the files was sent
                            if (transfer){
                                //sent message that the file is updated to the local owner
                                String update = "{\"status\":\"UpdateFile\"," + "\"senderID\":" + this.discoveryNode.getCurrentID() + ","
                                        + "\"filename\":" + "\"" + f.getName() + "\"" + "," + "\"location\":" + "\"" + fileLocation + "\"" + "}";
                                DatagramPacket updateFile = new DatagramPacket(update.getBytes(StandardCharsets.UTF_8), update.length(), InetAddress.getByName(receivedFiles.get(f.getName())), 8001);
                                this.discoveryNode.getAnswerSocket().send(updateFile); //sent the packet
                                receivedFiles.remove(f.getName()); //remove from our receivedfiles map
                                f.delete(); //delete the file in replicated folder because we  sent it to the right owner
                            }
                        } catch (Exception e){

                        }
                    }
                    //for sending files when we get a new neighbour, check all of our localfiles and replicatedFiles if we need to pass them on to this new neighbour
                    //send replicated file to its new owner
                    this.update = false;
                    this.sendFiles = false;
                }
            }
        try(ServerSocket receivingSocket = new ServerSocket(5000)){ // Try connecting to port 5000 to start listening to clients
            while(!this.sendFiles) { //while we are not sending anymore
                Socket sendingSocket = receivingSocket.accept(); //try accepting sockets
                dataInputStream = new DataInputStream(sendingSocket.getInputStream());
                System.out.println(sendingSocket + " connected for receiving a file");
                String remoteIP = sendingSocket.getLocalAddress().getHostAddress();
                // System.out.println("IP" + remoteIP);
                receiveFile(this.replicatedFolder.toString(), remoteIP); //receive the file
                //receivingSocket.close();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        }
    }
    //Handling receive & send of files
    private void receiveFile(String path, String remoteIP) throws Exception{
        int bytes = 0;
        String fileName = dataInputStream.readUTF();
        FileOutputStream fileOutputStream = new FileOutputStream(path + "/" + fileName);
        long size = dataInputStream.readLong();     // read file size
        byte[] buffer = new byte[4*1024];
        while (size > 0 && (bytes = dataInputStream.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
            fileOutputStream.write(buffer,0,bytes);
            size -= bytes;      // read upto file size
        }
        System.out.println("File received succesfully");
        fileOutputStream.close();
        this.replicatedFiles = this.replicatedFolder.listFiles();
        //System.out.println(remoteIP);
        receivedFiles.put(fileName, remoteIP);
        System.out.println("ReplicatedFiles: " + Arrays.toString(this.replicatedFiles));
    }
    static boolean sendFile(File file, String fileLocation) throws Exception{
        if (!fileLocation.equals("Error")) {
            JSONParser parser = new JSONParser();
            try {
                Object obj = parser.parse(fileLocation);
                int locationID = (int) (long) ((JSONObject) obj).get("node ID"); // get ID where the file should be
                //System.out.println("Sending to: " + InetAddress.getLocalHost().getHostAddress());
                String locationIP = ((JSONObject) obj).get("node IP").toString(); // get IP where the file should be
                if(!locationIP.equals(InetAddress.getLocalHost().getHostAddress())) { // if the file should be transferred
                    try(Socket sendingSocket = new Socket(InetAddress.getByName(locationIP), 5000)) {
                        //Socket sendingSocket = new Socket(InetAddress.getByName(IP), 5000);
                        dataOutputStream = new DataOutputStream(sendingSocket.getOutputStream());
                        int bytes = 0;
                        FileInputStream fileInputStream = new FileInputStream(file);
                        dataOutputStream.writeUTF(file.getName());
                        dataOutputStream.flush();
                        dataOutputStream.writeLong(file.length());
                        byte[] buffer = new byte[4 * 1024];
                        while ((bytes = fileInputStream.read(buffer)) != -1) {
                            dataOutputStream.write(buffer, 0, bytes);
                            dataOutputStream.flush();
                        }
                        fileInputStream.close();
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                    sentFiles.put(file.getName(), locationIP);
                    System.out.println("File was sent to: " + locationIP);
                }
                System.out.println("sentFiles log: " + sentFiles);
                return !locationIP.equals(InetAddress.getLocalHost().getHostAddress());
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return false;
    }
    static void deleteFile(File file, String fileLocation) { //

    }
}
