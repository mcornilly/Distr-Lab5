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
    private final NamingNode node;
    private DiscoveryNode discoveryNode;
    private static final HashMap<String, String> receivedFiles = new HashMap<>();
    private static DataInputStream dataInputStream = null;
    private File localFolder;
    private File replicatedFolder;
    private File[] replicatedFiles;
    private static ServerSocket receivingSocket;

    public static HashMap<String, String> getReceivedFiles() {
        return receivedFiles;
    }

    public FileReceive(NamingNode node, DiscoveryNode discoveryNode) throws IOException {
        this.node = node;
        this.discoveryNode = discoveryNode;
        String launchDirectory = System.getProperty("user.dir");
        this.localFolder = new File(launchDirectory + "/src/main/resources/LocalFiles"); //All localfiles
        this.replicatedFolder = new File( launchDirectory + "/src/main/resources/ReplicatedFiles");
        String localIP; //open a diff port on every diff node
        try {
            localIP = InetAddress.getLocalHost().getHostAddress();
            char localNumber = localIP.charAt(localIP.length()-1); //get the last char --> for 192.168.6.2 this is 2
            int portNumber =  Integer.parseInt("500" +localNumber); // so this will be 5002 on 6.2, receive on this port
            receivingSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        //Starting receiving
        while(NamingNode.getRunning()) {  //while the node is running, issues with volatile
            try{ // Try connecting to port 500x to start listening to client
                Socket sendingSocket = receivingSocket.accept(); //try accepting sockets
                dataInputStream = new DataInputStream(sendingSocket.getInputStream());
                System.out.println(sendingSocket + " connected for receiving a file");
                String remoteIP = sendingSocket.getInetAddress().getHostAddress();
                receiveFile(this.replicatedFolder.toString(), remoteIP); //receive the file
            } catch (Exception e){
                //e.printStackTrace();
            }
        }
    }

    public static void teardown() throws IOException {
        receivingSocket.close();
    }

    //Handling receive & send of files
    private void receiveFile(String path, String remoteIP) throws Exception {
        int bytes = 0;
        String filename = dataInputStream.readUTF();
        FileOutputStream fileOutputStream = new FileOutputStream(path + "/" + filename);
        long size = dataInputStream.readLong();     // read file size
        byte[] buffer = new byte[4 * 1024];
        while (size > 0 && (bytes = dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
            fileOutputStream.write(buffer, 0, bytes);
            size -= bytes;      // read upto file size
        }
        System.out.println("File " + filename + " received successfully");
        fileOutputStream.close();
        this.replicatedFiles = this.replicatedFolder.listFiles();
        FilenameFilter filenameFilter = (files, s) -> s.startsWith(filename);
        File[] localFile = this.localFolder.listFiles(filenameFilter); //only get the affected file
        File[] replicatedFile = this.replicatedFolder.listFiles(filenameFilter); //only get the affected file

        receivedFiles.put(filename, remoteIP);
        System.out.println("ReplicatedFiles: " + Arrays.toString(this.replicatedFiles));
        if(localFile[0].getName().equals(replicatedFile[0].getName())){
            System.out.println("This is our local file, delete it and send it to our previous node if possible");
            if(this.discoveryNode.getPreviousID() != this.discoveryNode.getCurrentID()) {
                FileSend.sendFile(replicatedFile[0], "{\"file\":" + "\"" + filename + "\"" + "," + "\"node ID\":" + this.discoveryNode.getPreviousID() + "," +
                        "\"node IP\":" + "\"" + this.discoveryNode.getPreviousIP() + "\"" + "}", false, false, this.discoveryNode.getPreviousIP()); //send the file to the prev neighbour
                FileSend.getSentFiles().replace(filename, this.discoveryNode.getPreviousIP());
            }else{
                FileSend.getSentFiles().remove(filename);
            }
            receivedFiles.remove(filename); //remove for our mapping because we are localowners ourselves
            replicatedFile[0].delete();
        }
    }


}

