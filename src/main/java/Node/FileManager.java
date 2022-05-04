package Node;

import Node.NamingNode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class FileManager extends Thread {
    private NamingNode node;
    private ArrayList<String> fileList = new ArrayList<>();
    private boolean sendFiles;
    private boolean receiving;
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    // Determines when to send or receive a file and where to send it to,
    //If we start a node, we want to send all our files to the respective
    public FileManager(NamingNode node){
        this.node = node;
        this.sendFiles = false;
        this.receiving = false;

        this.fileList = //
    }

    public void run(){
        while(this.node.discoveryNode.getNode().getRunning())
            for (String file : fileList){
                FileTransfer.sendFile(file);
            }
        try(ServerSocket receivingSocket = new ServerSocket(5000)){ // Try connecting to port 5000 to start listening to clients
            while(this.receiving) {
                Socket sendingSocket = receivingSocket.accept();
                System.out.println(sendingSocket + " connected.");
                dataInputStream = new DataInputStream(sendingSocket.getInputStream()); // Get input from the sending client
                //dataOutputStream = new DataOutputStream(sendingSocket.getOutputStream());
                FileTransfer.receivefile(file);
            }


        } catch (Exception e){
            e.printStackTrace();
        }





        }
    }
}
