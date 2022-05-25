package Node;

import org.apache.catalina.Server;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class FileReceive extends Thread{
    //WAT FILEMANAGER NU DOET OPSLITSEN IN 2 THREADS SEND EN RECEIVE
    private final NamingNode node;
    private DiscoveryNode discoveryNode;
    private ArrayList<String> fileList = new ArrayList<>();

    public static HashMap<String, String> getSentFiles() {
        return sentFiles;
    }

    public static void setSentFiles(HashMap<String, String> sentFiles) {
        FileReceive.sentFiles = sentFiles;
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
    private static ServerSocket receivingSocket;

    public static HashMap<String, String> getReceivedFiles() {
        return receivedFiles;
    }

    public static void setReceivedFiles(HashMap<String, String> receivedFiles) {
        FileReceive.receivedFiles = receivedFiles;
    }

    // Determines when to send or receive a file and where to send it to,
    //If we start a node, we want to send all our files to the respective
    public FileReceive(NamingNode node, DiscoveryNode discoveryNode) throws IOException {
        this.node = node;
        this.discoveryNode = discoveryNode;
        String launchDirectory = System.getProperty("user.dir");
        //System.out.println(launchDirectory);
        this.localFolder = new File(launchDirectory + "/src/main/resources/LocalFiles"); //All localfiles
        this.replicatedFolder = new File( launchDirectory + "/src/main/resources/ReplicatedFiles");
        this.localFiles = this.localFolder.listFiles();
        String localIP = null; //open a diff port for every diff node
        try {
            localIP = InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        char localNumber = localIP.charAt(localIP.length()-1); //get the last char --> for 192.168.6.2 this is 2
        int portNumber =  Integer.parseInt("500" +localNumber); // so this will be 5002 on 6.2, receive on this port
        receivingSocket = new ServerSocket(portNumber);
        //geef ook discoveryNode mee
        //this.fileChecker = new FileChecker(node, launchDirectory + "/src/main/resources/LocalFiles"); //check local directory for changes
        //this.fileChecker.start();

    }
    @Override
    public void run(){
        //Starting receiving
        String localIP = null; //open a diff port for every diff node
        try {
            localIP = InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        char localNumber = localIP.charAt(localIP.length()-1); //get the last char --> for 192.168.6.2 this is 2
        int portNumber =  Integer.parseInt("500" +localNumber); // so this will be 5002 on 6.2, receive on this port
        while(NamingNode.getRunning()) {  //while the node is running, issues with volatile
            try{ // Try connecting to port 500x to start listening to client
                Socket sendingSocket = receivingSocket.accept(); //try accepting sockets
                dataInputStream = new DataInputStream(sendingSocket.getInputStream());
                System.out.println(sendingSocket + " connected for receiving a file");
                String remoteIP = sendingSocket.getInetAddress().getHostAddress();
                receiveFile(this.replicatedFolder.toString(), remoteIP); //receive the file
                //receivingSocket.close();
            } catch (Exception e){
                //e.printStackTrace();
            }
        }
    }

    public static void teardown() throws IOException {
        receivingSocket.close();
    }

    //Handling receive & send of files
    private void receiveFile(String path, String remoteIP) throws Exception{
        int bytes = 0;
        String filename = dataInputStream.readUTF();
        FileOutputStream fileOutputStream = new FileOutputStream(path + "/" + filename);
        long size = dataInputStream.readLong();     // read file size
        byte[] buffer = new byte[4*1024];
        while (size > 0 && (bytes = dataInputStream.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
            fileOutputStream.write(buffer,0,bytes);
            size -= bytes;      // read upto file size
        }
        System.out.println("File received succesfully");
        fileOutputStream.close();
        FilenameFilter filenameFilter = (files, s) -> s.startsWith(filename);
        File[] localFiles = this.localFolder.listFiles(filenameFilter); //only get the affected file
        if (localFiles == null){
            this.replicatedFiles = this.replicatedFolder.listFiles();
            receivedFiles.put(filename, remoteIP);
            System.out.println("ReplicatedFiles: " + Arrays.toString(this.replicatedFiles));
        } else {
            if (localFiles[0].getName().equals(filename)) {
                this.replicatedFolder.listFiles(filenameFilter)[0].delete();
            }
        }
    }

}

