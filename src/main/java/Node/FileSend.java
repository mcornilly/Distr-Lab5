package Node;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class FileSend extends Thread {
    private final NamingNode node;
    private DiscoveryNode discoveryNode;
    private ArrayList<String> fileList = new ArrayList<>();

    public static HashMap<String, String> getSentFiles() {
        return sentFiles;
    }

    public void setSentFiles(HashMap<String, String> sentFiles) {
        FileSend.sentFiles = sentFiles;
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

    public void setShutdown(boolean shutdown) { this.shutdown = shutdown; }

    private static HashMap<String, String> sentFiles = new HashMap<>(); //files we have shared (LOCAL --> REPLICATED)
    //private static HashMap<String, String> receivedFiles = new HashMap<>(); //files we are owner of (REPLICATED <-- LOCAL)

    private boolean startup;
    private volatile boolean shutdown;
    private volatile boolean update;
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private File[] localFiles;

    public static File getLocalFolder() {
        return localFolder;
    }

    public static void setLocalFolder(File localFolder) {
        FileSend.localFolder = localFolder;
    }

    private static File localFolder;

    public static File getReplicatedFolder() {
        return replicatedFolder;
    }

    public static void setReplicatedFolder(File replicatedFolder) {
        FileSend.replicatedFolder = replicatedFolder;
    }

    private static File replicatedFolder;
    private File[] replicatedFiles;
    private static FileReceive fileReceive;
    private static DatagramSocket responseSocket;

    // Determines when to send or receive a file and where to send it to,
    //If we start a node, we want to send all our files to the respective
    public FileSend(NamingNode node, DiscoveryNode discoveryNode) throws IOException {
        this.node = node;
        this.discoveryNode = discoveryNode;
        this.startup = true;
        this.update = false;
        this.shutdown = false;
        responseSocket = discoveryNode.getAnswerSocket();
        String launchDirectory = System.getProperty("user.dir");
        localFolder = new File(launchDirectory + "/src/main/resources/LocalFiles"); //All localfiles
        this.localFiles = localFolder.listFiles();
        System.out.println("All LocalFiles at startup: " + Arrays.toString(this.localFiles));
        replicatedFolder = new File(launchDirectory + "/src/main/resources/ReplicatedFiles");
        FileChecker fileChecker = new FileChecker(this.node, launchDirectory + "/src/main/resources/LocalFiles"); //check local directory for changes
        fileChecker.start();
        //start receiving files in different Thread.
        fileReceive = new FileReceive(this.node, this.discoveryNode);
        fileReceive.start();
    }

    @Override
    public void run() {
        //Starting the FileManager
        //what if a node is added? maybe here in filemanager or filechecker another function
        while (NamingNode.getRunning()) {  //while the node is running, issues with volatile
                // System.out.println(this.node.discoveryNode.isDiscoveryPhase());
                if (this.startup && !this.discoveryNode.isDiscoveryPhase()) { //if the node is out of the discovery phase
                    //System.out.println(Arrays.toString(this.localFiles));
                    System.out.println("Distribute all our LocalFiles at startup");
                    for (File f : this.localFiles) { // for every local File
                        try {
                            System.out.println("    Filename:" + f.getName()); //print out the name
                            String fileLocation = this.node.getFile(f.getName()); //get the location where the file should be
                            sendFile(f, fileLocation, false, false, "");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    this.startup = false;
                }
                if (this.update) {
                    System.out.println("updating our local and replicated files because there is a new node in the system");
                    this.localFiles = localFolder.listFiles();
                    for (File f : this.localFiles) { // for every local File
                        try {
                            if (!sentFiles.containsKey(f.getName())) //If the file is not in the shared lists so we still have it ourselves, check if we need to send it
                            {
                                System.out.println("    Filename:" + f.getName()); //print out the name
                                String fileLocation = this.node.getFile(f.getName()); //get the location where the file should be
                                sendFile(f, fileLocation, false, false, "");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    this.replicatedFiles = replicatedFolder.listFiles(); //update to most recent
                    for (File f : this.replicatedFiles) { //check every replicatedFile if we need to move is, and delete it ourselves and tell the local owner
                        try {
                            System.out.println("    Filename:" + f.getName()); //print out the name
                            String fileLocation = this.node.getFile(f.getName()); //get the location where the file should be
                            sendFile(f, fileLocation, true, false, ""); //transfer = true if the files was sent
                            f.delete(); //delete the file in replicated folder because we  sent it to the right owner
                        } catch (Exception e) {
                        }
                    }
                    //for sending files when we get a new neighbour, check all of our localfiles and replicatedFiles if we need to pass them on to this new neighbour
                    //send replicated file to its new owner
                    this.update = false;
                }
        }
    }

    //Handling receive & send of files
    static boolean sendFile(File file, String fileLocation, boolean resending, boolean localOwner, String previousIP) throws Exception {
        if (!fileLocation.equals("Error")) {
            JSONParser parser = new JSONParser();
            try {
                Object obj = parser.parse(fileLocation);
                int locationID = (int) (long) ((JSONObject) obj).get("node ID"); // get ID where the file should be
                //System.out.println("Sending to: " + InetAddress.getLocalHost().getHostAddress());
                String locationIP = ((JSONObject) obj).get("node IP").toString(); // get IP where the file should be
                char localNumber = locationIP.charAt(locationIP.length() - 1); //get the last char --> for 192.168.6.2 this is 2
                int portNumber = Integer.parseInt("500" + localNumber); // so this will be 5002 on 6.2, receive on this port
                if (!locationIP.equals(InetAddress.getLocalHost().getHostAddress())) { // if the file should be transferred
                    try (Socket sendingSocket = new Socket(InetAddress.getByName(locationIP), portNumber)) {
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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if(resending){
                        //message
                        updateMessage(file.getName(), locationIP, localOwner, previousIP);
                        FileReceive.getReceivedFiles().remove(file.getName()); //remove from our receivedfiles map

                    }else{
                        sentFiles.put(file.getName(), locationIP);
                        System.out.println("    sentFiles log: " + sentFiles);
                    }
                    System.out.println("    File was sent to: " + locationIP);
                }
                return !locationIP.equals(InetAddress.getLocalHost().getHostAddress());
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return false;
    }

     static void updateMessage(String filename, String locationIP, boolean previousOwner, String previousIP) throws IOException {
        //tell owner of the file that we are moving the replicated file so he can keep track in his log
        //sent message that the file is updated to the local owner
        String update = "{\"status\":\"UpdateFile\","  + "\"filename\":" + "\"" + filename + "\""
                + "," + "\"location\":" + "\"" + locationIP + "\"" + "}";
        //System.out.println(FileReceive.getReceivedFiles().get(f.getName()));
         //
        if(!previousOwner) {
            DatagramPacket updateFile = new DatagramPacket(update.getBytes(StandardCharsets.UTF_8), update.length(), InetAddress.getByName(FileReceive.getReceivedFiles().get(filename)), 8001);
            responseSocket.send(updateFile); //sent the packet
        }else{
            DatagramPacket updateFile = new DatagramPacket(update.getBytes(StandardCharsets.UTF_8), update.length(), InetAddress.getByName(previousIP), 8001);
            responseSocket.send(updateFile); //sent the packet
        }


    }

     static void deleteMessage(File file, String fileLocation) { //
        //tell replicated to delete the file in his replicated folder
        if (!fileLocation.equals("Error")) {
            JSONParser parser = new JSONParser();
            try {
                Object obj = parser.parse(fileLocation);
                int locationID = (int) (long) ((JSONObject) obj).get("node ID"); // get ID where the file should be
                //System.out.println("Sending to: " + InetAddress.getLocalHost().getHostAddress());
                String locationIP = ((JSONObject) obj).get("node IP").toString(); // get IP where the file should be
                if (!locationIP.equals(InetAddress.getLocalHost().getHostAddress())) { // if the file should be deleted
                    //tell owner to delete it, maybe with UDP message?
                    //udp to discoverynode, where we handle it
                    String response;
                    response = "{\"status\":\"DeleteFile\","  +
                            "\"filename\":\"" + file.getName() + "\"" + "," +  "\"folder\":\"replicated\"" + "}";
                    DatagramPacket delete = new DatagramPacket(response.getBytes(), response.length(), InetAddress.getByName(locationIP), 8001); // In Discovery node nog antwoord krijgen
                    responseSocket.send(delete);
                    getSentFiles().remove(file.getName());  //remove this sent files from our log, we have a log with
                    // where our local files are replicated and where our replicated files come from
                    }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void deleteFile(String filename, String folder){
        FilenameFilter filenameFilter = (files, s) -> s.startsWith(filename);
        if(folder.equals("replicated")) {
            File[] replicatedFiles = replicatedFolder.listFiles(filenameFilter); //only get the affected file
            System.out.println("    Deleting replicated file: " + filename); //print out the name
            assert replicatedFiles != null;
            FileReceive.getReceivedFiles().remove(filename);
            replicatedFiles[0].delete(); //delete the file

        }
        if(folder.equals("local")){
            File[] localFiles = localFolder.listFiles(filenameFilter);
            System.out.println("    Deleting local file: " + filename);
            assert localFiles != null;
            localFiles[0].delete();
        }
    }
     void ShutdownFile(int previousID, String previousIP){
        //all of our replicated files should be moved to the previous node
        this.replicatedFiles = replicatedFolder.listFiles(); //update to most recent
        System.out.println("Moving our replicated files because of Shutdown");
        for (File f: this.replicatedFiles){
            try {
                System.out.println("    Filename:" + f.getName()); //print out the name
                //Move replicated file to the previous node
                sendFile(f, "{\"file\":" + "\"" + f.getName() + "\"" + "," + "\"node ID\":" + previousID + "," +
                        "\"node IP\":" + "\"" +  previousIP + "\"" +  "}", true, false, "");
            } catch (Exception e){
            }
        }
    }
}

