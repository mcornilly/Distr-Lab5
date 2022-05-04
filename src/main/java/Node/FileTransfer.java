package Node;

import Node.NamingNode;

import java.io.*;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;

public class FileTransfer {

    private final Socket clientSocket;
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private final NamingNode node;
    private final int currentID;

    private String fileName;


    public FileTransfer(NamingNode node, String fileName, int portNumber) throws Exception {
        this.node = node;
        String name = node.name;
        this.currentID = node.discoveryNode.getCurrentID();
        this.clientSocket = new Socket("localhost", portNumber);
        this.fileName = fileName;
        dataInputStream = new DataInputStream(this.clientSocket.getInputStream());
        dataOutputStream = new DataOutputStream(this.clientSocket.getOutputStream());
        //sendFile("\\resources\\LocalFiles\\"+fileName);
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
        fileOutputStream.close();
    }

    private static void sendFile(String path) throws Exception{
        int bytes = 0;
        File file = new File(path);
        FileInputStream fileInputStream = new FileInputStream(file);
        file.getName();
        dataOutputStream.writeUTF(file.getName());
        dataOutputStream.flush();
        dataOutputStream.writeLong(file.length());
        byte[] buffer = new byte[4*1024];
        while ((bytes=fileInputStream.read(buffer))!=-1){
            dataOutputStream.write(buffer,0,bytes);
            dataOutputStream.flush();
        }
        fileInputStream.close();
    }

}
