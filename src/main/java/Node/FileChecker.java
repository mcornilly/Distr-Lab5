package Node;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
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
    private File localFolder;

    public FileChecker(NamingNode node, String localDirectory) throws IOException {
        this.node = node;
        this.path = Paths.get(localDirectory);
        System.out.println(this.path.toString());
        this.watchService = FileSystems.getDefault().newWatchService();
        this.localFolder = new File(localDirectory); //All localfiles
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
            while (this.node.getRunning()) {
                while ((key = watchService.take()) != null) {
                    Thread.sleep(500);
                    for (WatchEvent<?> event : key.pollEvents()) {
                        File file = new File(event.context().toString()); //get the File affected
                        //System.out.println(Arrays.toString(localFiles));
                        switch (event.kind().toString()) {
                            case "ENTRY_CREATE":
                                //get the file with the directory
                                FilenameFilter filenameFilter = (files, s) -> s.startsWith(file.getName());
                                File[] localFiles = this.localFolder.listFiles(filenameFilter); //only get the affected file
                                System.out.println(file.getName()); //print out the name
                                this.fileLocation = this.node.getFile(file.getName()); //get the location where the file should be
                                //logfile?
                                //System.out.println(
                                        //"Event kind:" + event.kind()
                                                //+ ". File affected: " + event.context() + ".");

                                System.out.println("    Created File: " + file.getName());
                                FileSend.sendFile(localFiles[0], this.fileLocation, false);
                                break;
                            case "ENTRY_DELETE":
                                System.out.println(file.getName()); //print out the name
                                this.fileLocation = this.node.getFile(file.getName()); //get the location where the file should be
                                //System.out.println(
                                        //"Event kind:" + event.kind()
                                                //+ ". File affected: " + event.context() + ".");
                                System.out.println("    Deleted File: " + file.getName());
                                FileSend.deleteMessage(file, this.fileLocation);
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