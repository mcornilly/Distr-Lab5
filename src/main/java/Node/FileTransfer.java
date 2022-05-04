package Node;

import Node.NamingNode;

import java.io.*;
import java.net.DatagramSocket;

public class FileTransfer {

    private final DatagramSocket ClientSocket;
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private final NamingNode node;
    private final int currentID;
    private final int nextID;
    private final int previousID;
    private final String previousIP;
    private final String nextIP;

    public FileTransfer(NamingNode node){
        this.node = node;
        String name = node.name;
        this.currentID = node.discoveryNode.getCurrentID();
        this.nextID = node.discoveryNode.getNextID();
        this.previousID = node.discoveryNode.getPreviousID();
        this.nextIP = node.discoveryNode.getNextIP();
        this.previousIP = node.discoveryNode.getPreviousIP();
        this.ClientSocket = new DatagramSocket(8006);
        this.ClientSocket.setSoTimeout(1000);
        node.delete(currentID);
    }

    private static void receiveFile(String fileName) throws Exception{
        int bytes = 0;
        FileOutputStream fileOutputStream = new FileOutputStream(fileName);
        long size = dataInputStream.readLong();     // read file size
        byte[] buffer = new byte[4*1024];
        while (size > 0 && (bytes = dataInputStream.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
            fileOutputStream.write(buffer,0,bytes);
            size -= bytes;      // read upto file size
        }
        fileOutputStream.close();
    }

    private static void sendFile(String path) throws Exception{
        int bytes = 0;
        File file = new File(path);
        FileInputStream fileInputStream = new FileInputStream(file);
        dataOutputStream.writeLong(file.length());
        byte[] buffer = new byte[4*1024];
        while ((bytes=fileInputStream.read(buffer))!=-1){
            dataOutputStream.write(buffer,0,bytes);
            dataOutputStream.flush();
        }
        fileInputStream.close();
    }

}
