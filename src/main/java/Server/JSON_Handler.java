package Server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.stream.Collectors;

public class JSON_Handler extends NamingServer {
    private static final String mappingFile = "src/main/resources/nodeMapping.json";
    static void writeFile() {
        try {
            //write from IP mapping into file
            JSONObject jsonObject = new JSONObject();
            getIpMapLock().readLock().lock();
            for (int key : getIpMapping().keySet()) {
                jsonObject.put(key, getIpMapping().get(key));
            }
            getIpMapLock().readLock().unlock();
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(mappingFile)));
            jsonObject.writeJSONString(out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    static void readFile() throws ParseException {
        try {
            //read from file into the IP mapping
            BufferedReader reader = new BufferedReader(new FileReader(mappingFile));
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(reader.lines().collect(Collectors.joining(System.lineSeparator())));
            getIpMapLock().writeLock().lock();
            getIpMapping().clear();
            for (Object obj : jsonObject.keySet()) {
                long key = Long.parseLong((String) obj);
                getIpMapping().put((int) key, (String) jsonObject.get(obj));
            }
            getIpMapLock().writeLock().unlock();
        } catch (IOException | ParseException ignored) {
        }
    }
}


