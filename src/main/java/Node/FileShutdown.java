package Node;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;

public class FileShutdown {
    private final NamingNode node;
    private final int currentID;
    private final int nextID;
    private final int previousID;
    private final String previousIP;
    private final String nextIP;

    public FileShutdown(NamingNode node) {
        this.node = node;
        String name = node.name;
        this.currentID = node.discoveryNode.getCurrentID();
        this.nextID = node.discoveryNode.getNextID();
        this.previousID = node.discoveryNode.getPreviousID();
        this.nextIP = node.discoveryNode.getNextIP();
        this.previousIP = node.discoveryNode.getPreviousIP();
    }

    public void start(){
        System.out.println("Moving files...");
        File folder = new File("\\resources\\ReplicatedFiles\\" );
        File[] files = folder.listFiles();
        for(File f : files ){
            FileTranser.sendFile(previousIP,f);
        }
    }

    public void checkfile(File f){
        boolean test = false;
        File locFolder = new File("\\resources\\LocalFiles\\" );
        File[] locFiles = locFolder.listFiles();

        for (File floc : locFiles) {
            if (floc == f) {
                test = true;
                break;
            }

        }
    }
}


