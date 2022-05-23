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
    private static File localFolder;
    private static File replicatedFolder;
    private File[] replicatedFiles;
    private FileChecker fileChecker;
    private static DatagramSocket responseSocket;

    // Determines when to send or receive a file and where to send it to,
    //If we start a node, we want to send all our files to the respective
    public FileSend(NamingNode node, DiscoveryNode discoveryNode) throws IOException {
        //startup
        this.node = node;
        this.discoveryNode = discoveryNode;
        this.startup = true;
        this.sendFiles = true;
        this.update = false;
        //for deleting a file
        responseSocket = discoveryNode.getAnswerSocket();

        String launchDirectory = System.getProperty("user.dir");
        localFolder = new File(launchDirectory + "/src/main/resources/LocalFiles"); //All localfiles
        replicatedFolder = new File(launchDirectory + "/src/main/resources/ReplicatedFiles");
        this.localFiles = this.localFolder.listFiles();

        System.out.println("All LocalFiles at startup: " + Arrays.toString(this.localFiles));
        this.fileChecker = new FileChecker(node, launchDirectory + "/src/main/resources/LocalFiles"); //check local directory for changes
        this.fileChecker.start();

    }

    @Override
    public void run() {
        //Starting the FileManager
        //what if a node is added? maybe here in filemanager or filechecker another function
        while (this.discoveryNode.getNode().getRunning()) {  //while the node is running, issues with volatile
            while (this.sendFiles) {
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
                if (this.update) {
                    System.out.println("updating our local and replicated files because there is a new node in the system");
                    this.localFiles = localFolder.listFiles();
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
                    this.replicatedFiles = replicatedFolder.listFiles(); //update to most recent
                    for (File f : this.replicatedFiles) { //check every replicatedFile if we need to move is, and delete it ourselves and tell the local owner
                        try {
                            System.out.println("Filename:" + f.getName()); //print out the name
                            String fileLocation = this.node.getFile(f.getName()); //get the location where the file should be
                            boolean transfer = sendFile(f, fileLocation); //transfer = true if the files was sent
                            if (transfer) {
                                //sent message that the file is updated to the local owner
                                String update = "{\"status\":\"UpdateFile\"," + "\"senderID\":" + this.discoveryNode.getCurrentID() + ","
                                        + "\"filename\":" + "\"" + f.getName() + "\"" + "," + "\"location\":" + "\"" + fileLocation + "\"" + "}";
                                DatagramPacket updateFile = new DatagramPacket(update.getBytes(StandardCharsets.UTF_8), update.length(), InetAddress.getByName(receivedFiles.get(f.getName())), 8001);
                                responseSocket.send(updateFile); //sent the packet
                                receivedFiles.remove(f.getName()); //remove from our receivedfiles map
                                f.delete(); //delete the file in replicated folder because we  sent it to the right owner
                            }
                        } catch (Exception e) {

                        }
                    }
                    //for sending files when we get a new neighbour, check all of our localfiles and replicatedFiles if we need to pass them on to this new neighbour
                    //send replicated file to its new owner
                    this.update = false;
                    this.sendFiles = false;
                }
            }

        }
    }

    //Handling receive & send of files
    static boolean sendFile(File file, String fileLocation) throws Exception {
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

     static void deleteMessage(File file, String fileLocation) { //
        //tell owner to delete the file in his replicated folder
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
            System.out.println("Deleting replicated file: " + filename); //print out the name
            assert replicatedFiles != null;
            replicatedFiles[0].delete(); //delete the file
        }
        if(folder.equals("local")){
            File[] localFiles = localFolder.listFiles(filenameFilter);
            System.out.println("Deleting local file: " + filename);
            assert localFiles != null;
            localFiles[0].delete();
        }
    }
}

