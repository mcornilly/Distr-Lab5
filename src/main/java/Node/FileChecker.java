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
            while (true){//this.node.discoveryNode.getNode().getRunning()) {
                while ((key = watchService.take()) != null) {
                    Thread.sleep(200);
                    System.out.println("hello");
                    for (WatchEvent<?> event : key.pollEvents()) {
                        File file = new File(event.context().toString()); //get the File affected
                        System.out.println(file.getName()); //print out the name
                        String fileLocation = this.node.getFile(file.getName()); //get the location where the file should be
                        switch (event.kind().toString()) {
                            case "ENTRY_CREATE":
                                FileManager.sendFile(file, fileLocation);
                                System.out.println("Created File: " + file.getName());
                                //logfile?
                                System.out.println(
                                        "Event kind:" + event.kind()
                                                + ". File affected: " + event.context() + ".");
                                break;
                            case "ENTRY_DELETE":

                                System.out.println(
                                        "Event kind:" + event.kind()
                                                + ". File affected: " + event.context() + ".");
                                FileManager.deleteFile(file, fileLocation);
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