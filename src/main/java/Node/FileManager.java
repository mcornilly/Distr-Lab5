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
import java.util.HashMap;

public class FileManager extends Thread {
    private NamingNode node;
    private ArrayList<String> fileList = new ArrayList<>();
    private HashMap<String, String> sharedFiles = new HashMap<>();
    private boolean sendFiles;
    private boolean receiving;
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private File[] localFiles;
    private File localFolder;
    private File replicatedFolder;
    // Determines when to send or receive a file and where to send it to,
    //If we start a node, we want to send all our files to the respective
    public FileManager(NamingNode node){
        this.node = node;
        this.sendFiles = true;
        this.receiving = false;
        this.localFolder = new File("\\resources\\LocalFiles");
        this.replicatedFolder = new File("\\resources\\ReplicatedFiles\\");
        this.localFiles = this.localFolder.listFiles();

    }

    public void run(){
        while(this.node.discoveryNode.getNode().getRunning() && !this.node.discoveryNode.isDiscoveryPhase()) {
            while(sendFiles){
            assert this.localFiles != null;
            for (File f : this.localFiles) {
                String fileLocation = this.node.getFile(f.getName());
                if (!fileLocation.equals("Error")) {
                    JSONParser parser = new JSONParser();
                    try {
                        Object obj = parser.parse(fileLocation);
                        int locationID = (int) ((JSONObject) obj).get("node ID");
                        String locationIP = ((JSONObject) obj).get("node IP").toString();
                        sendFile(f, locationIP);
                        this.sharedFiles.put(f.getName(), locationIP);
                        System.out.println("succes send");
                        System.out.println(this.sharedFiles);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
                this.sendFiles = false;
            }
        try(ServerSocket receivingSocket = new ServerSocket(5000)){ // Try connecting to port 5000 to start listening to clients
            while(this.receiving) {
                Socket sendingSocket = receivingSocket.accept();
                dataInputStream = new DataInputStream(sendingSocket.getInputStream());
                System.out.println(sendingSocket + " connected.");
                receiveFile(this.replicatedFolder.toString());
                receivingSocket.close();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        }
    }
    private static void receiveFile(String path) throws Exception{
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
        fileOutputStream.close();
    }
    private static void sendFile(File file, String IP) throws Exception{
        Socket sendingSocket = new Socket(InetAddress.getByName(IP), 5000);
        dataOutputStream = new DataOutputStream(sendingSocket.getOutputStream());
        int bytes = 0;
        FileInputStream fileInputStream = new FileInputStream(file);
        dataOutputStream.writeUTF(file.getName());
        dataOutputStream.flush();
        dataOutputStream.writeLong(file.length());
        byte[] buffer = new byte[4*1024];
        while ((bytes=fileInputStream.read(buffer))!=-1){
            dataOutputStream.write(buffer,0,bytes);
            dataOutputStream.flush();
        }
        fileInputStream.close();
        sendingSocket.close();
    }
}
