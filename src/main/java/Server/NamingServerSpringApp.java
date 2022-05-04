package Server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // if we use 'mvnw spring-boot:run' we start the application
public class NamingServerSpringApp {
    public static void main(String[]  args) {
        SpringApplication springApp = new SpringApplication(NamingServerSpringApp.class); // new spring app we can run
        springApp.run(args); // Run the springApp (naming server)
    }
}
