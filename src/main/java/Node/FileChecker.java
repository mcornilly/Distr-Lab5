package Node;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;

import org.apache.tomcat.jni.Time;

/*
Constantly check our LocalFiles directory for changes
We do this with a WatchService for our LocalFiles directory
 */

public class FileChecker extends Thread {
    private final NamingNode node;
    private final WatchService watchService;
    private final Path path;
    private String fileLocation;

    public FileChecker(NamingNode node, String localDirectory) throws IOException {
        this.node = node;
        this.path = Paths.get(localDirectory);
        System.out.println(this.path.toString());
        this.watchService = FileSystems.getDefault().newWatchService();
    }


    //https://www.baeldung.com/java-nio2-watchservice
    @Override
    public void run() {
        try {
            this.path.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
            WatchKey key;
            while (this.node.discoveryNode.getNode().getRunning()) {
                while ((key = watchService.take()) != null) {
                    Thread.sleep(200);
                    for (WatchEvent<?> event : key.pollEvents()) {
                        File file = new File(event.context().toString()); //get the File affected
                        switch (event.kind().toString()) {
                            case "ENTRY_CREATE":
                                System.out.println(file.getName()); //print out the name
                                this.fileLocation = this.node.getFile(file.getName()); //get the location where the file should be
                                //logfile?
                                //System.out.println(
                                        //"Event kind:" + event.kind()
                                                //+ ". File affected: " + event.context() + ".");
                                FileSend.sendFile(file, this.fileLocation);
                                System.out.println("Created File: " + file.getName());
                                break;
                            case "ENTRY_DELETE":
                                System.out.println(file.getName()); //print out the name
                                this.fileLocation = this.node.getFile(file.getName()); //get the location where the file should be
                                //System.out.println(
                                        //"Event kind:" + event.kind()
                                                //+ ". File affected: " + event.context() + ".");
                                FileSend.deleteFile(file, this.fileLocation);
                                System.out.println("Deleted File: " + file.getName());
                                break;
                        }
                    }
                    key.reset();
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}