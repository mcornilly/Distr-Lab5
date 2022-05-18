import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
//https://stackabuse.com/executing-shell-commands-with-java/

public class Main {

    public static void main(String[] args) throws IOException {
        // write your code here
        //Process process = Runtime.getRuntime().exec("ping www.google.com");
        Process process = Runtime.getRuntime().exec("ssh root@dist-computing.idlab.uantwerpen.be -p 10030");
        printResults(process);

    }

    public static void printResults(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }
}
