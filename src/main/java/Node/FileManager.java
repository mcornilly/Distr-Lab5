package Node;

import Node.NamingNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class FileManager extends Thread {
    private NamingNode node;
    private ArrayList<String> fileList = new ArrayList<>();
    private HashMap<String, String> sharedFiles = new HashMap<>();
    private boolean sendFiles;
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private File[] localFiles;
    private File localFolder;
    private File replicatedFolder;
    private File[] replicatedFiles;
    // Determines when to send or receive a file and where to send it to,
    //If we start a node, we want to send all our files to the respective
    public FileManager(NamingNode node){
        this.node = node;
        this.sendFiles = true;
        String launchDirectory = System.getProperty("user.dir");
        System.out.println(launchDirectory);
        this.localFolder = new File(launchDirectory + "/src/main/resources/LocalFiles"); //All localfiles
        System.out.println(this.localFolder);
        this.replicatedFolder = new File( launchDirectory + "/src/main/resources/ReplicatedFiles");
        this.localFiles = this.localFolder.listFiles();
        System.out.println(Arrays.toString(this.localFiles));
    }

    public void run(){
        //Starting the FileManager
        while(this.node.discoveryNode.getNode().getRunning()) {  //while the node is running
            while(this.sendFiles) {
                //System.out.println(this.node.discoveryNode.isDiscoveryPhase());
                if (!this.node.discoveryNode.isDiscoveryPhase()) { //if the node is out of the discovery phase
                    System.out.println(Arrays.toString(this.localFiles));
                    for (File f : this.localFiles) { // for every local File
                        System.out.println(f.getName()); //print out the name
                        String fileLocation = this.node.getFile(f.getName()); //get the location where the file should be
                        if (!fileLocation.equals("Error")) {
                            JSONParser parser = new JSONParser();
                            try {
                                Object obj = parser.parse(fileLocation);
                                int locationID = (int) (long) ((JSONObject) obj).get("node ID"); // get ID where the file should be
                                System.out.println("local" + InetAddress.getLocalHost().getHostAddress());
                                String locationIP = ((JSONObject) obj).get("node IP").toString(); // get IP where the file should be
                                if(!locationIP.equals(InetAddress.getLocalHost().getHostAddress())) { // if the file should be transferred
                                    sendFile(f, locationIP); //sendFile to the right node
                                    this.sharedFiles.put(f.getName(), locationIP); //keep track of which files are shared
                                }
                                System.out.println("succes send");
                                System.out.println(this.sharedFiles);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    this.sendFiles = false;

                }
            }
        try(ServerSocket receivingSocket = new ServerSocket(5000)){ // Try connecting to port 5000 to start listening to clients
            while(!this.sendFiles) { //while we are not sending anymore
                Socket sendingSocket = receivingSocket.accept(); //try accepting sockets
                dataInputStream = new DataInputStream(sendingSocket.getInputStream());
                System.out.println(sendingSocket + " connected.");
                receiveFile(this.replicatedFolder.toString()); //receive the file
                //receivingSocket.close();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        }
    }



    //Handling receive & send of files
    private void receiveFile(String path) throws Exception{
        int bytes = 0;
        String fileName = dataInputStream.readUTF();
        FileOutputStream fileOutputStream = new FileOutputStream(path + fileName);
        long size = dataInputStream.readLong();     // read file size
        byte[] buffer = new byte[4*1024];
        while (size > 0 && (bytes = dataInputStream.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
            fileOutputStream.write(buffer,0,bytes);
            size -= bytes;      // read upto file size
        }
        System.out.println("succes receive");
        this.replicatedFiles = this.replicatedFolder.listFiles();
        System.out.println(Arrays.toString(this.replicatedFiles));
        fileOutputStream.close();
    }
    private static void sendFile(File file, String IP) throws Exception{
        try(Socket sendingSocket = new Socket(InetAddress.getByName(IP), 5000)) {
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
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
